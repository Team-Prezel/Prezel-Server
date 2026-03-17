package com.finger.handoff.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResult {

    private String accessToken;
    private String refreshToken;

}