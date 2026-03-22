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
        KakaoTokenResponse tokenResponse = kakaoAuthClient.getAccessToken(
                "authorization_code",
                clientId,
                redirectUri,
                code,
                clientSecret
        );

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

        if (!jwtTokenProvider.isValidToken(oldRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(oldRefreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(oldRefreshToken)) {
            user.deleteRefreshToken();
            throw new BusinessException(ErrorCode.TOKEN_STOLEN);
        }
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        user.updateRefreshToken(newRefreshToken);

        return new AuthResult(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.deleteRefreshToken();
    }
}
