package com.finger.handoff.domain.admin;

import com.finger.handoff.domain.oidc.dto.response.LoginResponse;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin")
@RequiredArgsConstructor
@RestController
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> testLogin() {
        String testEmail = "admin@admin.com";

        User testUser = userRepository.findByEmail(testEmail).orElseGet(() -> {
            User newUser = User.builder()
                    .email(testEmail)
                    .nickname("관리자")
                    .isTermsAgreement(true)
                    .isProfileComplete(true)
                    .build();
            return userRepository.save(newUser);
        });

        String accessToken = jwtTokenProvider.createAccessToken(testUser.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser.getId());
        testUser.updateRefreshToken(refreshToken);

        userRepository.save(testUser);

        LoginResponse response = new LoginResponse(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
