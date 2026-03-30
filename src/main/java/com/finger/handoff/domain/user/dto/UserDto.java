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

    private String profileImgUrl;
}
