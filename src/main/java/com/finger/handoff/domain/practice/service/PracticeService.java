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
            return "DB에 문장이 없어서 기본 문장이 표시됩니다.";
        }

        return script;
    }

    public PracticeDto.AnalysisResponse analyzePracticeVoice(MultipartFile audioFile, String referenceText) {
        File convertedWavFile = null;
        try {
            convertedWavFile = audioConverter.convertToWav(audioFile);

            SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
            speechConfig.setSpeechRecognitionLanguage("ko-KR");
            AudioConfig audioConfig = AudioConfig.fromWavFileInput(convertedWavFile.getAbsolutePath());

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
