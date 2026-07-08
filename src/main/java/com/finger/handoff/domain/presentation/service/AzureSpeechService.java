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
import java.util.List;
import java.util.concurrent.Future;

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

                Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
                SpeechRecognitionResult result = task.get();

                if (result.getReason() == ResultReason.RecognizedSpeech) {
                    return parseAnalysisResult(result, referenceText, hasScript);
                } else if (result.getReason() == ResultReason.NoMatch) {
                    NoMatchDetails noMatchDetails = NoMatchDetails.fromResult(result);
                    log.warn("Azure 음성 인식 실패 (무음 또는 음성 감지 불가): {}", noMatchDetails.getReason());

                    if (noMatchDetails.getReason() == NoMatchReason.InitialSilenceTimeout) {
                        log.warn("무음 파일이 감지되었습니다. (InitialSilenceTimeout)");
                        throw new BusinessException(ErrorCode.SILENT_AUDIO_DETECTED);
                    } else {
                        log.warn("음성을 인식할 수 없습니다. (NotRecognized)");
                        throw new BusinessException(ErrorCode.VOICE_RECOGNITION_FAILED);
                    }
                } else {
                    log.error("Azure 음성 인식 실패 (Reason: {})", result.getReason());
                    throw new BusinessException(ErrorCode.VOICE_ANALYSIS_FAILED);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Azure API 호출 및 분석 중 에러 발생", e);
            throw new BusinessException(ErrorCode.VOICE_ANALYSIS_FAILED);
        }
    }

    private AzureAnalysisDto parseAnalysisResult(SpeechRecognitionResult result, String referenceText, boolean hasScript) throws Exception {
        String jsonResult = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonResult);
        JsonNode wordsNode = rootNode.path("NBest").get(0).path("Words");

        long totalDurationDocs = 0;
        int spokenCharCount = 0;

        if (wordsNode.isArray() && wordsNode.size() > 0) {
            for (int i = wordsNode.size() - 1; i >= 0; i--) {
                JsonNode lastValidWord = wordsNode.get(i);
                long offset = lastValidWord.path("Offset").asLong(0);
                long duration = lastValidWord.path("Duration").asLong(0);
                if (offset > 0 || duration > 0) {
                    totalDurationDocs = offset + duration;
                    break;
                }
            }

            for (JsonNode wordNode : wordsNode) {
                String errorType = wordNode.path("PronunciationAssessment").path("ErrorType").asText("None");
                if (!errorType.equals("Omission")) {
                    spokenCharCount += wordNode.path("Word").asText("").length();
                }
            }
        }

        double durationSecondsDouble = totalDurationDocs / 10000000.0;
        int durationSeconds = (int) Math.round(durationSecondsDouble);

        if (hasScript) {
            PronunciationAssessmentResult assessment = PronunciationAssessmentResult.fromResult(result);
            int spm = (int)((durationSecondsDouble > 0) ? (spokenCharCount / durationSecondsDouble) * 60 : 0);

            List<WordAnalysisDetail> wordDetails = new ArrayList<>();
            String previousWord = "";

            if (wordsNode.isArray()) {
                for (JsonNode wordNode : wordsNode) {
                    String word = wordNode.path("Word").asText("");
                    long offset = wordNode.path("Offset").asLong(0);
                    long duration = wordNode.path("Duration").asLong(0);

                    JsonNode assessmentNode = wordNode.path("PronunciationAssessment");
                    String errorType = assessmentNode.path("ErrorType").asText("None");
                    double accuracy = assessmentNode.path("AccuracyScore").asDouble(0.0);

                    if (offset == 0 && duration == 0 && !errorType.equals("Omission")) {
                        continue;
                    }

                    long startMs = offset / 10000;
                    long endMs = (offset + duration) / 10000;

                    boolean isStutter = false;
                    if (errorType.equals("Insertion") && word.equals(previousWord)) {
                        isStutter = true;
                    }

                    String statusCode;
                    if (isStutter) {
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

                    wordDetails.add(WordAnalysisDetail.builder()
                            .word(word)
                            .status(statusCode)
                            .accuracy(accuracy)
                            .startTimeMs(startMs)
                            .endTimeMs(endMs)
                            .build());

                    if (!errorType.equals("Insertion")) {
                        previousWord = word;
                    }
                }
            }

            List<PresentationDTO.SentenceAnalysisDetail> sentenceDetails = splitWordsIntoNaturalSentences(wordDetails);

            applyTopNRelativeEvaluation(sentenceDetails);

            return AzureAnalysisDto.builder()
                    .durationSeconds(durationSeconds)
                    .spm(spm)
                    .speedEval(evaluateSpeed(spm))
                    .accuracyScore(assessment.getAccuracyScore())
                    .scriptMatchRate(assessment.getCompletenessScore())
                    .sentenceDetails(sentenceDetails)
                    .build();

        } else {
            int spm = (int)((durationSecondsDouble > 0) ? (spokenCharCount / durationSecondsDouble) * 60 : 0);
            return AzureAnalysisDto.builder()
                    .durationSeconds(durationSeconds)
                    .spm(spm)
                    .speedEval(evaluateSpeed(spm))
                    .build();
        }
    }

    private List<PresentationDTO.SentenceAnalysisDetail> splitWordsIntoNaturalSentences(List<WordAnalysisDetail> wordDetails) {
        List<PresentationDTO.SentenceAnalysisDetail> sentenceDetails = new ArrayList<>();
        if (wordDetails == null || wordDetails.isEmpty()) return sentenceDetails;

        List<WordAnalysisDetail> currentChunk = new ArrayList<>();

        for (int i = 0; i < wordDetails.size(); i++) {
            WordAnalysisDetail currentWord = wordDetails.get(i);
            String text = currentWord.getWord();

            if (!currentChunk.isEmpty() && isConjunction(text)) {
                sentenceDetails.add(buildSentenceDetailFromWords(currentChunk));
                currentChunk = new ArrayList<>();
            }

            currentChunk.add(currentWord);

            boolean shouldSplit = false;

            if (isSentenceEnding(text)) {
                shouldSplit = true;
            }

            if (i < wordDetails.size() - 1) {
                WordAnalysisDetail nextWord = wordDetails.get(i + 1);
                long silenceGap = nextWord.getStartTimeMs() - currentWord.getEndTimeMs();
                if (silenceGap >= 700) {
                    shouldSplit = true;
                }
            }

            if (shouldSplit && !currentChunk.isEmpty()) {
                sentenceDetails.add(buildSentenceDetailFromWords(currentChunk));
                currentChunk = new ArrayList<>();
            }
        }

        if (!currentChunk.isEmpty()) {
            sentenceDetails.add(buildSentenceDetailFromWords(currentChunk));
        }

        return sentenceDetails;
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

    private PresentationDTO.SentenceAnalysisDetail buildSentenceDetailFromWords(List<WordAnalysisDetail> words) {
        if (words == null || words.isEmpty()) {
            return PresentationDTO.SentenceAnalysisDetail.builder()
                    .sentence("")
                    .wordDetails(new ArrayList<>())
                    .build();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            sb.append(words.get(i).getWord());
            if (i < words.size() - 1) sb.append(" ");
        }
        String sentenceText = sb.toString();

        long startMs = words.get(0).getStartTimeMs();
        long endMs = words.get(words.size() - 1).getEndTimeMs();
        double totalAccuracy = 0.0;

        boolean hasStutter = false;
        boolean hasInsertion = false;
        boolean hasMispronunciation = false;
        boolean hasOmission = false;

        for (WordAnalysisDetail w : words) {
            totalAccuracy += w.getAccuracy();
            if ("Stutter".equals(w.getStatus())) hasStutter = true;
            if ("Insertion".equals(w.getStatus())) hasInsertion = true;
            if ("Mispronunciation".equals(w.getStatus())) hasMispronunciation = true;
            if ("Omission".equals(w.getStatus())) hasOmission = true;
        }

        double avgAccuracy = totalAccuracy / words.size();

        String statusTag;
        String mainFeedback;
        String subFeedback;

        if (hasStutter) {
            statusTag = "불필요한 표현";
            mainFeedback = "같은 말을 반복하고 있어요.";
            subFeedback = "앞에서 했던 말은 반복하지 않는 것이 좋아요.";
        } else if (hasInsertion) {
            statusTag = "불필요한 표현";
            mainFeedback = "불필요한 추임새가 포함되어 있어요.";
            subFeedback = "대본에 없는 단어가 들어가지 않도록 주의해 주세요.";
        } else if (hasMispronunciation) {
            statusTag = "발음";
            mainFeedback = "일부 단어의 발음이 부정확해요.";
            subFeedback = "단어의 발음이 명확하지 않습니다. 다시 한 번 또박또박 연습해 보세요.";
        } else if (hasOmission) {
            statusTag = "누락";
            mainFeedback = "대본의 일부 단어를 빠뜨렸어요.";
            subFeedback = "문장을 끝까지 읽을 수 있도록 대본에 집중해 보세요.";
        } else {
            statusTag = "발음";
            mainFeedback = "문장의 흐름이 깔끔했어요";
            subFeedback = "지금처럼 또렷한 말하기를 유지해주세요.";
        }

        return PresentationDTO.SentenceAnalysisDetail.builder()
                .sentence(sentenceText)
                .status(statusTag)
                .mainFeedback(mainFeedback)
                .subFeedback(subFeedback)
                .accuracy(avgAccuracy)
                .startTimeMs(startMs)
                .endTimeMs(endMs)
                .wordDetails(words)
                .build();
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
                candidate.setStatus("Excellent");
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
        if ("Excellent".equals(sentence.getStatus())) {
            sentence.setStatus("Good");
        }
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