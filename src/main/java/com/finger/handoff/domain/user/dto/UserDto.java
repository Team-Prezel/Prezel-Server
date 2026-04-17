package com.finger.handoff.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;

    private String email;

    private String nickname;

    private ProfileImageDto profileImgUrl;

    private Boolean isTermsAgreement;

    private Boolean isProfileComplete;


    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProfileImageDto {
        private String url;
        private Boolean isDefault;
    }
}
