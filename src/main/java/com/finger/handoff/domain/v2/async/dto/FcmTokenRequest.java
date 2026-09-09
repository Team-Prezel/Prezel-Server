package com.finger.handoff.domain.v2.async.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Schema(description = "Firebase Cloud Messaging 기기 토큰", example = "fcm_token_sample_string...")
    private String token;

    public FcmTokenRequest(String token) {
        this.token = token;
    }
}
