package com.finger.handoff.domain.oidc.controller;

import com.finger.handoff.domain.oidc.dto.response.LoginResponse;
import com.finger.handoff.domain.oidc.dto.request.KakaoLoginRequest;
import com.finger.handoff.domain.oidc.dto.request.TokenReissueRequest;
import com.finger.handoff.domain.oidc.service.AuthService;
import com.finger.handoff.domain.user.dto.UserWithdrawRequest;
import com.finger.handoff.domain.user.service.UserService;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.finger.handoff.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증(Auth)", description = "카카오 로그인 및 토큰 재발급 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "카카오 로그인", description = "카카오 ID Token을 받아 로그인 또는 회원가입을 진행합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 ID Token", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"code\": \"T003\",\n  \"data\": null,\n  \"message\": \"idToken이 올바르지 않습니다.\"\n}")
            ))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<KakaoLoginRequest>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        LoginResponse result = authService.loginWithIdToken(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 Access Token과 Refresh Token을 재발급합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 에러 (만료, 탈취 의심 등)", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "T001 - 토큰 만료/유효하지 않음", value = "{\n  \"status\": 401,\n  \"code\": \"T001\",\n  \"data\": null,\n  \"message\": \"유효하지 않거나 만료된 토큰입니다.\"\n}"),
                            @ExampleObject(name = "T002 - 토큰 탈취 의심", value = "{\n  \"status\": 401,\n  \"code\": \"T002\",\n  \"data\": null,\n  \"message\": \"토큰 탈취가 의심되어 강제 로그아웃 되었습니다.\"\n}")
                    }
            )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"code\": \"U003\",\n  \"data\": null,\n  \"message\": \"존재하지 않는 유저입니다.\"\n}")
            ))
    })
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(@RequestBody TokenReissueRequest request) {
        LoginResponse result = authService.reissueToken(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "로그아웃", description = "DB에 저장된 Refresh Token을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 (Header에 Access Token 누락)", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"code\": \"U001\",\n  \"data\": null,\n  \"message\": \"인증이 필요합니다.\"\n}")
            ))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원탈퇴", description = "DB에 저장된 User정보를 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요 (Header에 Access Token 누락)", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"code\": \"U001\",\n  \"data\": null,\n  \"message\": \"인증이 필요합니다.\"\n}")
            ))
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserWithdrawRequest request) {

        Long userId = userDetails.getId();
        userService.withdraw(userId, request);

        return ResponseEntity.ok(ApiResponse.success());
    }
}