package com.finger.handoff.domain.auth.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KakaoTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
}