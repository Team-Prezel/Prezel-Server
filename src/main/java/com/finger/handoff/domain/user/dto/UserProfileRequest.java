package com.finger.handoff.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileRequest {

    private String nickname;

    private MultipartFile profileImage;

    private Boolean deleteImage;
}