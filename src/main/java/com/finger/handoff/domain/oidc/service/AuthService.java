package com.finger.handoff.domain.oidc.service;

import com.finger.handoff.domain.oidc.dto.response.LoginResponse;
import com.finger.handoff.domain.oidc.dto.request.TokenReissueRequest;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.repository.UserRepository;
import com.finger.handoff.domain.user.service.UserService;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import com.finger.handoff.global.security.provider.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final KakaoOidcValidator kakaoOidcValidator;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    @Transactional
    public LoginResponse loginWithIdToken(String idToken) {
        Claims payload = kakaoOidcValidator.getValidatedPayload(idToken);

        String email = payload.get("email", String.class);

        User user = userService.findOrCreateUser(email);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }


    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse reissueToken(TokenReissueRequest request) {
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

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.deleteRefreshToken();
    }
}
