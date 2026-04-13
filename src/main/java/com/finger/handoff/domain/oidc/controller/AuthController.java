package com.finger.handoff.domain.oidc.controller;

import com.finger.handoff.domain.oidc.dto.response.LoginResponse;
import com.finger.handoff.domain.oidc.dto.request.KakaoLoginRequest;
import com.finger.handoff.domain.oidc.dto.request.TokenReissueRequest;
import com.finger.handoff.domain.oidc.service.AuthService;
import com.finger.handoff.domain.user.dto.UserWithdrawRequest;
import com.finger.handoff.domain.user.service.UserService;
import com.finger.handoff.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        LoginResponse result = authService.loginWithIdToken(request.getIdToken());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reissue")
    public ResponseEntity<LoginResponse> reissue(@RequestBody TokenReissueRequest request) {
        LoginResponse result = authService.reissueToken(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        authService.logout(userId);
        return ResponseEntity.ok("로그아웃이 성공적으로 완료되었습니다.");
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<String> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserWithdrawRequest request) {

        Long userId = userDetails.getId();
        userService.withdraw(userId,request);

        return ResponseEntity.ok("회원 탈퇴가 성공적으로 완료되었습니다. 이용해 주셔서 감사합니다.");
    }
}
