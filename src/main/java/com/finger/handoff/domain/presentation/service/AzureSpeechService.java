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
                        sentenceDetails.add(buildSentenceDetail(validSentences.get(currentSentenceIndex), currentSentenceWords));

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
                        sentenceDetails.add(buildSentenceDetail(referenceText.trim(), currentSentenceWords));
                    } else {
                        PresentationDTO.SentenceAnalysisDetail last = sentenceDetails.remove(sentenceDetails.size() - 1);
                        List<WordAnalysisDetail> combined = new ArrayList<>(last.getWordDetails());
                        combined.addAll(currentSentenceWords);
                        sentenceDetails.add(buildSentenceDetail(last.getSentence(), combined));
                    }
                }
            } else {
                sentenceDetails.add(buildSentenceDetail(referenceText.trim(), wordDetails));
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

    private PresentationDTO.SentenceAnalysisDetail buildSentenceDetail(String sentenceText, List<WordAnalysisDetail> words) {
        if (words == null || words.isEmpty()) {
            return PresentationDTO.SentenceAnalysisDetail.builder()
                    .sentence(sentenceText)
                    .wordDetails(new ArrayList<>())
                    .build();
        }

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
}