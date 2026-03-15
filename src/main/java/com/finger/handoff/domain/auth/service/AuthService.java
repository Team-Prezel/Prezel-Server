package com.finger.handoff.domain.auth.service;

import com.finger.handoff.domain.auth.api.KakaoApiClient;
import com.finger.handoff.domain.auth.api.KakaoAuthClient;
import com.finger.handoff.domain.auth.dto.AuthResult;
import com.finger.handoff.domain.auth.dto.Response.KakaoTokenResponse;
import com.finger.handoff.domain.auth.dto.Response.KakaoUserInfoResponse;
import com.finger.handoff.domain.auth.dto.request.TokenReissueRequest;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.domain.user.service.UserService;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.finger.handoff.global.security.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    // 환경변수 가져오기
    @Value("${oauth2.kakao.client-id}")
    private String clientId;

    @Value("${oauth2.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${oauth2.kakao.client-secret}")
    private String clientSecret;

    public AuthResult processKakaoLogin(String code) {
        // 1. 카카오 토큰 받기
        KakaoTokenResponse tokenResponse = kakaoAuthClient.getAccessToken(
                "authorization_code",
                clientId,
                redirectUri,
                code,
                clientSecret
        );

        // 2. 카카오 유저 정보 받기
        KakaoUserInfoResponse userInfo = kakaoApiClient.getUserInfo(
                "Bearer " + tokenResponse.getAccessToken()
        );

        String email = userInfo.getKakaoAccount().getEmail();

        User user = userService.findOrCreateUser(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        userService.updateRefreshToken(user.getId(), refreshToken);
        return new AuthResult(jwtTokenProvider.createAccessToken(user.getId()), refreshToken);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult reissueToken(TokenReissueRequest request) {
        String oldRefreshToken = request.getRefreshToken();

        // 1. Refresh Token 자체의 유효성 검증 (만료일, 서명 등)
        if (!jwtTokenProvider.isValidToken(oldRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 토큰에서 유저 정보(PK) 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(oldRefreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(oldRefreshToken)) {
            user.deleteRefreshToken();
            // noRollbackFor 설정 덕분에 아래 예외가 터져도 deleteRefreshToken()은 커밋됨
            throw new BusinessException(ErrorCode.TOKEN_STOLEN);
        }
        // 4. 새로운 Access Token 및 Refresh Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 5. DB의 Refresh Token 업데이트 (RTR 핵심)
        user.updateRefreshToken(newRefreshToken);

        return new AuthResult(newAccessToken, newRefreshToken);
    }
}
