package com.finger.handoff.domain.oidc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.oidc.api.KakaoJwksClient;
import com.finger.handoff.domain.oidc.dto.request.KakaoJwksResponse;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class KakaoOidcValidator {

    private final KakaoJwksClient kakaoJwksClient;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    public Claims getValidatedPayload(String idToken) {
        try {
            String headerStr = new String(Base64.getUrlDecoder().decode(idToken.split("\\.")[0]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode headerNode = mapper.readTree(headerStr);
            String kid = headerNode.get("kid").asText();

            KakaoJwksResponse jwksResponse = kakaoJwksClient.getKakaoJwks();
            KakaoJwksResponse.KakaoJwk jwk = jwksResponse.getKeys().stream()
                    .filter(k -> k.getKid().equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("일치하는 공개키가 없습니다."));

            byte[] nBytes = Base64.getUrlDecoder().decode(jwk.getN());
            byte[] eBytes = Base64.getUrlDecoder().decode(jwk.getE());
            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer("https://kauth.kakao.com")
                    .requireAudience(clientId)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_ID_TOKEN);
        }
    }
}
