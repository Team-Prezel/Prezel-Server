package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.dto.PresentationDTO.WordAnalysisDetail;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureSpeechService {

    @Value("${azure.speech.key}")
    private String speechKey;

    @Value("${azure.speech.region}")
    private String speechRegion;

    private record FeedbackPair(String mainFeedback, String subFeedback) {}

    private static final List<FeedbackPair> PRONUNCIATION_STRENGTH_FEEDBACKS = List.of(
            new FeedbackPair("발음이 선명하게 잘 들려요.", "다음 문장에서도 지금처럼 또렷한 발음을 유지해보세요."),
            new FeedbackPair("전달하고자 하는 내용이 정확하게 잘 들려요.", "중요한 단어는 지금처럼 분명하게 발음해보세요."),
            new FeedbackPair("막힘 없이 자연스럽게 이어졌어요.", "긴 문장에서도 흐름이 끊기지 않도록 끝까지 이어서 말해보세요.")
    );

    private static final List<FeedbackPair> UNNECESSARY_EXPRESSION_FEEDBACKS = List.of(
            new FeedbackPair("잠시 머뭇거리는 부분이 있어요.", "전달하고자하는 핵심 문장에 집중해서 말해보세요."),
            new FeedbackPair("말 사이에 불필요한 표현이 들어갔어요.", "생각할 시간이 필요할 때는 잠깐 멈춘 뒤 이어서 말해보세요."),
            new FeedbackPair("문장의 흐름을 끊는 표현이 있어요.", "다음 문장의 핵심 단어를 파악한 뒤 자연스럽게 이어가보세요.")
    );

    private static final List<String> MISMATCH_MAIN_FEEDBACKS = List.of(
            "일부 단어가 대본과 다르게 들려요.",
            "대본과 다른 발음이 들려요.",
            "대본과 다르게 말한 부분이 있어요."
    );
    private static final String MISMATCH_SUB_FEEDBACK = "단어의 발음이 명확하지 않습니다. 다시 한 번 또박또박 연습해 보세요.";

    private static final List<String> OMISSION_MAIN_FEEDBACKS = List.of(
            "말하지 않고 넘어간 단어가 있어요.",
            "대본의 일부 내용이 빠졌어요.",
            "중간에 건너뛴 부분이 있어요."
    );
    private static final String OMISSION_SUB_FEEDBACK = "문장을 끝까지 읽을 수 있도록 대본에 집중해 보세요.";

    private static FeedbackPair getRandomFeedback(List<FeedbackPair> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private static String getRandomString(List<String> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    @Getter
    @Builder
    public static class AzureAnalysisDto {
        private Integer durationSeconds;
        private Integer spm;
        private String speedEval;
        private Double accuracyScore;
        private Double scriptMatchRate;
        private List<PresentationDTO.SentenceAnalysisDetail> sentenceDetails;
    }

    public AzureAnalysisDto analyzePronunciation(String audioFilePath, String referenceText) {
        try {
            SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
            speechConfig.setSpeechRecognitionLanguage("ko-KR");
            speechConfig.requestWordLevelTimestamps();

            AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFilePath);
            boolean hasScript = referenceText != null && !referenceText.trim().isEmpty();

            try (SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {
                if (hasScript) {
                    PronunciationAssessmentConfig pronunciationConfig = new PronunciationAssessmentConfig(
                            referenceText,
                            PronunciationAssessmentGradingSystem.HundredMark,
                            PronunciationAssessmentGranularity.Phoneme,
                            true
                    );
                    pronunciationConfig.applyTo(recognizer);
                }

                List<String> jsonResults = Collections.synchronizedList(new ArrayList<>());
                List<Double> accuracyScores = Collections.synchronizedList(new ArrayList<>());
                List<Double> completenessScores = Collections.synchronizedList(new ArrayList<>());

                CountDownLatch latch = new CountDownLatch(1);

                recognizer.recognized.addEventListener((s, e) -> {
                    if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                        String json = e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
                        if (json != null) {
                            jsonResults.add(json);
                        }
                        if (hasScript) {
                            PronunciationAssessmentResult assessment = PronunciationAssessmentResult.fromResult(e.getResult());
                            if (assessment != null) {
                                accuracyScores.add(assessment.getAccuracyScore());
                                completenessScores.add(assessment.getCompletenessScore());
                            }
                        }
                    }
                });

                recognizer.sessionStopped.addEventListener((s, e) -> {
                    log.info("Azure 음성 연속 인식 완료 (SessionStopped)");
                    latch.countDown();
                });

                recognizer.canceled.addEventListener((s, e) -> {
                    log.warn("Azure 음성 인식 취소됨 (Canceled)");
                    latch.countDown();
                });

                recognizer.startContinuousRecognitionAsync().get();

                latch.await();

                recognizer.stopContinuousRecognitionAsync().get();

                if (jsonResults.isEmpty()) {
                    log.warn("인식된 음성이 없습니다. (무음 파일이거나 인식 실패)");
                    throw new BusinessException(ErrorCode.VOICE_RECOGNITION_FAILED);
                }

                return parseAnalysisResultContinuous(jsonResults, accuracyScores, completenessScores, referenceText, hasScript);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Azure API 호출 및 분석 중 에러 발생", e);
            throw new BusinessException(ErrorCode.VOICE_ANALYSIS_FAILED);
        }
    }

    private AzureAnalysisDto parseAnalysisResultContinuous(
            List<String> jsonResults,
            List<Double> accuracyScores,
            List<Double> completenessScores,
            String referenceText,
            boolean hasScript) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        List<WordAnalysisDetail> allWordDetails = new ArrayList<>();

        long totalDurationDocs = 0;
        int spokenCharCount = 0;

        List<String> origList = new ArrayList<>();
        if (hasScript) {
            String pattern = "(?<=(니다|요|까)[.!?]?)\\s+|(?<=[.!?])\\s+|\\r?\\n+";
            String[] splits = referenceText.trim().split(pattern);
            for(String s : splits) {
                if(!s.trim().isEmpty()) {
                    origList.add(s.trim());
                }
            }
        }


        String previousWord = "";

        for (String json : jsonResults) {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode nBest = rootNode.path("NBest");
            if (nBest.isMissingNode() || nBest.size() == 0) continue;

            JsonNode wordsNode = nBest.get(0).path("Words");
            if (!wordsNode.isArray() || wordsNode.size() == 0) continue;

            for (int i = wordsNode.size() - 1; i >= 0; i--) {
                JsonNode lastValidWord = wordsNode.get(i);
                long offset = lastValidWord.path("Offset").asLong(0);
                long duration = lastValidWord.path("Duration").asLong(0);
                if (offset > 0 || duration > 0) {
                    totalDurationDocs = Math.max(totalDurationDocs, offset + duration);
                    break;
                }
            }

            for (JsonNode wordNode : wordsNode) {
                String word = wordNode.path("Word").asText("");
                String cleanWord = word.replaceAll("[^가-힣a-zA-Z0-9]", "");

                long offset = wordNode.path("Offset").asLong(0);
                long duration = wordNode.path("Duration").asLong(0);

                JsonNode assessmentNode = wordNode.path("PronunciationAssessment");
                String errorType = assessmentNode.path("ErrorType").asText("None");
                double accuracy = assessmentNode.path("AccuracyScore").asDouble(0.0);

                if (!errorType.equals("Omission")) {
                    spokenCharCount += word.length();
                }

                if (offset == 0 && duration == 0 && !errorType.equals("Omission")) {
                    continue;
                }

                long startMs = offset / 10000;
                long endMs = (offset + duration) / 10000;

                boolean isFiller = cleanWord.equals("어") || cleanWord.equals("음") || cleanWord.equals("그") ||
                        cleanWord.equals("아") || cleanWord.equals("저기") || cleanWord.equals("그니까") ||
                        cleanWord.equals("막") || cleanWord.equals("이제") || cleanWord.equals("에");

                boolean isStutter = false;

                if (!errorType.equals("Omission") && !cleanWord.isEmpty() && cleanWord.equals(previousWord)) {
                    isStutter = true;
                }

                if (!errorType.equals("Omission") && !cleanWord.isEmpty()) {
                    previousWord = cleanWord;
                }

                String statusCode;
                if (isStutter || isFiller) {
                    statusCode = "Stutter";
                } else if (errorType.equals("Insertion")) {
                    statusCode = "Insertion";
                } else if (errorType.equals("Omission")) {
                    statusCode = "Omission";
                } else if (errorType.equals("Mispronunciation")) {
                    statusCode = "Mispronunciation";
                } else {
                    if (accuracy >= 90.0) {
                        statusCode = "Excellent";
                    } else {
                        statusCode = "Good";
                    }
                }

                WordAnalysisDetail wd = WordAnalysisDetail.builder()
                        .word(word)
                        .status(statusCode)
                        .accuracy(accuracy)
                        .startTimeMs(startMs)
                        .endTimeMs(endMs)
                        .build();

                allWordDetails.add(wd);
            }
        }

        allWordDetails.sort(java.util.Comparator.comparingLong(WordAnalysisDetail::getStartTimeMs));

        if (allWordDetails.isEmpty() || spokenCharCount == 0) {
            log.warn("Azure가 음성을 인식했으나 실제로 발음된 단어가 없습니다 (무음 또는 잡음 감지)");
            throw new BusinessException(ErrorCode.SILENT_AUDIO_DETECTED);
        }

        double durationSecondsDouble = totalDurationDocs / 10000000.0;
        int durationSeconds = (int) Math.round(durationSecondsDouble);
        int spm = (int)((durationSecondsDouble > 0) ? (spokenCharCount / durationSecondsDouble) * 60 : 0);

        if (hasScript) {
            List<PresentationDTO.SentenceAnalysisDetail> sentenceDetails = splitWordsIntoNaturalSentences(allWordDetails, origList);
            applyTopNRelativeEvaluation(sentenceDetails);

            double finalAccuracy = accuracyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double finalCompleteness = completenessScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            return AzureAnalysisDto.builder()
                    .durationSeconds(durationSeconds)
                    .spm(spm)
                    .speedEval(evaluateSpeed(spm))
                    .accuracyScore(finalAccuracy)
                    .scriptMatchRate(finalCompleteness)
                    .sentenceDetails(sentenceDetails)
                    .build();
        } else {
            return AzureAnalysisDto.builder()
                    .durationSeconds(durationSeconds)
                    .spm(spm)
                    .speedEval(evaluateSpeed(spm))
                    .build();
        }
    }

    private List<PresentationDTO.SentenceAnalysisDetail> splitWordsIntoNaturalSentences(
            List<WordAnalysisDetail> wordDetails,
            List<String> origList) {

        List<PresentationDTO.SentenceAnalysisDetail> sentenceDetails = new ArrayList<>();
        if (origList == null || origList.isEmpty()) {
            return sentenceDetails;
        }

        if (wordDetails == null || wordDetails.isEmpty()) {
            for (String omitted : origList) {
                sentenceDetails.add(buildOmittedSentenceDetail(omitted, 0L));
            }
            return sentenceDetails;
        }

        int origIdx = 0;
        List<WordAnalysisDetail> currentChunk = new ArrayList<>();
        StringBuilder accumulatedRefText = new StringBuilder();

        String targetSentence = origList.get(origIdx);
        String cleanTarget = targetSentence.replaceAll("[^가-힣a-zA-Z0-9]", "");

        for (WordAnalysisDetail word : wordDetails) {
            currentChunk.add(word);

            if (!"Insertion".equals(word.getStatus())) {
                accumulatedRefText.append(word.getWord().replaceAll("[^가-힣a-zA-Z0-9]", ""));
            }

            int lookAheadLimit = Math.min(origIdx + 3, origList.size() - 1);
            if (origIdx < lookAheadLimit && currentChunk.size() >= 2) {
                int currentMatch = countMatchingMeaningfulWords(currentChunk, cleanTarget);
                if (currentMatch == 0) {
                    int bestSkipIdx = -1;
                    int maxMatch = 1;

                    for (int nextIdx = origIdx + 1; nextIdx <= lookAheadLimit; nextIdx++) {
                        String cleanNext = origList.get(nextIdx).replaceAll("[^가-힣a-zA-Z0-9]", "");
                        int matchCount = countMatchingMeaningfulWords(currentChunk, cleanNext);
                        if (matchCount > maxMatch) {
                            maxMatch = matchCount;
                            bestSkipIdx = nextIdx;
                        }
                    }

                    if (bestSkipIdx != -1) {
                        long skipTimeMs = currentChunk.get(0).getStartTimeMs();
                        while (origIdx < bestSkipIdx) {
                            sentenceDetails.add(buildOmittedSentenceDetail(origList.get(origIdx), skipTimeMs));
                            origIdx++;
                        }
                        targetSentence = origList.get(origIdx);
                        cleanTarget = targetSentence.replaceAll("[^가-힣a-zA-Z0-9]", "");
                        accumulatedRefText.setLength(0);
                        for (WordAnalysisDetail w : currentChunk) {
                            if (!"Insertion".equals(w.getStatus())) {
                                accumulatedRefText.append(w.getWord().replaceAll("[^가-힣a-zA-Z0-9]", ""));
                            }
                        }
                    }
                }
            }

            if (!cleanTarget.isEmpty() && accumulatedRefText.length() >= cleanTarget.length()) {
                sentenceDetails.add(buildSentenceDetailFromWords(currentChunk, targetSentence));

                origIdx++;
                if (origIdx < origList.size()) {
                    targetSentence = origList.get(origIdx);
                    cleanTarget = targetSentence.replaceAll("[^가-힣a-zA-Z0-9]", "");
                } else {
                    cleanTarget = "";
                }

                currentChunk = new ArrayList<>();
                accumulatedRefText.setLength(0);
            }
        }

        long lastEndMs = 0;
        if (!sentenceDetails.isEmpty()) {
            lastEndMs = sentenceDetails.get(sentenceDetails.size() - 1).getEndTimeMs();
        }

        if (!currentChunk.isEmpty()) {
            String fallbackTarget = origIdx < origList.size() ? origList.get(origIdx) : targetSentence;
            PresentationDTO.SentenceAnalysisDetail detail = buildSentenceDetailFromWords(currentChunk, fallbackTarget);
            sentenceDetails.add(detail);
            lastEndMs = Math.max(lastEndMs, detail.getEndTimeMs());
            origIdx++;
        }

        while (origIdx < origList.size()) {
            String omittedSentence = origList.get(origIdx);
            sentenceDetails.add(buildOmittedSentenceDetail(omittedSentence, lastEndMs));
            origIdx++;
        }

        return sentenceDetails;
    }

    private int countMatchingMeaningfulWords(List<WordAnalysisDetail> words, String cleanSentence) {
        if (words == null || words.isEmpty() || cleanSentence == null || cleanSentence.isEmpty()) {
            return 0;
        }
        int matchCount = 0;
        for (WordAnalysisDetail w : words) {
            if ("Insertion".equals(w.getStatus())) continue;
            String cleanWord = w.getWord().replaceAll("[^가-힣a-zA-Z0-9]", "");
            if (cleanWord.length() >= 2 && cleanSentence.contains(cleanWord)) {
                matchCount++;
            }
        }
        return matchCount;
    }

    private PresentationDTO.SentenceAnalysisDetail buildOmittedSentenceDetail(
            String originalSentence,
            long timeMs) {

        String mainFeedback = getRandomString(OMISSION_MAIN_FEEDBACKS);
        String subFeedback = OMISSION_SUB_FEEDBACK;

        List<WordAnalysisDetail> omittedWords = new ArrayList<>();
        if (originalSentence != null && !originalSentence.trim().isEmpty()) {
            String[] tokens = originalSentence.trim().split("\\s+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    omittedWords.add(WordAnalysisDetail.builder()
                            .word(token)
                            .status("Omission")
                            .accuracy(0.0)
                            .startTimeMs(timeMs)
                            .endTimeMs(timeMs)
                            .build());
                }
            }
        }

        return PresentationDTO.SentenceAnalysisDetail.builder()
                .sentence(originalSentence)
                .status("누락")
                .mainFeedback(mainFeedback)
                .subFeedback(subFeedback)
                .guideScript(originalSentence)
                .accuracy(0.0)
                .startTimeMs(timeMs)
                .endTimeMs(timeMs)
                .wordDetails(omittedWords)
                .build();
    }

    private PresentationDTO.SentenceAnalysisDetail buildSentenceDetailFromWords(
            List<WordAnalysisDetail> words,
            String originalSentence) {

        long startMs = -1;
        long endMs = 0;
        double totalAccuracy = 0.0;

        boolean hasStutter = false;
        boolean hasInsertion = false;
        boolean hasMispronunciation = false;
        boolean hasOmission = false;

        int spokenLength = 0;

        for (WordAnalysisDetail w : words) {
            totalAccuracy += w.getAccuracy();

            if ("Stutter".equals(w.getStatus())) hasStutter = true;
            if ("Insertion".equals(w.getStatus())) hasInsertion = true;
            if ("Mispronunciation".equals(w.getStatus())) hasMispronunciation = true;
            if ("Omission".equals(w.getStatus())) hasOmission = true;

            if (!"Omission".equals(w.getStatus())) {
                if (startMs == -1) startMs = w.getStartTimeMs();
                endMs = Math.max(endMs, w.getEndTimeMs());
            }

            if (!"Insertion".equals(w.getStatus())) {
                spokenLength += w.getWord().replaceAll("[^가-힣a-zA-Z0-9]", "").length();
            }
        }

        if (startMs == -1) startMs = 0;
        double avgAccuracy = totalAccuracy / words.size();
        String statusTag;
        String mainFeedback;
        String subFeedback;

        String cleanOriginal = originalSentence.replaceAll("[^가-힣a-zA-Z0-9]", "");
        if (spokenLength < cleanOriginal.length()) {
            hasOmission = true;
        }

        if (hasStutter || hasInsertion) {
            statusTag = "불필요한 표현";
            FeedbackPair feedback = getRandomFeedback(UNNECESSARY_EXPRESSION_FEEDBACKS);
            mainFeedback = feedback.mainFeedback();
            subFeedback = feedback.subFeedback();
        }
        else if (hasOmission) {
            statusTag = "누락";
            mainFeedback = getRandomString(OMISSION_MAIN_FEEDBACKS);
            subFeedback = OMISSION_SUB_FEEDBACK;
        }
        else if (hasMispronunciation) {
            statusTag = "발음";
            mainFeedback = getRandomString(MISMATCH_MAIN_FEEDBACKS);
            subFeedback = MISMATCH_SUB_FEEDBACK;
        }
        else {
            statusTag = "훌륭해요";
            FeedbackPair feedback = getRandomFeedback(PRONUNCIATION_STRENGTH_FEEDBACKS);
            mainFeedback = feedback.mainFeedback();
            subFeedback = feedback.subFeedback();
        }

        return PresentationDTO.SentenceAnalysisDetail.builder()
                .sentence(originalSentence)
                .status(statusTag)
                .mainFeedback(mainFeedback)
                .subFeedback(subFeedback)
                .guideScript(originalSentence)
                .accuracy(avgAccuracy)
                .startTimeMs(startMs)
                .endTimeMs(endMs)
                .wordDetails(words)
                .build();
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    private boolean isConjunction(String word) {
        if (word == null) return false;
        String cleanWord = word.replaceAll("[^가-힣]", "");
        return cleanWord.equals("그리고") || cleanWord.equals("그래서") ||
                cleanWord.equals("하지만") || cleanWord.equals("그러나") ||
                cleanWord.equals("다음으로") || cleanWord.equals("또한") ||
                cleanWord.equals("반면에") || cleanWord.equals("결과적으로");
    }

    private boolean isSentenceEnding(String word) {
        if (word == null) return false;
        return word.endsWith(".") || word.endsWith("?") || word.endsWith("!") ||
                word.endsWith("다") || word.endsWith("요") || word.endsWith("죠") ||
                word.endsWith("까") || word.endsWith("니다") || word.endsWith("아") || word.endsWith("어");
    }

    private void applyTopNRelativeEvaluation(List<PresentationDTO.SentenceAnalysisDetail> sentenceDetails) {
        if (sentenceDetails == null || sentenceDetails.isEmpty()) return;

        for (PresentationDTO.SentenceAnalysisDetail sentence : sentenceDetails) {
            if (sentence.getWordDetails() != null) {
                for (PresentationDTO.WordAnalysisDetail word : sentence.getWordDetails()) {
                    if ("Mispronunciation".equals(word.getStatus()) && word.getAccuracy() != null && word.getAccuracy() >= 75.0) {
                        word.setStatus("Good");
                    }
                }
            }
        }

        List<PresentationDTO.SentenceAnalysisDetail> bestCandidates = new ArrayList<>();

        for (PresentationDTO.SentenceAnalysisDetail sentence : sentenceDetails) {
            boolean hasError = false;

            if (sentence.getWordDetails() != null) {
                for (PresentationDTO.WordAnalysisDetail word : sentence.getWordDetails()) {
                    String st = word.getStatus();
                    if ("Stutter".equals(st) || "Insertion".equals(st) ||
                            "Omission".equals(st) || "Mispronunciation".equals(st)) {
                        hasError = true;
                        break;
                    }
                }
            }

            if (!hasError && sentence.getAccuracy() != null && sentence.getAccuracy() >= 90.0) {
                bestCandidates.add(sentence);
            } else {
                demoteToGood(sentence);
            }
        }

        bestCandidates.sort((a, b) -> Double.compare(b.getAccuracy(), a.getAccuracy()));

        int maxExcellentCount = Math.min(3, Math.max(1, sentenceDetails.size() / 4));

        for (int i = 0; i < bestCandidates.size(); i++) {
            PresentationDTO.SentenceAnalysisDetail candidate = bestCandidates.get(i);

            if (i < maxExcellentCount) {
                if (candidate.getWordDetails() != null) {
                    for (PresentationDTO.WordAnalysisDetail word : candidate.getWordDetails()) {
                        if ("Good".equals(word.getStatus()) || "Excellent".equals(word.getStatus())) {
                            word.setStatus("Excellent");
                        }
                    }
                }
            } else {
                demoteToGood(candidate);
            }
        }
    }


    private void demoteToGood(PresentationDTO.SentenceAnalysisDetail sentence) {
        if (sentence.getWordDetails() != null) {
            for (PresentationDTO.WordAnalysisDetail word : sentence.getWordDetails()) {
                if ("Excellent".equals(word.getStatus())) {
                    word.setStatus("Good");
                }
            }
        }
    }

    private String evaluateSpeed(double spm) {
        if (spm <= 210) return "느려요";
        if (spm >= 260) return "빨라요";
        return "적당해요";
    }
}