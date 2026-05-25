package com.finger.handoff.domain.practice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.badge.event.BadgeEvent;
import com.finger.handoff.domain.practice.dto.PracticeDto;
import com.finger.handoff.domain.practice.repository.PracticeScriptRepository;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.global.audio.AudioConverter;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeService {

    private final ObjectMapper objectMapper;
    private final PracticeScriptRepository repository;
    private final AudioConverter audioConverter;
    private final ApplicationEventPublisher eventPublisher;
    private final PresentationRepository presentationRepository;

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

    @Transactional
    public PracticeDto.AnalysisResponse analyzePracticeVoice(Long userId, MultipartFile audioFile, String referenceText, Long presentationId) {
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
                        PracticeDto.AnalysisResponse response = extractAnalysisResult(result, referenceText);

                        if (presentationId != null) {
                            Presentation presentation = presentationRepository.findById(presentationId)
                                    .orElseThrow(() -> new BusinessException(ErrorCode.PRESENTATION_NOT_FOUND));

                            if (!presentation.getUser().getId().equals(userId)) {
                                throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                            }

                            presentation.addPracticeDate(LocalDate.now());
                        }

                        eventPublisher.publishEvent(new BadgeEvent(userId, "PRACTICE_COMPLETED"));

                        if ("Perfect".equals(response.getOverallEvaluation())) {
                            eventPublisher.publishEvent(new BadgeEvent(userId, "PERFECT_SCORE"));
                        }
                        return response;
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
        if (spm <= 210) speedEval = "느려요";
        else if (spm >= 260) speedEval = "빨라요";
        else speedEval = "적당해요";

        String overallEval;
        if (accuracy >= 95.0 && speedEval.equals("적당해요")) {
            overallEval = "Perfect";
        } else if ((accuracy >= 70.0 && speedEval.equals("적당해요")) || (accuracy >= 95.0 && !speedEval.equals("적당해요"))) {
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