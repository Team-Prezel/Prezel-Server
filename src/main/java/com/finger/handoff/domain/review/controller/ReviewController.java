package com.finger.handoff.domain.review.controller;

import com.finger.handoff.domain.review.dto.ReviewDto;
import com.finger.handoff.domain.review.service.ReviewService;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review", description = "셀프 피드백(회고) API")
@RestController
@RequestMapping("/recording")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "발표 셀프 피드백(회고) 작성",
            description = "발표가 끝난 후 최대 200자까지 셀프 피드백을 작성합니다. 하나의 발표당 하나의 회고만 작성할 수 있습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회고 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "R003", description = "글자 수 초과", value = "{\"status\": 400, \"code\": \"R003\", \"message\": \"셀프 피드백은 최대 200자까지 입력 가능합니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "PR002", description = "타인의 발표에 접근", value = "{\"status\": 403, \"code\": \"PR002\", \"message\": \"해당 데이터에 접근할 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "PR001", description = "존재하지 않는 발표", value = "{\"status\": 404, \"code\": \"PR001\", \"message\": \"존재하지 않는 발표입니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "상태 충돌", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "R002", description = "이미 작성된 회고 존재", value = "{\"status\": 409, \"code\": \"R002\", \"message\": \"이미 회고를 작성한 발표입니다.\"}")))
    })
    @PostMapping("/{presentationId}/review")
    public ApiResponse<ReviewDto.Response> createReview(
            @Parameter(description = "회고를 작성할 발표의 ID") @PathVariable Long presentationId,
            @Valid @RequestBody ReviewDto.Request request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        ReviewDto.Response response = reviewService.saveReview(presentationId, customUserDetails.getUser().getId(), request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "발표 셀프 피드백(회고) 단건 조회",
            description = "특정 발표에 매칭된 셀프 피드백 내용을 상세 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "데이터를 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "R001", description = "회고 데이터 없음", value = "{\"status\": 404, \"code\": \"R001\", \"message\": \"존재하지 않는 회고입니다.\"}")))
    })
    @GetMapping("/{presentationId}/review")
    public ApiResponse<ReviewDto.Response> getReview(
            @Parameter(description = "조회할 회고가 속한 발표의 ID") @PathVariable Long presentationId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        ReviewDto.Response response = reviewService.getReview(presentationId, customUserDetails.getUser().getId());
        return ApiResponse.success(response);
    }
    @Operation(
            summary = "발표 셀프 피드백(회고) 수정",
            description = "작성된 셀프 피드백 내용을 수정합니다. 최대 200자까지 입력 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회고 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "R003", description = "글자 수 초과", value = "{\"status\": 400, \"code\": \"R003\", \"message\": \"셀프 피드백은 최대 200자까지 입력 가능합니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "PR002", description = "타인의 회고에 접근", value = "{\"status\": 403, \"code\": \"PR002\", \"message\": \"해당 데이터에 접근할 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "R001", description = "존재하지 않는 회고", value = "{\"status\": 404, \"code\": \"R001\", \"message\": \"존재하지 않는 회고입니다.\"}")))
    })
    @PatchMapping("/{presentationId}/review")
    public ApiResponse<ReviewDto.Response> updateReview(
            @Parameter(description = "회고를 수정할 발표의 ID") @PathVariable Long presentationId,
            @Valid @RequestBody ReviewDto.Request request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        ReviewDto.Response response = reviewService.updateReview(presentationId, customUserDetails.getUser().getId(), request);
        return ApiResponse.success(response);
    }
}