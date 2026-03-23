package com.finger.handoff.domain.auth.controller;

import com.finger.handoff.domain.auth.dto.AuthResult;
import com.finger.handoff.domain.auth.dto.request.TokenReissueRequest;
import com.finger.handoff.domain.auth.service.AuthService;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증(Auth)", description = "카카오 로그인 및 토큰 재발급 관련 API")
@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "카카오 로그인", description = "카카오 인가 코드를 받아 로그인 또는 회원가입을 진행합니다.")
    @GetMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestParam String code) {
        System.out.println("kakao login");
        AuthResult result = authService.processKakaoLogin(code);

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 Access Token과 Refresh Token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<AuthResult> reissue(@RequestBody TokenReissueRequest request) {

        AuthResult result = authService.reissueToken(request);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "로그아웃", description = "DB에 저장된 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();

        authService.logout(userId);

        return ResponseEntity.ok("로그아웃이 성공적으로 완료되었습니다.");
    }
}