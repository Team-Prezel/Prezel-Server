package com.finger.handoff.domain.user.controller;

import com.finger.handoff.domain.user.dto.UserDto;
import com.finger.handoff.domain.user.dto.UserProfileRequest;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.service.UserService;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유저(User)", description = "유저 관련 API")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    final private UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 유저의 상세 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal CustomUserDetails userDetails)
    {
        Long userId = userDetails.getId();
        UserDto userDto = userService.getUser(userId);
        return ResponseEntity.ok(userDto);
    }

    @Operation(
            summary = "프로필 수정",
            description = "유저의 닉네임 및 프로필 이미지를 수정합니다. (Multipart/form-data 형식)"
    )
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute UserProfileRequest request
    ) {
        Long userId = userDetails.getId();
        userService.updateProfile(userId, request);

        return ResponseEntity.ok("프로필 설정이 완료되었습니다.");
    }
}
