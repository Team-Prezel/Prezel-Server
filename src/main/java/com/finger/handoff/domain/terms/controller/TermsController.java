package com.finger.handoff.domain.terms.controller;

import com.finger.handoff.domain.terms.dto.TermsAgreementRequest;
import com.finger.handoff.domain.terms.service.TermsService;
import com.finger.handoff.global.security.user.CustomUserDetails;
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