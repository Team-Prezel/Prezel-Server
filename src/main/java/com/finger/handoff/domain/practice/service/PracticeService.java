package com.finger.handoff.domain.practice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.practice.dto.PracticeDto;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class PracticeService {

    @Value("${azure.speech.key}$")
    private String speechKey;

    @Value("${azure.speech.region}$")
    private String speechRegion;

    private final ObjectMapper objectMapper;

    private final List<String> PRACTICE_SENTENCES = Arrays.asList(
            "동해물과 백두산이 마르고 닳도록 하느님이 보우하사 우리나라 만세",
            "남산위에 저 소나무 철갑을 두른듯 바람서리 불변함은 우리 기상일세",
            "가을 하늘 공활한데 높고 구름없이 밝은 달은 우리 가슴 일편단심일세",
            "이기상과 이 맘으로 충성을 다하여 괴로우나 즐거우나 나라 사랑하세"
    );

    public String getRandomSentence() {
        Random random = new Random();
        return PRACTICE_SENTENCES.get(random.nextInt(PRACTICE_SENTENCES.size()));
    }

    public PracticeDto.AnalysisResponse analyzePracticeVoice(MultipartFile audioFile, String referenceText) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("audio_", ".wav");
            audioFile.transferTo(tempFile);

            SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
            speechConfig.setSpeechRecognitionLanguage("ko-KR");
            AudioConfig audioConfig = AudioConfig.fromWavFileInput(tempFile.getAbsolutePath());

            PronunciationAssessmentConfig pronunciationConfig = new PronunciationAssessmentConfig(
                    referenceText,
                    PronunciationAssessmentGradingSystem.HundredMark,
                    PronunciationAssessmentGranularity.Phoneme,
                    true
            );

            try (SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {
                pronunciationConfig.applyTo(recognizer);

                Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
                SpeechRecognitionResult result = task.get();

                if (result.getReason() == ResultReason.RecognizedSpeech) {
                    return extractAnalysisResult(result, referenceText);
                } else {
                    throw new RuntimeException("음성 인식에 실패했습니다. Reason: " + result.getReason());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("음성 분석 처리 중 서버 오류가 발생했습니다.");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private PracticeDto.AnalysisResponse extractAnalysisResult(SpeechRecognitionResult result, String script) throws Exception {
        PronunciationAssessmentResult assessment = PronunciationAssessmentResult.fromResult(result);
        double accuracy = assessment.getAccuracyScore();

        String jsonResult = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
        JsonNode rootNode = objectMapper.readTree(jsonResult);
        JsonNode wordsNode = rootNode.path("NBest").get(0).path("Words");

        long totalDurationDocs = 0;
        if (wordsNode.isArray() && wordsNode.size() > 0) {
            JsonNode lastWord = wordsNode.get(wordsNode.size() - 1);
            totalDurationDocs = lastWord.path("Offset").asLong() + lastWord.path("Duration").asLong();
        }

        double durationSeconds = totalDurationDocs / 10000000.0;
        int charCount = script.replace(" ", "").length();

        double spm = durationSeconds > 0 ? (charCount / durationSeconds) * 60 : 0;

        String speedEval;
        if (spm < 200) speedEval = "느려요";
        else if (spm > 400) speedEval = "빨라요";
        else speedEval = "적당해요";

        String overallEval;
        if (accuracy >= 85.0 && speedEval.equals("적당해요")) {
            overallEval = "Perfect";
        } else if (accuracy >= 70.0) {
            overallEval = "Good";
        } else {
            overallEval = "Try";
        }

        return PracticeDto.AnalysisResponse.builder()
                .accuracyScore(accuracy)
                .speedEvaluation(speedEval)
                .overallEvaluation(overallEval)
                .build();
    }
}
