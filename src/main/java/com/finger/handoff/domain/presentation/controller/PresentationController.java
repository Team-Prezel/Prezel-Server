package com.finger.handoff.domain.presentation.controller;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.presentation.service.PresentationService;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/recording")
@RequiredArgsConstructor
public class PresentationController {

    private final PresentationService presentationService;
    private final PresentationRepository presentationRepository;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PresentationDTO.SummaryResponse> analyzeRecording(
            @ModelAttribute PresentationDTO.PresentationRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        // 1. DTO의 날짜 타입 처리 (요청된 날짜의 형변환)
        LocalDate presentationDate = null;
        if (request.getDate() != null) {
            presentationDate = request.getDate().toLocalDate();
        }

        // 2. AuthenticationPrincipal을 통해 가져온 User 정보로 Presentation 엔티티 실제 생성
        Presentation presentation = Presentation.builder()
                .user(customUserDetails.getUser()) // CustomUserDetails 내장 메서드 활용
                .title(request.getName())          // DTO: name -> Entity: title
                .presentationDate(presentationDate)
                .type(request.getType())
                .purpose(request.getPurpose())
                .style(request.getStyle())
                .audience(request.getAudience())
                .script(request.getScript())
                .build();

        // 3. DB에 먼저 저장하여 ID(PK) 발급
        Presentation savedPresentation = presentationRepository.save(presentation);

        // 4. 저장된 엔티티를 서비스로 넘겨서 분석 로직 진행
        PresentationDTO.SummaryResponse response = presentationService.analyzePresentation(request, savedPresentation);

        return ApiResponse.success(response);
    }

    @GetMapping("/analyze/{analysisResultId}/words")
    public ApiResponse<PresentationDTO.WordDetailResponse> getWordDetails(
            @PathVariable Long analysisResultId) {

        PresentationDTO.WordDetailResponse response = presentationService.getWordDetails(analysisResultId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/analyze/{analysisResultId}")
    public ApiResponse<Void> cancelAnalysis(@PathVariable Long analysisResultId) {
        presentationService.deleteAnalysisResult(analysisResultId);
        return ApiResponse.success();
    }

}