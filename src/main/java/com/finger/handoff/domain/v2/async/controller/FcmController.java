package com.finger.handoff.domain.v2.async.controller;

import com.finger.handoff.domain.v2.async.dto.FcmTokenRequest;
import com.finger.handoff.domain.v2.async.service.FcmService;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FCM Notification", description = "FCM 푸시 알림 기기 토큰 관리 API")
@RestController
@RequestMapping("/v2/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @Operation(summary = "FCM 기기 토큰 등록", description = "사용자의 디바이스 FCM 토큰을 서버에 등록하여 비동기 분석 완료 푸시를 수신할 수 있게 합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/token")
    public ApiResponse<Void> saveToken(
            @Valid @RequestBody FcmTokenRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        fcmService.saveToken(userDetails.getUser(), request.getToken());
        return ApiResponse.success();
    }

    @Operation(summary = "FCM 기기 토큰 삭제 (로그아웃 등)", description = "등록된 기기 토큰을 삭제하여 더 이상 푸시 알림을 받지 않도록 합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/token")
    public ApiResponse<Void> removeToken(
            @Valid @RequestBody FcmTokenRequest request) {

        fcmService.removeToken(request.getToken());
        return ApiResponse.success();
    }
}
