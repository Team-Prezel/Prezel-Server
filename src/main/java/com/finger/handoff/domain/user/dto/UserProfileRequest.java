package com.finger.handoff.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter // 🌟 Multipart 데이터를 바인딩하려면 Setter가 필요합니다!
@NoArgsConstructor
public class UserProfileRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    // 프로필 사진은 선택 사항일 수 있으므로 NotNull을 붙이지 않습니다.
    private MultipartFile profileImage;
}