package com.finger.handoff.domain.badge.controller;

import com.finger.handoff.domain.badge.dto.BadgeDto;
import com.finger.handoff.domain.badge.service.BadgeService;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Badge API", description = "뱃지 및 업적 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/badges")
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(
            summary = "나의 전체 뱃지 목록 조회",
            description = "시스템의 모든 뱃지 목록과 함께, 현재 로그인한 사용자의 해금 여부 및 획득 일자를 조회합니다. 정렬 기준을 쿼리 파라미터로 지정할 수 있습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뱃지 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 권한 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "U001", description = "유효하지 않은 토큰", value = "{\"status\": 401, \"code\": \"U001\", \"message\": \"인증이 필요합니다.\"}")))
    })
    @GetMapping
    public ApiResponse<List<BadgeDto.BadgeListResponse>> getMyBadges(
            @Parameter(description = "정렬 기준<br>" +
                    "- `default`: 기본 정렬 (뱃지 ID 또는 우선순위 순)<br>" +
                    "- `acquired`: 최근 획득순 (획득일자 내림차순)",
                    example = "acquired")
            @RequestParam(defaultValue = "default") String sort,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        // 서비스 단으로 sort 파라미터를 함께 넘겨줍니다.
        List<BadgeDto.BadgeListResponse> response = badgeService.getBadgeList(customUserDetails.getUser().getId(), sort);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "특정 뱃지 상세 조회",
            description = "지정한 뱃지 코드에 대한 세부 조건 정보와 본인의 획득 여부를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뱃지 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 권한 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "U001", description = "유효하지 않은 토큰", value = "{\"status\": 401, \"code\": \"U001\", \"message\": \"인증이 필요합니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "잘못된 뱃지 코드 요청", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "B001", description = "존재하지 않는 뱃지", value = "{\"status\": 404, \"code\": \"B001\", \"message\": \"요청하신 뱃지 정보를 찾을 수 없습니다.\"}")))
    })
    @GetMapping("/{badgeCode}")
    public ApiResponse<BadgeDto.BadgeDetailResponse> getBadgeDetail(
            @Parameter(description = "조회할 뱃지 문자열 코드<br><br>" +
                    "**[뱃지 코드 리스트]**<br>" +
                    "- `START`: 첫 발표 등록<br>" +
                    "- `ANALYZE_AGAIN`: 등록한 발표 재분석<br>" +
                    "- `FIRST_PRACTICE`: 첫 연습 완료<br>" +
                    "- `REVIEW`: 발표 후 첫 회고 작성<br>" +
                    "- `PERFECT_SCORE`: 연습 결과 Perfect 달성<br>" +
                    "- `REPEAT_10`: 총 10회의 발표 등록",
                    example = "START") @PathVariable String badgeCode,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        BadgeDto.BadgeDetailResponse response = badgeService.getBadgeDetail(customUserDetails.getUser().getId(), badgeCode);
        return ApiResponse.success(response);
    }
}