package com.finger.handoff.domain.terms.controller;

import com.finger.handoff.domain.terms.dto.TermsAgreementRequest;
import com.finger.handoff.domain.terms.service.TermsService;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "약관(Terms)", description = "이용약관 동의 관련 API")
@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @Operation(summary = "약관 동의 저장", description = "유저가 선택한 약관 동의 내역을 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "약관 동의 성공"),
            @ApiResponse(responseCode = "400", description = "필수 약관 미동의", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 400,\n  \"code\": \"TR002\",\n  \"data\": null,\n  \"message\": \"필수 약관에는 반드시 동의해야 합니다.\"\n}")
            )),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"code\": \"U001\",\n  \"data\": null,\n  \"message\": \"인증이 필요합니다.\"\n}")
            )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 약관 ID", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"code\": \"TR001\",\n  \"data\": null,\n  \"message\": \"존재하지 않는 약관입니다.\"\n}")
            ))
    })
    @PostMapping("/agree")
    public ResponseEntity<String> agreeTerms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody List<TermsAgreementRequest> requests
    ) {
        Long userId = userDetails.getId();
        termsService.saveAgreements(userId, requests);

        return ResponseEntity.ok("약관 동의가 완료되었습니다.");
    }
}