package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.AnalysisResult;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.AnalysisResultRepository;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.global.audio.AudioConverter;
import com.finger.handoff.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final PresentationRepository presentationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PresentationDTO.SummaryResponse analyzePresentation(Presentation presentation, MultipartFile audio) {
        return executeAnalysis(presentation, audio);
    }

    @Transactional
    public PresentationDTO.SummaryResponse reAnalyzePresentation(Long presentationId, MultipartFile audio, User user) {
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 발표입니다."));

        if (!presentation.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 발표만 재녹음할 수 있습니다.");
        }

        return executeAnalysis(presentation, audio);
    }

    private PresentationDTO.SummaryResponse executeAnalysis(Presentation presentation, MultipartFile audio) {
        File wavFile = null;
        try {
            String audioUrl = s3Service.uploadAudioFile(audio);
            wavFile = audioConverter.convertToWav(audio);

            AzureSpeechService.AzureAnalysisDto azureResult =
                    azureSpeechService.analyzePronunciation(wavFile.getAbsolutePath(), presentation.getScript());

            String summaryFeedback = "현재 AI 서버 혼잡으로 피드백을 생성할 수 없습니다.";
            try {
                summaryFeedback = geminiService.generateSummaryFeedback(azureResult);
            } catch (Exception e) {
                log.error("Gemini API 호출 실패: ", e);
            }

            String wordDetailsJson = "[]";
            if (azureResult.getWordDetails() != null) {
                wordDetailsJson = objectMapper.writeValueAsString(azureResult.getWordDetails());
            }

            // 🔥 TODO: 추후 Gemini AI를 통해 맞춤법/주술호응 검사 결과를 받아와서 파싱할 부분
            // 현재는 프론트엔드 API 테스트를 위해 기본값 세팅
            int spellErrorCount = 0;
            int grammarErrorCount = 0;
            String scriptDetailsJson = "[]";

            AnalysisResult analysisResult = AnalysisResult.builder()
                    .presentation(presentation)
                    .durationSeconds(azureResult.getDurationSeconds())
                    .spm(azureResult.getSpm())
                    .speedEval(azureResult.getSpeedEval())
                    .accuracyScore(azureResult.getAccuracyScore())
                    .scriptMatchRate(azureResult.getScriptMatchRate())
                    .summaryFeedback(summaryFeedback)
                    .audioUrl(audioUrl)
                    .wordDetailsJson(wordDetailsJson)
                    .spellErrorCount(spellErrorCount)       // 🔥 추가됨
                    .grammarErrorCount(grammarErrorCount)   // 🔥 추가됨
                    .scriptDetailsJson(scriptDetailsJson)   // 🔥 추가됨
                    .build();

            presentation.getAnalysisResults().add(analysisResult);
            analysisResult = analysisResultRepository.saveAndFlush(analysisResult);

            List<AnalysisResult> historyResults = analysisResultRepository.findByPresentationIdOrderByCreatedAtAsc(presentation.getId());

            List<PresentationDTO.GrowthData> growthGraph = new ArrayList<>();
            int attemptCounter = 1;
            for (AnalysisResult history : historyResults) {
                Double accuracy = history.getAccuracyScore() != null ? history.getAccuracyScore() : 0.0;
                Double scriptMatch = history.getScriptMatchRate() != null ? history.getScriptMatchRate() : 0.0;

                growthGraph.add(PresentationDTO.GrowthData.builder()
                        .attempt(attemptCounter++)
                        .accuracyScore(accuracy)
                        .scriptMatchRate(scriptMatch)
                        .build());
            }

            LocalDateTime analysisDate = analysisResult.getCreatedAt() != null ? analysisResult.getCreatedAt() : LocalDateTime.now();
            int duration = azureResult.getDurationSeconds() != null ? azureResult.getDurationSeconds() : 0;
            String formattedDuration = String.format("%02d:%02d", duration / 60, duration % 60);

            int totalError = spellErrorCount + grammarErrorCount;

            return PresentationDTO.SummaryResponse.builder()
                    .presentationId(presentation.getId())
                    .analysisResultId(analysisResult.getId())
                    .name(presentation.getTitle())
                    .type(presentation.getType())
                    .purpose(presentation.getPurpose())
                    .style(presentation.getStyle())
                    .audience(presentation.getAudience())
                    .analysisDate(analysisDate)
                    .durationSeconds(duration)
                    .formattedDuration(formattedDuration)
                    .spm(azureResult.getSpm())
                    .speedEval(azureResult.getSpeedEval())
                    .summaryFeedback(summaryFeedback)
                    .accuracyScore(azureResult.getAccuracyScore())
                    .scriptMatchRate(azureResult.getScriptMatchRate())
                    .spellErrorCount(spellErrorCount)     // 🔥 탑재
                    .grammarErrorCount(grammarErrorCount) // 🔥 탑재
                    .totalErrorCount(totalError)          // 🔥 탑재
                    .growthGraph(growthGraph)
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

    @Transactional(readOnly = true)
    public PresentationDTO.WordDetailResponse getWordDetails(Long analysisResultId) {
        AnalysisResult result = analysisResultRepository.findById(analysisResultId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다."));

        List<PresentationDTO.WordAnalysisDetail> wordDetails = null;
        try {
            if (result.getWordDetailsJson() != null && !result.getWordDetailsJson().equals("[]")) {
                wordDetails = objectMapper.readValue(result.getWordDetailsJson(),
                        new TypeReference<List<PresentationDTO.WordAnalysisDetail>>() {});
            }
        } catch (JsonProcessingException e) {
            log.error("단어 파싱 오류", e);
        }

        return PresentationDTO.WordDetailResponse.builder()
                .presentationId(result.getPresentation().getId())
                .audioUrl(result.getAudioUrl())
                .wordDetails(wordDetails)
                .build();
    }

    @Transactional(readOnly = true)
    public PresentationDTO.ScriptDetailResponse getScriptDetails(Long analysisResultId) {
        AnalysisResult result = analysisResultRepository.findById(analysisResultId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다. id: " + analysisResultId));

        List<PresentationDTO.ScriptAnalysisDetail> scriptDetails = null;
        try {
            if (result.getScriptDetailsJson() != null && !result.getScriptDetailsJson().equals("[]")) {
                scriptDetails = objectMapper.readValue(result.getScriptDetailsJson(),
                        new TypeReference<List<PresentationDTO.ScriptAnalysisDetail>>() {});
            }
        } catch (JsonProcessingException e) {
            log.error("대본 데이터 파싱 중 오류 발생", e);
            throw new RuntimeException("대본 데이터 파싱 중 오류 발생", e);
        }

        return PresentationDTO.ScriptDetailResponse.builder()
                .presentationId(result.getPresentation().getId())
                .audioUrl(result.getAudioUrl())
                .scriptDetails(scriptDetails)
                .build();
    }

    @Transactional
    public void deleteAnalysisResult(Long analysisResultId) {
        AnalysisResult result = analysisResultRepository.findById(analysisResultId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다."));
        if (result.getAudioUrl() != null) s3Service.deleteAudioFile(result.getAudioUrl());
        analysisResultRepository.delete(result);
    }
}