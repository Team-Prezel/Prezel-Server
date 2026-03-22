package com.finger.handoff.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserNicknameUpdateRequest {

    @NotBlank(message = "변경할 닉네임을 입력해주세요.")
    private String newNickname;

}