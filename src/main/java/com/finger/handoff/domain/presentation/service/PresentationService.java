package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.badge.event.BadgeEvent;
import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.AnalysisResult;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.AnalysisResultRepository;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.review.entity.Review;
import com.finger.handoff.domain.review.repository.ReviewRepository;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.global.audio.AudioConverter;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.finger.handoff.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final ReviewRepository reviewRepository;

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PresentationDTO.SummaryResponse analyzePresentation(Presentation presentation, MultipartFile audio) {

        PresentationDTO.SummaryResponse response = executeAnalysis(presentation, audio);

        eventPublisher.publishEvent(new BadgeEvent(presentation.getUser().getId(), "PRESENTATION_CREATED"));

        return response;
    }

    @Transactional
    public PresentationDTO.SummaryResponse reAnalyzePresentation(Long presentationId, MultipartFile audio, User user) {
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESENTATION_NOT_FOUND));

        if (!presentation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        PresentationDTO.SummaryResponse response = executeAnalysis(presentation, audio);

        eventPublisher.publishEvent(new BadgeEvent(user.getId(), "ANALYZE_COMPLETED"));

        return response;
    }

    private PresentationDTO.SummaryResponse executeAnalysis(Presentation presentation, MultipartFile audio) {
        File wavFile = null;
        try {
            String audioUrl = s3Service.uploadAudioFile(audio);
            wavFile = audioConverter.convertToWav(audio);

            AzureSpeechService.AzureAnalysisDto azureResult =
                    azureSpeechService.analyzePronunciation(wavFile.getAbsolutePath(), presentation.getScript());

            String wordDetailsJson = "[]";
            if (azureResult.getWordDetails() != null) {
                wordDetailsJson = objectMapper.writeValueAsString(azureResult.getWordDetails());
            }

            GeminiService.GeminiAllInOneResponse aiResponse;
            try {
                aiResponse = geminiService.analyzeAll(azureResult, presentation.getScript());
            } catch (Exception e) {
                log.error("AI 통합 분석 실패 (강제 방어 모드 발동): ", e);
                aiResponse = GeminiService.GeminiAllInOneResponse.builder()
                        .summaryFeedback("현재 AI 서버 혼잡으로 요약을 생성할 수 없습니다.")
                        .spellErrorCount(0)
                        .grammarErrorCount(0)
                        .scriptDetailsJson("[]")
                        .expectedQuestionsJson("[]")
                        .build();
            }

            String summaryFeedback = aiResponse.getSummaryFeedback();
            int spellErrorCount = aiResponse.getSpellErrorCount();
            int grammarErrorCount = aiResponse.getGrammarErrorCount();
            String scriptDetailsJson = aiResponse.getScriptDetailsJson();
            String expectedQuestionsJson = aiResponse.getExpectedQuestionsJson();

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
                    .spellErrorCount(spellErrorCount)
                    .grammarErrorCount(grammarErrorCount)
                    .scriptDetailsJson(scriptDetailsJson)
                    .expectedQuestionsJson(expectedQuestionsJson)
                    .build();

            presentation.getAnalysisResults().add(analysisResult);
            analysisResult = analysisResultRepository.saveAndFlush(analysisResult);

            List<PresentationDTO.ExpectedQuestionData> expectedQuestions = new ArrayList<>();
            boolean hasScript = presentation.getScript() != null && !presentation.getScript().trim().isEmpty();

            try {
                if (expectedQuestionsJson != null && !expectedQuestionsJson.equals("[]") && !expectedQuestionsJson.trim().isEmpty()) {
                    expectedQuestions = objectMapper.readValue(expectedQuestionsJson,
                            new TypeReference<List<PresentationDTO.ExpectedQuestionData>>() {});
                }
            } catch (Exception e) {
                log.error("예상 질문 JSON 파싱 에러: {}", expectedQuestionsJson, e);
            }

            if (hasScript) {
                if (expectedQuestions.isEmpty()) {
                    expectedQuestions.add(PresentationDTO.ExpectedQuestionData.builder()
                            .question("예상 질문을 생성하는 데 일시적인 지연이 발생했습니다.")
                            .answer("AI 서버 혼잡으로 응답을 받지 못했습니다. 잠시 후 재녹음을 통해 다시 시도해 주세요.")
                            .build());
                }
            } else {
                expectedQuestions = new ArrayList<>();
            }

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

            LocalDate analysisDate = analysisResult.getCreatedAt() != null ? analysisResult.getCreatedAt().toLocalDate() : LocalDate.now();

            int duration = azureResult.getDurationSeconds() != null ? azureResult.getDurationSeconds() : 0;
            String formattedDuration = String.format("%02d:%02d", duration / 60, duration % 60);
            int totalErrorCount = spellErrorCount + grammarErrorCount;

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
                    .spellErrorCount(spellErrorCount)
                    .grammarErrorCount(grammarErrorCount)
                    .totalErrorCount(totalErrorCount)
                    .expectedQuestions(expectedQuestions)
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
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다."));

        List<PresentationDTO.ScriptAnalysisDetail> scriptDetails = null;
        try {
            if (result.getScriptDetailsJson() != null && !result.getScriptDetailsJson().equals("[]")) {
                scriptDetails = objectMapper.readValue(result.getScriptDetailsJson(),
                        new TypeReference<List<PresentationDTO.ScriptAnalysisDetail>>() {});
            }
        } catch (JsonProcessingException e) {
            log.error("대본 파싱 오류", e);
        }

        return PresentationDTO.ScriptDetailResponse.builder()
                .presentationId(result.getPresentation().getId())
                .audioUrl(result.getAudioUrl())
                .originalScript(result.getPresentation().getScript())
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

    @Transactional(readOnly = true)
    public List<PresentationDTO.PresentationListResponse> getUpcomingPresentations(User user) {
        LocalDate today = LocalDate.now();
        List<Presentation> presentations = presentationRepository
                .findByUserIdAndPresentationDateGreaterThanEqualOrderByPresentationDateAsc(user.getId(), today);

        return presentations.stream()
                .map(this::mapToPresentationListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PresentationDTO.PresentationListResponse> getPastPresentations(User user) {
        LocalDate today = LocalDate.now();
        List<Presentation> presentations = presentationRepository
                .findByUserIdAndPresentationDateLessThanOrderByPresentationDateDesc(user.getId(), today);

        return presentations.stream()
                .map(this::mapToPresentationListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PresentationDTO.UpcomingDetailResponse getUpcomingPresentationDetail(Long presentationId, User user) {
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESENTATION_NOT_FOUND));

        if (!presentation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (presentation.getPresentationDate() != null && presentation.getPresentationDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        PresentationDTO.SummaryResponse summaryResponse = buildSummaryResponseFromDB(presentation);

        return PresentationDTO.UpcomingDetailResponse.builder()
                .analysisResult(summaryResponse)
                .build();
    }

    @Transactional
    public PresentationDTO.PastDetailResponse getPastPresentationDetail(Long presentationId, User user) {
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESENTATION_NOT_FOUND));

        if (!presentation.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (presentation.getPresentationDate() != null && !presentation.getPresentationDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        PresentationDTO.SummaryResponse summaryResponse = buildSummaryResponseFromDB(presentation);

        String reviewContent = reviewRepository.findByIdAndUserId(presentationId, user.getId())
                .map(Review::getContent)
                .orElse(null);

        long practiceCount = presentation.getPracticeCount();

        return PresentationDTO.PastDetailResponse.builder()
                .analysisResult(summaryResponse)
                .reviewContent(reviewContent)
                .practiceCount(practiceCount)
                .build();
    }


    private PresentationDTO.PresentationListResponse mapToPresentationListResponse(Presentation presentation) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = presentation.getPresentationDate();

        String dDay = "";
        if (targetDate != null) {
            long days = ChronoUnit.DAYS.between(today, targetDate);
            if (days == 0) {
                dDay = "D-Day";
            } else if (days > 0) {
                dDay = "D-" + days;
            } else {
                dDay = "D+" + Math.abs(days);
            }
        }

        return PresentationDTO.PresentationListResponse.builder()
                .presentationId(presentation.getId())
                .title(presentation.getTitle())
                .presentationDate(presentation.getPresentationDate())
                .dDay(dDay)
                .type(presentation.getType())
                .purpose(presentation.getPurpose())
                .style(presentation.getStyle())
                .audience(presentation.getAudience())
                .build();
    }

    private PresentationDTO.SummaryResponse buildSummaryResponseFromDB(Presentation presentation) {
        List<AnalysisResult> historyResults = analysisResultRepository.findByPresentationIdOrderByCreatedAtAsc(presentation.getId());

        if (historyResults.isEmpty()) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND);
        }

        AnalysisResult latestResult = historyResults.get(historyResults.size() - 1);

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

        List<PresentationDTO.ExpectedQuestionData> expectedQuestions = new ArrayList<>();
        try {
            if (latestResult.getExpectedQuestionsJson() != null && !latestResult.getExpectedQuestionsJson().equals("[]")) {
                expectedQuestions = objectMapper.readValue(latestResult.getExpectedQuestionsJson(),
                        new TypeReference<List<PresentationDTO.ExpectedQuestionData>>() {});
            }
        } catch (Exception e) {
            log.error("예상 질문 JSON 파싱 에러", e);
        }

        int duration = latestResult.getDurationSeconds() != null ? latestResult.getDurationSeconds() : 0;
        String formattedDuration = String.format("%02d:%02d", duration / 60, duration % 60);

        int spellError = latestResult.getSpellErrorCount() != null ? latestResult.getSpellErrorCount() : 0;
        int grammarError = latestResult.getGrammarErrorCount() != null ? latestResult.getGrammarErrorCount() : 0;
        int totalErrorCount = spellError + grammarError;

        LocalDate analysisDate = latestResult.getCreatedAt() != null ? latestResult.getCreatedAt().toLocalDate() : LocalDate.now();

        return PresentationDTO.SummaryResponse.builder()
                .presentationId(presentation.getId())
                .analysisResultId(latestResult.getId())
                .name(presentation.getTitle())
                .type(presentation.getType())
                .purpose(presentation.getPurpose())
                .style(presentation.getStyle())
                .audience(presentation.getAudience())
                .analysisDate(analysisDate)
                .durationSeconds(duration)
                .formattedDuration(formattedDuration)
                .spm(latestResult.getSpm())
                .speedEval(latestResult.getSpeedEval())
                .summaryFeedback(latestResult.getSummaryFeedback())
                .accuracyScore(latestResult.getAccuracyScore())
                .scriptMatchRate(latestResult.getScriptMatchRate())
                .spellErrorCount(spellError)
                .grammarErrorCount(grammarError)
                .totalErrorCount(totalErrorCount)
                .expectedQuestions(expectedQuestions)
                .growthGraph(growthGraph)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PresentationDTO.MainScreenResponse> getMainScreenData(User user) {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.minusDays(1);

        List<Presentation> presentations = presentationRepository
                .findTop3ByUserIdAndPresentationDateGreaterThanEqualOrderByPresentationDateAsc(user.getId(), cutoffDate);

        List<PresentationDTO.MainScreenResponse> responses = new ArrayList<>();

        for (Presentation presentation : presentations) {
            LocalDate targetDate = presentation.getPresentationDate();

            long days = ChronoUnit.DAYS.between(today, targetDate);
            String dDay;
            if (days == 0) {
                dDay = "D-Day";
            } else if (days > 0) {
                dDay = "D-" + days;
            } else {
                dDay = "D+" + Math.abs(days);
            }

            boolean isPast = days < 0;

            List<PresentationDTO.GrowthData> growthGraph = null;
            Integer accuracyScoreChange = null;
            Integer scriptMatchRateChange = null;

            if (isPast) {
                List<AnalysisResult> historyResults = analysisResultRepository.findByPresentationIdOrderByCreatedAtAsc(presentation.getId());
                growthGraph = new ArrayList<>();

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

                if (!growthGraph.isEmpty()) {
                    PresentationDTO.GrowthData first = growthGraph.get(0);
                    PresentationDTO.GrowthData last = growthGraph.get(growthGraph.size() - 1);

                    accuracyScoreChange = (int) Math.round(last.getAccuracyScore() - first.getAccuracyScore());
                    scriptMatchRateChange = (int) Math.round(last.getScriptMatchRate() - first.getScriptMatchRate());
                } else {
                    accuracyScoreChange = 0;
                    scriptMatchRateChange = 0;
                }
            }

            responses.add(PresentationDTO.MainScreenResponse.builder()
                    .presentationId(presentation.getId())
                    .type(presentation.getType())
                    .presentationDate(presentation.getPresentationDate())
                    .title(presentation.getTitle())
                    .practiceCount(presentation.getPracticeCount())
                    .dDay(dDay)
                    .isPast(isPast)
                    .growthGraph(growthGraph)
                    .accuracyScoreChange(accuracyScoreChange)
                    .scriptMatchRateChange(scriptMatchRateChange)
                    .build());
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public PresentationDTO.SummaryResponse getAnalysisSummary(Long analysisResultId) {
        AnalysisResult targetResult = analysisResultRepository.findById(analysisResultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

        Presentation presentation = targetResult.getPresentation();

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

        List<PresentationDTO.ExpectedQuestionData> expectedQuestions = new ArrayList<>();
        try {
            if (targetResult.getExpectedQuestionsJson() != null && !targetResult.getExpectedQuestionsJson().equals("[]")) {
                expectedQuestions = objectMapper.readValue(targetResult.getExpectedQuestionsJson(),
                        new TypeReference<List<PresentationDTO.ExpectedQuestionData>>() {});
            }
        } catch (Exception e) {
            log.error("예상 질문 JSON 파싱 에러", e);
        }

        int duration = targetResult.getDurationSeconds() != null ? targetResult.getDurationSeconds() : 0;
        String formattedDuration = String.format("%02d:%02d", duration / 60, duration % 60);

        int spellError = targetResult.getSpellErrorCount() != null ? targetResult.getSpellErrorCount() : 0;
        int grammarError = targetResult.getGrammarErrorCount() != null ? targetResult.getGrammarErrorCount() : 0;

        LocalDate analysisDate = targetResult.getCreatedAt() != null ? targetResult.getCreatedAt().toLocalDate() : LocalDate.now();

        return PresentationDTO.SummaryResponse.builder()
                .presentationId(presentation.getId())
                .analysisResultId(targetResult.getId())
                .name(presentation.getTitle())
                .type(presentation.getType())
                .purpose(presentation.getPurpose())
                .style(presentation.getStyle())
                .audience(presentation.getAudience())
                .analysisDate(analysisDate)
                .durationSeconds(duration)
                .formattedDuration(formattedDuration)
                .spm(targetResult.getSpm())
                .speedEval(targetResult.getSpeedEval())
                .summaryFeedback(targetResult.getSummaryFeedback())
                .accuracyScore(targetResult.getAccuracyScore())
                .scriptMatchRate(targetResult.getScriptMatchRate())
                .spellErrorCount(spellError)
                .grammarErrorCount(grammarError)
                .totalErrorCount(spellError + grammarError)
                .expectedQuestions(expectedQuestions)
                .growthGraph(growthGraph)
                .build();
    }
}