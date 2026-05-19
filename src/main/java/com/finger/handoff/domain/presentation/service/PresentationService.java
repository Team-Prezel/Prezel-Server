package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.AnalysisResult;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.AnalysisResultRepository;
import com.finger.handoff.global.audio.AudioConverter;
import com.finger.handoff.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresentationService {

    private final AudioConverter audioConverter;
    private final AzureSpeechService azureSpeechService;
    private final GeminiService geminiService;
    private final S3Service s3Service;
    private final AnalysisResultRepository analysisResultRepository;
    private final ObjectMapper objectMapper;

    // 1️⃣ [API 1] 발표 분석 및 요약 리포트 반환 (저장 로직 포함)
    @Transactional
    public PresentationDTO.SummaryResponse analyzePresentation(PresentationDTO.PresentationRequest request, Presentation savedPresentation) {
        File wavFile = null;
        try {
            // 1. 오디오 포맷 변환 및 S3 업로드
            String audioUrl = s3Service.uploadAudioFile(request.getAudio());
            wavFile = audioConverter.convertToWav(request.getAudio());

            // 2. Azure Speech API 분석
            AzureSpeechService.AzureAnalysisDto azureResult =
                    azureSpeechService.analyzePronunciation(wavFile.getAbsolutePath(), request.getScript());

            // 3. Gemini 요약 피드백 생성
            String summaryFeedback = geminiService.generateSummaryFeedback(azureResult);

            // 4. 단어 상세 내역을 JSON으로 변환
            String wordDetailsJson = "[]";
            if (azureResult.getWordDetails() != null) {
                wordDetailsJson = objectMapper.writeValueAsString(azureResult.getWordDetails());
            }

            // 5. AnalysisResult 엔티티 DB 저장
            AnalysisResult analysisResult = AnalysisResult.builder()
                    .presentation(savedPresentation)
                    .durationSeconds(azureResult.getDurationSeconds())
                    .spm(azureResult.getSpm())
                    .speedEval(azureResult.getSpeedEval())
                    .accuracyScore(azureResult.getAccuracyScore())
                    .scriptMatchRate(azureResult.getScriptMatchRate())
                    .summaryFeedback(summaryFeedback)
                    .audioUrl(audioUrl)               // 🔥 오디오 링크 저장
                    .wordDetailsJson(wordDetailsJson) // 🔥 단어 내역 저장
                    .build();

            analysisResult = analysisResultRepository.save(analysisResult);

            // 🔥 추가됨: 분석일(createdAt)이 영속화 지연으로 null일 경우를 대비한 방어 로직
            LocalDateTime analysisDate = analysisResult.getCreatedAt() != null ?
                    analysisResult.getCreatedAt() : LocalDateTime.now();

            // 6. 발표 시간 포맷팅 (예: "02:30") NPE 방지 추가
            int duration = azureResult.getDurationSeconds() != null ? azureResult.getDurationSeconds() : 0;
            int minutes = duration / 60;
            int seconds = duration % 60;
            String formattedDuration = String.format("%02d:%02d", minutes, seconds);

            // 7. 요약 DTO 반환 (WordDetails 제거됨)
            return PresentationDTO.SummaryResponse.builder()
                    .presentationId(savedPresentation.getId())
                    .name(request.getName())
                    .type(request.getType())
                    .purpose(request.getPurpose())
                    .style(request.getStyle())
                    .audience(request.getAudience())
                    .analysisDate(analysisDate)
                    .durationSeconds(duration)
                    .formattedDuration(formattedDuration)
                    .spm(azureResult.getSpm())
                    .speedEval(azureResult.getSpeedEval())
                    .summaryFeedback(summaryFeedback)
                    .accuracyScore(azureResult.getAccuracyScore())
                    .scriptMatchRate(azureResult.getScriptMatchRate())
                    // .growthGraph(getGrowthGraphData(savedPresentation.getId())) // 이전 회차 로직 연결 시 사용
                    .build();

        } catch (Exception e) {
            log.error("발표 분석 중 오류 발생", e);
            throw new RuntimeException("분석 중 오류 발생", e);
        } finally {
            if (wavFile != null && wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    // 2️⃣ [API 2] 단어 상세 분석 및 오디오 링크 반환
    @Transactional(readOnly = true)
    public PresentationDTO.WordDetailResponse getWordDetails(Long analysisResultId) {
        AnalysisResult result = analysisResultRepository.findById(analysisResultId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다. id: " + analysisResultId));

        List<PresentationDTO.WordAnalysisDetail> wordDetails = null;
        try {
            // DB에 저장된 JSON을 다시 List 객체로 변환
            if (result.getWordDetailsJson() != null && !result.getWordDetailsJson().equals("[]")) {
                wordDetails = objectMapper.readValue(result.getWordDetailsJson(),
                        new TypeReference<List<PresentationDTO.WordAnalysisDetail>>() {});
            }
        } catch (JsonProcessingException e) {
            log.error("단어 데이터 파싱 중 오류 발생", e);
            throw new RuntimeException("단어 데이터 파싱 중 오류 발생", e);
        }

        return PresentationDTO.WordDetailResponse.builder()
                .presentationId(result.getPresentation().getId())
                .audioUrl(result.getAudioUrl()) // 🔥 S3 오디오 링크 반환 (모달 오디오 재생용)
                .wordDetails(wordDetails)       // 🔥 타임스탬프(start/endTimeMs) 포함 리스트
                .build();
    }

    @Transactional
    public void deleteAnalysisResult(Long analysisResultId) {
        AnalysisResult result = analysisResultRepository.findById(analysisResultId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다. id: " + analysisResultId));

        // 1. S3에서 오디오 파일 삭제
        if (result.getAudioUrl() != null) {
            s3Service.deleteAudioFile(result.getAudioUrl());
        }

        // 2. 부모(Presentation)와의 연관관계 안전하게 끊기
        Presentation presentation = result.getPresentation();
        if (presentation != null) {
            presentation.setAnalysisResult(null);
        }

        // 3. DB에서 분석 결과 삭제
        analysisResultRepository.delete(result);
    }
}