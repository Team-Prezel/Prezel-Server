package com.finger.handoff.domain.user.controller;

import com.finger.handoff.domain.user.dto.UserDto;
import com.finger.handoff.domain.user.dto.UserProfileRequest;
import com.finger.handoff.domain.user.service.UserService;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"code\": \"U001\",\n  \"data\": null,\n  \"message\": \"인증이 필요합니다.\"\n}")
            )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"code\": \"U003\",\n  \"data\": null,\n  \"message\": \"존재하지 않는 유저입니다.\"\n}")
            ))
    })
    @GetMapping
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        UserDto userDto = userService.getUser(userId);
        return ResponseEntity.ok(userDto);
    }

    @Operation(
            summary = "프로필 수정",
            description = "유저의 닉네임 및 프로필 이미지를 수정합니다. (Multipart/form-data 형식)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"code\": \"U001\",\n  \"data\": null,\n  \"message\": \"인증이 필요합니다.\"\n}")
            )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"code\": \"U003\",\n  \"data\": null,\n  \"message\": \"존재하지 않는 유저입니다.\"\n}")
            )),
            @ApiResponse(responseCode = "409", description = "닉네임 중복", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 409,\n  \"code\": \"U004\",\n  \"data\": null,\n  \"message\": \"이미 사용 중인 닉네임입니다.\"\n}")
            )),
            @ApiResponse(responseCode = "424", description = "파일 업로드 실패", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 424,\n  \"code\": \"F002\",\n  \"data\": null,\n  \"message\": \"파일 업로드 중 오류가 발생했습니다.\"\n}")
            ))
    })
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute UserProfileRequest request
    ) {
        Long userId = userDetails.getId();
        userService.updateProfile(userId, request);

        return ResponseEntity.ok("프로필 설정이 완료되었습니다.");
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = "입력한 닉네임의 사용 가능 여부를 반환합니다. (true: 사용 가능, false: 중복)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확인 완료 (true/false 반환)")
    })
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isAvailable = userService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(isAvailable);
    }
}