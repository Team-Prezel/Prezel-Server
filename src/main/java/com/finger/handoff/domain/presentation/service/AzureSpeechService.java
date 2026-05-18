package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        private List<WordAnalysisDetail> wordDetails;
    }

    public AzureAnalysisDto analyzePronunciation(String audioFilePath, String referenceText) {
        try {
            SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
            speechConfig.setSpeechRecognitionLanguage("ko-KR");
            speechConfig.requestWordLevelTimestamps(); // 대본 유무와 상관없이 단어 길이 측정을 위해 필요

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
                    log.error("음성 인식 실패: {}", result.getReason());
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

            // 1) 발화 시간 계산: 끝에서부터 역순 탐색하여 Omission이 아닌 '진짜 마지막 단어' 시간 찾기
            for (int i = wordsNode.size() - 1; i >= 0; i--) {
                JsonNode lastValidWord = wordsNode.get(i);
                long offset = lastValidWord.path("Offset").asLong(0);
                long duration = lastValidWord.path("Duration").asLong(0);

                if (offset > 0 || duration > 0) {
                    totalDurationDocs = offset + duration;
                    break; // 유효한 마지막 시간을 찾으면 반복문 종료
                }
            }

            // 2) 글자수 계산: 안 읽고 넘어간 단어(Omission)는 속도 계산에서 제외
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

            int charCount = referenceText.replace(" ", "").length();
            int spm = (int)((durationSeconds > 0) ? (charCount / durationSeconds) * 60 : 0);

            List<WordAnalysisDetail> wordDetails = new ArrayList<>();
            String previousWord = "";
            final double EXCELLENT_PRONUNCIATION_THRESHOLD = 90.0; // 기존 테스트 코드 기준점 유지

            if (wordsNode.isArray()) {
                for (JsonNode wordNode : wordsNode) {
                    String word = wordNode.path("Word").asText("");
                    long offset = wordNode.path("Offset").asLong(0);
                    long duration = wordNode.path("Duration").asLong(0);

                    JsonNode assessmentNode = wordNode.path("PronunciationAssessment");
                    String errorType = assessmentNode.path("ErrorType").asText("None");
                    double accuracy = assessmentNode.path("AccuracyScore").asDouble(0.0);

                    // AzureSpeechService2의 무의미한 데이터 스킵 로직 동일 적용
                    if (offset == 0 && duration == 0 && !errorType.equals("Omission")) {
                        continue;
                    }

                    boolean isStutter = false;
                    if (errorType.equals("Insertion") && word.equals(previousWord)) {
                        isStutter = true;
                    }

                    String statusCode;
                    String description;

                    // AzureSpeechService2의 6가지 상태 분기 완벽 적용
                    if (isStutter) {
                        statusCode = "Stutter";
                        description = "단어 반복/더듬음";
                    }
                    else if (errorType.equals("Insertion")) {
                        statusCode = "Insertion";
                        description = "추임새/불필요한 말";
                    }
                    else if (errorType.equals("Omission")) {
                        statusCode = "Omission";
                        description = "안 읽고 넘어감";
                    }
                    else if (errorType.equals("Mispronunciation")) {
                        statusCode = "Mispronunciation";
                        description = "발음 틀림/부정확";
                    }
                    else {
                        if (accuracy >= EXCELLENT_PRONUNCIATION_THRESHOLD) {
                            statusCode = "Excellent";
                            description = "매우 또렷하고 훌륭한 발음";
                        } else {
                            statusCode = "Good";
                            description = "틀리지 않은 무난한 발음";
                        }
                    }

                    wordDetails.add(WordAnalysisDetail.builder()
                            .word(word)
                            .status(statusCode)
                            .description(description)
                            .accuracy(accuracy)
                            .build());

                    // Insertion(추임새)가 아닐 때만 previousWord 업데이트하는 로직 유지
                    if (!errorType.equals("Insertion")) {
                        previousWord = word;
                    }
                }
            }

            return AzureAnalysisDto.builder()
                    .durationSeconds(durationSeconds)
                    .spm(spm)
                    .speedEval(evaluateSpeed(spm))
                    .accuracyScore(assessment.getAccuracyScore())
                    .scriptMatchRate(assessment.getCompletenessScore())
                    .wordDetails(wordDetails)
                    .build();

        } else {
            // 대본이 없는 경우 (기존 유지)
            String recognizedText = result.getText();
            int charCount = recognizedText.replace(" ", "").length();
            int spm = (int)((durationSeconds > 0) ? (charCount / durationSeconds) * 60 : 0);

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
}