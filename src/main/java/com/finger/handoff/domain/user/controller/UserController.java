package com.finger.handoff.domain.user.controller;

import com.finger.handoff.domain.user.dto.UserDto;
import com.finger.handoff.domain.user.dto.UserNicknameUpdateRequest;
import com.finger.handoff.domain.user.dto.UserProfileRequest;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.service.UserService;
import com.finger.handoff.global.security.user.CustomUserDetails;
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

    @GetMapping
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal CustomUserDetails userDetails)
    {
        Long userId = userDetails.getId();
        UserDto userDto = userService.getUser(userId);
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<UserDto> updateNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @Valid @RequestBody UserNicknameUpdateRequest nicknameUpdateRequest)
    {
        Long userId = userDetails.getId();
        String newNickname = nicknameUpdateRequest.getNewNickname();
        UserDto userDto = userService.updateUserNickname(userId, newNickname);
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> setupProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute UserProfileRequest request
    ) {
        Long userId = userDetails.getId();
        userService.setupProfile(userId, request);

        return ResponseEntity.ok("프로필 설정이 완료되었습니다.");
    }
}
