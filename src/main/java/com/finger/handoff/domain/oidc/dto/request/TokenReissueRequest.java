package com.finger.handoff.domain.oidc.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenReissueRequest {
    @JsonProperty("refresh-token")
    private String refreshToken;
}
