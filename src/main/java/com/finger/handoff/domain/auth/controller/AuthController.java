package com.finger.handoff.domain.auth.controller;

import com.finger.handoff.domain.auth.dto.AuthResult;
import com.finger.handoff.domain.auth.dto.request.TokenReissueRequest;
import com.finger.handoff.domain.auth.service.AuthService;
import com.finger.handoff.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestParam String code) {
        System.out.println("kakao login");
        AuthResult result = authService.processKakaoLogin(code);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reissue")
    public ResponseEntity<AuthResult> reissue(@RequestBody TokenReissueRequest request) {

        AuthResult result = authService.reissueToken(request);
        return ResponseEntity.ok(result);
    }
}