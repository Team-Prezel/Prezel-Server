package com.finger.handoff.global.security.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import io.jsonwebtoken.security.SignatureException;

@Component
@Slf4j
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityTime;
    private final long refreshTokenValidityTime;

    // application.yml에서 설정값 가져오기
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityTime,
            @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityTime) {

        // 시크릿 키를 암호화 알고리즘에 맞게 안전한 Key 객체로 변환 (jjwt 0.11.5 방식)
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 초(second) 단위를 밀리초(millisecond)로 변환
        this.accessTokenValidityTime = accessTokenValidityTime * 1000;
        this.refreshTokenValidityTime = refreshTokenValidityTime * 1000;
    }

    // 🎟️ 1. Access Token 생성
    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenValidityTime);
    }

    // 🎟️ 2. Refresh Token 생성
    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenValidityTime);
    }

    // 내부 공통 토큰 생성 로직
    private String createToken(Long userId, long expireTime) {
        Claims claims = Jwts.claims();
        // Payload에 우리 서비스의 핵심 열쇠인 DB PK(id)를 담습니다.
        claims.put("userId", userId);

        Date now = new Date();
        Date validity = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setClaims(claims) // 내용물(Payload)
                .setIssuedAt(now)  // 발급 시간
                .setExpiration(validity) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 비밀키로 암호화 서명(Signature)
                .compact();
    }
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key) // 서버가 가진 시크릿 키로 서명 검증
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("userId", Long.class);
    }
    public boolean isValidToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true; // 예외가 터지지 않았다면 정상적이고 유효한 토큰임

        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다 (내용이 비어있음 등).");
        }

        return false; // 위 예외 중 하나라도 발생했다면 유효하지 않은 토큰임
    }
}