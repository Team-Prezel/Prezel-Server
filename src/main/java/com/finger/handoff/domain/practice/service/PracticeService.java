package com.finger.handoff.domain.practice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.practice.dto.PracticeDto;
import com.finger.handoff.domain.practice.repository.PracticeScriptRepository;
import com.finger.handoff.global.audio.AudioConverter;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeService {

    private final ObjectMapper objectMapper;
    private final PracticeScriptRepository repository;
    private final AudioConverter audioConverter;

    @Value("${azure.speech.key}")
    private String speechKey;

    @Value("${azure.speech.region}")
    private String speechRegion;

    public String getRandomSentence() {
        String script = repository.findRandomScript();

        if (script == null || script.isBlank()) {
            throw new BusinessException(ErrorCode.SCRIPT_NOT_FOUND);
        }

        return script;
    }

    public PracticeDto.AnalysisResponse analyzePracticeVoice(MultipartFile audioFile, String referenceText) {

        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_IS_EMPTY);
        }

        File convertedWavFile = null;
        try {
            try {
                convertedWavFile = audioConverter.convertToWav(audioFile);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.FILE_CONVERT_FAILED);
            }

            try (SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
                 AudioConfig audioConfig = AudioConfig.fromWavFileInput(convertedWavFile.getAbsolutePath())) {

                speechConfig.setSpeechRecognitionLanguage("ko-KR");

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
                    } else if (result.getReason() == ResultReason.NoMatch) {
                        throw new BusinessException(ErrorCode.VOICE_RECOGNITION_FAILED);
                    } else {
                        throw new BusinessException(ErrorCode.VOICE_ANALYSIS_FAILED);
                    }
                }
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VOICE_ANALYSIS_FAILED);
        } finally {
            if (convertedWavFile != null && convertedWavFile.exists()) {
                convertedWavFile.delete();
            }
        }
    }

    private PracticeDto.AnalysisResponse extractAnalysisResult(SpeechRecognitionResult result, String script) throws Exception {
        PronunciationAssessmentResult assessment = PronunciationAssessmentResult.fromResult(result);
        double accuracy = assessment.getAccuracyScore();

        String jsonResult = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
        JsonNode rootNode = objectMapper.readTree(jsonResult);

        JsonNode wordsNode = rootNode.path("NBest").path(0).path("Words");

        long totalDurationDocs = 0;

        if (wordsNode.isArray() && wordsNode.size() > 0) {
            JsonNode firstWord = wordsNode.get(0);
            long startOffset = firstWord.path("Offset").asLong();

            JsonNode lastWord = wordsNode.get(wordsNode.size() - 1);
            long endDocs = lastWord.path("Offset").asLong() + lastWord.path("Duration").asLong();

            totalDurationDocs = endDocs - startOffset;
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