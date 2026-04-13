package com.finger.handoff.domain.oidc.api;

import com.finger.handoff.domain.oidc.dto.request.KakaoJwksResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "kakaoJwksClient", url = "https://kauth.kakao.com")
public interface KakaoJwksClient {

    @GetMapping("/.well-known/jwks.json")
    KakaoJwksResponse getKakaoJwks();
}
