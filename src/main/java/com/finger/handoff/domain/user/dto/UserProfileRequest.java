package com.finger.handoff.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter // 🌟 Multipart 데이터를 바인딩하려면 Setter가 필요합니다!
@NoArgsConstructor
public class UserProfileRequest {

    private String nickname;

    private MultipartFile profileImage;

    private Boolean isImageDeleted;
}