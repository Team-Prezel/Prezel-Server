package com.finger.handoff.domain.presentation.controller;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.presentation.service.PresentationService;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "Presentation", description = "발표 분석 및 관리 API")
@RestController
@RequestMapping("/recording")
@RequiredArgsConstructor
public class PresentationController {

    private final PresentationService presentationService;
    private final PresentationRepository presentationRepository;

    @Operation(
            summary = "발표 음성 및 대본 분석 시작",
            description = "사용자가 업로드한 녹음 파일과 대본을 바탕으로 AI 분석을 수행합니다. 대본이 없는 경우 음성 파일만으로 분석이 진행됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발표 분석 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터 또는 파일 형식", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "F002", description = "지원하지 않는 파일 형식", value = "{\"status\": 400, \"code\": \"F002\", \"message\": \"지원하지 않는 오디오 파일 형식입니다.\"}"),
                            @ExampleObject(name = "V001", description = "음성 데이터 인식 실패", value = "{\"status\": 400, \"code\": \"V001\", \"message\": \"음성 인식(STT) 처리에 실패했습니다.\"}")
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 자격 증명 무효", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "U001", description = "JWT 토큰 누락/만료", value = "{\"status\": 401, \"code\": \"U001\", \"message\": \"인증 자격 증명이 유효하지 않습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "U003", description = "존재하지 않는 사용자", value = "{\"status\": 404, \"code\": \"U003\", \"message\": \"해당 사용자를 찾을 수 없습니다.\"}"),
                            @ExampleObject(name = "F001", description = "빈 파일 업로드 시도", value = "{\"status\": 404, \"code\": \"F001\", \"message\": \"업로드된 파일이 비어 있습니다.\"}")
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 (AI 엔진 에러 등)", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "S001", description = "AI 엔진 분석 실패", value = "{\"status\": 500, \"code\": \"S001\", \"message\": \"AI 분석 서버와의 통신 중 오류가 발생했습니다.\"}")))
    })
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PresentationDTO.SummaryResponse> analyzeRecording(
            @ModelAttribute PresentationDTO.PresentationRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        LocalDate presentationDate = request.getDate() != null ? request.getDate().toLocalDate() : null;

        Presentation presentation = Presentation.builder()
                .user(customUserDetails.getUser())
                .title(request.getName())
                .presentationDate(presentationDate)
                .type(request.getType())
                .purpose(request.getPurpose())
                .style(request.getStyle())
                .audience(request.getAudience())
                .script(request.getScript())
                .build();

        Presentation savedPresentation = presentationRepository.save(presentation);

        PresentationDTO.SummaryResponse response = presentationService.analyzePresentation(savedPresentation, request.getAudio());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "기존 발표 재녹음 및 재분석",
            description = "기존에 등록된 발표(presentationId)에 대해 새로운 음성 파일을 업로드하여 재분석을 수행합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발표 재분석 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "권한 없음 또는 잘못된 요청", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 400, \"code\": \"V001\", \"message\": \"본인의 발표만 재녹음할 수 있습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "발표를 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"code\": \"P001\", \"message\": \"존재하지 않는 발표입니다.\"}")))
    })
    @PostMapping(value = "/{presentationId}/re-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PresentationDTO.SummaryResponse> reAnalyzeRecording(
            @Parameter(description = "재분석할 발표 ID") @PathVariable Long presentationId,
            @RequestParam("audio") MultipartFile audio,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        PresentationDTO.SummaryResponse response = presentationService.reAnalyzePresentation(presentationId, audio, customUserDetails.getUser());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "단어별 분석 상세 조회",
            description = "특정 발표 분석 결과(analysisResultId)에 대한 단어별 발음 정확도, 상태 등의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"code\": \"A001\", \"message\": \"분석 결과를 찾을 수 없습니다.\"}")))
    })
    @GetMapping("/analyze/{analysisResultId}/words")
    public ApiResponse<PresentationDTO.WordDetailResponse> getWordDetails(@Parameter(description = "조회할 분석 결과 ID") @PathVariable Long analysisResultId) {
        return ApiResponse.success(presentationService.getWordDetails(analysisResultId));
    }

    @Operation(
            summary = "대본 교정 및 피드백 상세 조회",
            description = "특정 발표 분석 결과에 대한 맞춤법, 주술 호응 교정 내역 및 원본 대본을 조회합니다. 대본 없이 진행된 분석은 빈 리스트가 반환됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"code\": \"A001\", \"message\": \"분석 결과를 찾을 수 없습니다.\"}")))
    })
    @GetMapping("/analyze/{analysisResultId}/scripts")
    public ApiResponse<PresentationDTO.ScriptDetailResponse> getScriptDetails(@Parameter(description = "조회할 분석 결과 ID") @PathVariable Long analysisResultId) {
        return ApiResponse.success(presentationService.getScriptDetails(analysisResultId));
    }

    @Operation(
            summary = "발표 분석 결과 삭제",
            description = "특정 발표 분석 결과(음성 파일 및 분석 데이터)를 삭제합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": 404, \"code\": \"A001\", \"message\": \"분석 결과를 찾을 수 없습니다.\"}")))
    })
    @DeleteMapping("/analyze/{analysisResultId}")
    public ApiResponse<Void> cancelAnalysis(@Parameter(description = "삭제할 분석 결과 ID") @PathVariable Long analysisResultId) {
        presentationService.deleteAnalysisResult(analysisResultId);
        return ApiResponse.success();
    }
}