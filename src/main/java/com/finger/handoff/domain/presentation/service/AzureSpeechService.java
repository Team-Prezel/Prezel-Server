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

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureSpeechService {

    @Value("${azure.speech.key}")
    private String speechKey;

    @Value("${azure.speech.region}")
    private String speechRegion;

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
        if (wordDetails == null || wordDetails.isEmpty() || origList == null || origList.isEmpty()) {
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

        if (!currentChunk.isEmpty()) {
            String fallbackTarget = origIdx < origList.size() ? origList.get(origIdx) : targetSentence;
            sentenceDetails.add(buildSentenceDetailFromWords(currentChunk, fallbackTarget));
        }

        return sentenceDetails;
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
            if (hasStutter) {
                mainFeedback = "같은 말을 반복하고 있어요.";
                subFeedback = "앞에서 했던 말은 반복하지 않는 것이 좋아요.";
            } else {
                mainFeedback = "불필요한 추임새가 포함되어 있어요.";
                subFeedback = "대본에 없는 단어가 들어가지 않도록 주의해 주세요.";
            }
        }
        else if (hasOmission) {
            statusTag = "누락";
            mainFeedback = "대본의 일부 단어를 빠뜨렸어요.";
            subFeedback = "문장을 끝까지 읽을 수 있도록 대본에 집중해 보세요.";
        }
        else if (hasMispronunciation) {
            statusTag = "발음";
            mainFeedback = "일부 단어의 발음이 부정확해요.";
            subFeedback = "단어의 발음이 명확하지 않습니다. 다시 한 번 또박또박 연습해 보세요.";
        }
        else {
            statusTag = "훌륭해요";
            mainFeedback = "문장의 흐름이 깔끔했어요";
            subFeedback = "지금처럼 또렷한 말하기를 유지해주세요.";
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