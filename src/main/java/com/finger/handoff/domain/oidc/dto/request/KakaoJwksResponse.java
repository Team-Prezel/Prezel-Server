package com.finger.handoff.domain.oidc.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KakaoJwksResponse {

    private List<KakaoJwk> keys;

    @Getter
    @NoArgsConstructor
    public static class KakaoJwk {
        private String kid;
        private String kty;
        private String alg;
        private String use;
        private String n;
        private String e;
    }
}
