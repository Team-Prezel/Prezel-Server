package com.finger.handoff.domain.v2.async.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.badge.event.BadgeEvent;
import com.finger.handoff.domain.presentation.entity.AnalysisResult;
import com.finger.handoff.domain.presentation.repository.AnalysisResultRepository;
import com.finger.handoff.domain.presentation.service.AzureSpeechService;
import com.finger.handoff.domain.presentation.service.GeminiService;
import com.finger.handoff.domain.v2.async.entity.AnalysisStatus;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAnalysisService {

    private final AzureSpeechService azureSpeechService;
    private final GeminiService geminiService;
    private final AnalysisResultRepository analysisResultRepository;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Async("analysisTaskExecutor")
    @Transactional
    public void processAnalysisInBackground(Long presentationId, Long analysisResultId, String localWavPath, String script, Long userId, boolean isNew) {
        AnalysisResult analysisResult = analysisResultRepository.findById(analysisResultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

        try {
            log.info("비동기 분석 시작 - presentationId: {}, analysisResultId: {}", presentationId, analysisResultId);

            AzureSpeechService.AzureAnalysisDto azureResult = azureSpeechService.analyzePronunciation(localWavPath, script);

            GeminiService.GeminiAllInOneResponse geminiResult = geminiService.analyzeAll(azureResult, script, "");

            String wordDetailsJson = "[]";
            if (azureResult.getSentenceDetails() != null) {
                wordDetailsJson = objectMapper.writeValueAsString(azureResult.getSentenceDetails());
            }
            String scriptDetailsJson = geminiResult.getScriptDetailsJson();
            String expectedQuestionsJson = geminiResult.getExpectedQuestionsJson();

            analysisResult.updateAnalysisData(
                    AnalysisStatus.COMPLETED,
                    azureResult.getDurationSeconds(),
                    azureResult.getSpm(),
                    azureResult.getSpeedEval(),
                    azureResult.getAccuracyScore(),
                    azureResult.getScriptMatchRate(),
                    geminiResult.getSummaryFeedback(),
                    wordDetailsJson,
                    geminiResult.getSpellErrorCount(),
                    geminiResult.getGrammarErrorCount(),
                    scriptDetailsJson,
                    expectedQuestionsJson
            );

            if (isNew) {
                eventPublisher.publishEvent(new BadgeEvent(userId, "PRESENTATION_CREATED"));
            } else {
                eventPublisher.publishEvent(new BadgeEvent(userId, "ANALYZE_COMPLETED"));
            }

            fcmService.sendAnalysisResultPush(userId, presentationId, analysisResultId, AnalysisStatus.COMPLETED, script, analysisResult.getAudioUrl());
            log.info("비동기 분석 완료 - presentationId: {}, analysisResultId: {}", presentationId, analysisResultId);

        } catch (Exception e) {
            log.error("비동기 분석 중 예외 발생 - presentationId: {}, analysisResultId: {}", presentationId, analysisResultId, e);

            AnalysisStatus errorStatus = determineErrorStatus(e);
            analysisResult.updateStatus(errorStatus);
            analysisResultRepository.saveAndFlush(analysisResult);

            fcmService.sendAnalysisResultPush(userId, presentationId, analysisResultId, errorStatus, script, analysisResult.getAudioUrl());
        } finally {
            if (localWavPath != null) {
                try {
                    File wavFile = new File(localWavPath);
                    if (wavFile.exists()) {
                        boolean deleted = wavFile.delete();
                        log.info("임시 로컬 WAV 파일 정리 완료: {} (성공: {})", localWavPath, deleted);
                    }
                } catch (Exception ex) {
                    log.warn("임시 로컬 WAV 파일 삭제 중 에러 발생: {}", localWavPath, ex);
                }
            }
        }
    }

    private AnalysisStatus determineErrorStatus(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof BusinessException be) {
                ErrorCode code = be.getErrorCode();
                if (code == ErrorCode.VOICE_RECOGNITION_FAILED || code == ErrorCode.SILENT_AUDIO_DETECTED) {
                    return AnalysisStatus.ERR_VOICE_RECOG;
                } else if (code == ErrorCode.FILE_CONVERT_FAILED || code == ErrorCode.INVALID_FILE_EXTENSION
                        || code == ErrorCode.FILE_UPLOAD_FAILED || code == ErrorCode.FILE_IS_EMPTY) {
                    return AnalysisStatus.ERR_FILE_RECOG;
                }
            }
            current = current.getCause();
        }

        String fullMessage = (e.getMessage() != null ? e.getMessage() : "") + " "
                + (e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : "");
        String upper = fullMessage.toUpperCase();

        if (upper.contains("VOICE_RECOGNITION_FAILED") || upper.contains("SILENT") || fullMessage.contains("음성을 인식하지 못했")) {
            return AnalysisStatus.ERR_VOICE_RECOG;
        } else if (upper.contains("FILE") || upper.contains("FORMAT") || fullMessage.contains("포맷") || fullMessage.contains("변환") || fullMessage.contains("파일 형식")) {
            return AnalysisStatus.ERR_FILE_RECOG;
        }

        return AnalysisStatus.ERR_ANALYSIS;
    }
}
