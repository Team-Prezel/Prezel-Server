package com.finger.handoff.domain.v2.async.controller;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.v2.async.dto.PresentationCardResponse;
import com.finger.handoff.domain.v2.async.service.AsyncAnalysisService;
import com.finger.handoff.domain.v2.async.service.PresentationServiceV2;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "Presentation V2", description = "발표 비동기 분석 및 관리 API (V2)")
@RestController
@RequestMapping("/v2/recording")
@RequiredArgsConstructor
public class PresentationControllerV2 {

    private final PresentationServiceV2 presentationServiceV2;
    private final AsyncAnalysisService asyncAnalysisService;

    @Operation(summary = "발표 음성 및 대본 비동기 분석 시작", description = "음성 녹음과 대본을 접수하여 즉시 presentationId와 analysisResultId를 반환하고, 백그라운드에서 분석을 수행합니다. 완료 시 FCM 푸시가 발송됩니다.")
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ApiResponse<Map<String, Long>> analyzeRecordingV2(
            @Valid @ModelAttribute PresentationDTO.PresentationRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        String finalScript = request.getScript();
        if (finalScript == null || finalScript.trim().isEmpty()) {
            MultipartFile scriptFile = request.getScriptFile();
            if (scriptFile == null || scriptFile.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_SCRIPT_REQUEST);
            }
            try {
                finalScript = new String(scriptFile.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SCRIPT_FILE_READ_FAILED);
            }
        }

        var initData = presentationServiceV2.createInitialData(request, finalScript, customUserDetails.getUser());

        asyncAnalysisService.processAnalysisInBackground(
                initData.getPresentationId(),
                initData.getAnalysisResultId(),
                initData.getLocalWavPath(),
                finalScript,
                customUserDetails.getUser().getId(),
                initData.isNew()
        );

        return ApiResponse.success(Map.of(
                "presentationId", initData.getPresentationId(),
                "analysisResultId", initData.getAnalysisResultId()
        ));
    }

    @Operation(summary = "기존 발표 재녹음 및 재분석 (V2)", description = "기존에 등록된 발표(presentationId)에 대해 새로운 음성 파일이나 대본으로 비동기 재분석을 수행합니다.")
    @PostMapping(value = "/{presentationId}/re-analyze", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Long>> reAnalyzeRecordingV2(
            @Parameter(description = "재분석할 발표 ID") @PathVariable Long presentationId,
            @Parameter(description = "재녹음한 음성 파일") @RequestParam(value = "audio", required = false) MultipartFile audio,
            @Parameter(description = "수정할 대본 txt 파일 (선택)") @RequestPart(value = "scriptFile", required = false) MultipartFile scriptFile,
            @Parameter(description = "수정할 대본 텍스트 (선택)") @RequestParam(value = "script", required = false) String script,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        boolean hasTextScript = script != null && !script.trim().isEmpty();
        boolean hasFileScript = scriptFile != null && !scriptFile.isEmpty();

        if (hasTextScript && hasFileScript) {
            throw new BusinessException(ErrorCode.INVALID_SCRIPT_REQUEST);
        }

        String finalScript = null;
        if (hasFileScript) {
            try {
                finalScript = new String(scriptFile.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SCRIPT_FILE_READ_FAILED);
            }
        } else if (hasTextScript) {
            finalScript = script;
        }

        var initData = presentationServiceV2.prepareReAnalysisData(presentationId, audio, finalScript, customUserDetails.getUser());

        asyncAnalysisService.processAnalysisInBackground(
                initData.getPresentationId(),
                initData.getAnalysisResultId(),
                initData.getLocalWavPath(),
                initData.getFinalScript(),
                customUserDetails.getUser().getId(),
                initData.isNew()
        );

        return ApiResponse.success(Map.of(
                "presentationId", initData.getPresentationId(),
                "analysisResultId", initData.getAnalysisResultId()
        ));
    }

    @Operation(
            summary = "준비 중인 발표 목록 조회 (V2)",
            description = "발표일이 지나지 않은 준비 중인 발표 목록을 (상태값: inProcess, complete, default, fail)조회합니다."
    )
    @GetMapping("/upcoming")
    public ApiResponse<List<PresentationCardResponse>> getUpcomingCards(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        return ApiResponse.success(presentationServiceV2.getUpcomingCards(customUserDetails.getUser()));
    }
}