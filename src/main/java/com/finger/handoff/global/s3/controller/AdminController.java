package com.finger.handoff.global.s3.controller;

import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.s3.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin API", description = "관리자 전용 API (뱃지/기초 데이터 세팅용)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final S3Service s3Service;

    @Operation(summary = "S3 뱃지 이미지 업로드", description = "뱃지 이미지를 S3에 올리고 영구 URL을 반환받습니다.")
    @PostMapping(value = "/badges/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadBadgeImage(@RequestPart("file") MultipartFile file, @RequestPart("badgeName") String badgeName) {

        String uploadedUrl = s3Service.uploadBadgeImage(file,badgeName);

        return ApiResponse.success(uploadedUrl);
    }
}