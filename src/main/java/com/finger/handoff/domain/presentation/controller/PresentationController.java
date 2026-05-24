package com.finger.handoff.domain.presentation.controller;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.presentation.service.PresentationService;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
            description = "사용자가 업로드한 녹음 파일과 대본을 바탕으로 AI 분석을 수행합니다. 대본은 텍스트 파일(.txt)로 업로드하거나 직접 문자열로 입력할 수 있습니다. 대본이 없는 경우 음성 파일만으로 분석이 진행됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발표 분석 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터 또는 파일 형식", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "F004", description = "지원하지 않는 파일 형식", value = "{\"status\": 400, \"code\": \"F004\", \"data\": null, \"message\": \"지원하지 않는 파일 형식입니다.\"}"),
                            @ExampleObject(name = "V001", description = "음성 데이터 인식 실패", value = "{\"status\": 400, \"code\": \"V001\", \"data\": null, \"message\": \"분석할 음성을 인식하지 못했어요.\"}"),
                            @ExampleObject(name = "P001", description = "대본 입력 방식 중복", value = "{\"status\": 400, \"code\": \"P001\", \"data\": null, \"message\": \"직접 입력한 대본과 대본 파일을 동시에 등록할 수 없습니다.\"}")
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 자격 증명 무효", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "U001", description = "JWT 토큰 누락/만료", value = "{\"status\": 401, \"code\": \"U001\", \"data\": null, \"message\": \"인증이 필요합니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "U003", description = "존재하지 않는 사용자", value = "{\"status\": 404, \"code\": \"U003\", \"data\": null, \"message\": \"존재하지 않는 유저입니다.\"}"),
                            @ExampleObject(name = "F001", description = "빈 파일 업로드 시도", value = "{\"status\": 404, \"code\": \"F001\", \"data\": null, \"message\": \"파일이 없습니다.\"}")
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 (AI 엔진 에러 등)", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "V002", description = "AI 엔진 분석 실패", value = "{\"status\": 500, \"code\": \"V002\", \"data\": null, \"message\": \"분석 중 문제가 발생했어요.\"}"),
                            @ExampleObject(name = "P002", description = "대본 파일 읽기 실패", value = "{\"status\": 500, \"code\": \"P002\", \"data\": null, \"message\": \"대본 파일을 읽는 중 오류가 발생했습니다.\"}"),
                            @ExampleObject(name = "F003", description = "오디오 변환 실패", value = "{\"status\": 500, \"code\": \"F003\", \"data\": null, \"message\": \"오디오 파일 변환 중 오류가 발생했습니다.\"}")
                    }))
    })
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PresentationDTO.SummaryResponse> analyzeRecording(
            @ModelAttribute PresentationDTO.PresentationRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        String finalScript = null;

        boolean hasTextScript = request.getScript() != null && !request.getScript().trim().isEmpty();
        boolean hasFileScript = request.getScriptFile() != null && !request.getScriptFile().isEmpty();

        if (hasTextScript && hasFileScript) {
            throw new BusinessException(ErrorCode.INVALID_SCRIPT_REQUEST);
        }

        if (hasFileScript) {
            try {
                finalScript = new String(request.getScriptFile().getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SCRIPT_FILE_READ_FAILED);
            }
        }
        else if (hasTextScript) {
            finalScript = request.getScript();
        }

        LocalDate presentationDate = request.getDate();

        Presentation presentation = Presentation.builder()
                .user(customUserDetails.getUser())
                .title(request.getName())
                .presentationDate(presentationDate)
                .type(request.getType())
                .purpose(request.getPurpose())
                .style(request.getStyle())
                .audience(request.getAudience())
                .script(finalScript)
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "U002", value = "{\"status\": 403, \"code\": \"U002\", \"data\": null, \"message\": \"접근 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "발표를 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "P003", value = "{\"status\": 404, \"code\": \"P003\", \"data\": null, \"message\": \"존재하지 않는 발표입니다.\"}")))
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
                    examples = @ExampleObject(name = "A001", value = "{\"status\": 404, \"code\": \"A001\", \"data\": null, \"message\": \"분석 결과를 찾을 수 없습니다.\"}")))
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
                    examples = @ExampleObject(name = "A001", value = "{\"status\": 404, \"code\": \"A001\", \"data\": null, \"message\": \"분석 결과를 찾을 수 없습니다.\"}")))
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
                    examples = @ExampleObject(name = "A001", value = "{\"status\": 404, \"code\": \"A001\", \"data\": null, \"message\": \"분석 결과를 찾을 수 없습니다.\"}")))
    })
    @DeleteMapping("/analyze/{analysisResultId}")
    public ApiResponse<Void> cancelAnalysis(@Parameter(description = "삭제할 분석 결과 ID") @PathVariable Long analysisResultId) {
        presentationService.deleteAnalysisResult(analysisResultId);
        return ApiResponse.success();
    }

    @Operation(
            summary = "다가오는 발표 목록 조회 (일반 조회)",
            description = "발표일이 지나지 않은(오늘 포함) 발표 목록을 가까운 날짜순으로 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/upcoming")
    public ApiResponse<java.util.List<PresentationDTO.PresentationListResponse>> getUpcomingPresentations(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        return ApiResponse.success(presentationService.getUpcomingPresentations(customUserDetails.getUser()));
    }


    @Operation(
            summary = "지난 발표 목록 조회 (일반 조회)",
            description = "발표일이 지난 발표 목록을 최근 날짜순으로 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/past")
    public ApiResponse<java.util.List<PresentationDTO.PresentationListResponse>> getPastPresentations(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        return ApiResponse.success(presentationService.getPastPresentations(customUserDetails.getUser()));
    }
}