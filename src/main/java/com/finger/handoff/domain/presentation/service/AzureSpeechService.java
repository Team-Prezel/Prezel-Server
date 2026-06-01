package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.dto.PresentationDTO.WordAnalysisDetail;
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
                } else {
                    throw new RuntimeException("Azure 음성 인식에 실패했습니다.");
                }
            }
        } catch (Exception e) {
            log.error("Azure API 호출 중 에러 발생", e);
            throw new RuntimeException("음성 분석 중 오류가 발생했습니다.", e);
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
            final double EXCELLENT_PRONUNCIATION_THRESHOLD = 90.0;

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
                    String mainFeedback;
                    String subFeedback;

                    if (isStutter) {
                        statusCode = "Stutter";
                        mainFeedback = "단어 반복/더듬음";
                        subFeedback = "말을 더듬었습니다. 자연스럽게 이어서 말할 수 있도록 연습해 보세요.";
                    } else if (errorType.equals("Insertion")) {
                        statusCode = "Insertion";
                        mainFeedback = "추임새/불필요한 말";
                        subFeedback = "대본에 없는 단어가 발음되었습니다. 대본에 집중해 보세요.";
                    } else if (errorType.equals("Omission")) {
                        statusCode = "Omission";
                        mainFeedback = "안 읽고 넘어감";
                        subFeedback = "대본에 있는 단어를 빠뜨렸습니다. 빼먹지 않도록 주의해 주세요.";
                    } else if (errorType.equals("Mispronunciation")) {
                        statusCode = "Mispronunciation";
                        mainFeedback = "발음 틀림/부정확";
                        subFeedback = "단어의 발음이 명확하지 않습니다. 다시 한 번 또박또박 연습해 보세요.";
                    } else {
                        if (accuracy >= EXCELLENT_PRONUNCIATION_THRESHOLD) {
                            statusCode = "Excellent";
                            mainFeedback = "매우 또렷하고 훌륭한 발음";
                            subFeedback = "아주 정확하게 발음하셨습니다!";
                        } else {
                            statusCode = "Good";
                            mainFeedback = "틀리지 않은 무난한 발음";
                            subFeedback = "조금 더 정확하게 발음해 보세요.";
                        }
                    }

                    wordDetails.add(WordAnalysisDetail.builder()
                            .word(word)
                            .status(statusCode)
                            .mainFeedback(mainFeedback)
                            .subFeedback(subFeedback)
                            .accuracy(accuracy)
                            .startTimeMs(startMs)
                            .endTimeMs(endMs)
                            .build());

                    if (!errorType.equals("Insertion")) {
                        previousWord = word;
                    }
                }
            }

            List<PresentationDTO.SentenceAnalysisDetail> sentenceDetails = new ArrayList<>();

            String[] rawSentences = referenceText.split("(?<=[.!?])\\s+|\\r?\\n+");
            List<String> validSentences = new ArrayList<>();
            for (String s : rawSentences) {
                if (s != null && !s.trim().isEmpty()) {
                    validSentences.add(s.trim());
                }
            }

            if (!validSentences.isEmpty()) {
                int currentSentenceIndex = 0;
                int wordCountInCurrentSentence = countWords(validSentences.get(currentSentenceIndex));
                int matchedWords = 0;
                List<WordAnalysisDetail> currentSentenceWords = new ArrayList<>();

                for (WordAnalysisDetail wordDetail : wordDetails) {
                    currentSentenceWords.add(wordDetail);

                    if (!"Insertion".equals(wordDetail.getStatus())) {
                        matchedWords++;
                    }

                    if (matchedWords >= wordCountInCurrentSentence) {
                        sentenceDetails.add(PresentationDTO.SentenceAnalysisDetail.builder()
                                .sentence(validSentences.get(currentSentenceIndex))
                                .wordDetails(currentSentenceWords)
                                .build());

                        currentSentenceIndex++;
                        if (currentSentenceIndex < validSentences.size()) {
                            wordCountInCurrentSentence = countWords(validSentences.get(currentSentenceIndex));
                        } else {
                            wordCountInCurrentSentence = Integer.MAX_VALUE;
                        }
                        matchedWords = 0;
                        currentSentenceWords = new ArrayList<>();
                    }
                }

                if (!currentSentenceWords.isEmpty()) {
                    if (sentenceDetails.isEmpty()) {
                        sentenceDetails.add(PresentationDTO.SentenceAnalysisDetail.builder()
                                .sentence(referenceText.trim())
                                .wordDetails(currentSentenceWords)
                                .build());
                    } else {
                        sentenceDetails.get(sentenceDetails.size() - 1).getWordDetails().addAll(currentSentenceWords);
                    }
                }
            } else {
                sentenceDetails.add(PresentationDTO.SentenceAnalysisDetail.builder()
                        .sentence(referenceText.trim())
                        .wordDetails(wordDetails)
                        .build());
            }

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

    private String evaluateSpeed(double spm) {
        if (spm <= 210) return "느려요";
        if (spm >= 260) return "빨라요";
        return "적당해요";
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }
}