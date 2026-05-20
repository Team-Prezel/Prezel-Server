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
import org.springframework.web.multipart.MultipartFile;

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

        LocalDate presentationDate = request.getDate() != null ? request.getDate().toLocalDate() : null;

        Presentation presentation = Presentation.builder()
                .user(customUserDetails.getUser())
                .title(request.getName())
                .presentationDate(presentationDate)
                .type(request.getType())
                .purpose(request.getPurpose())
                .style(request.getStyle())
                .audience(request.getAudience())
                .script(request.getScript())
                .build();

        Presentation savedPresentation = presentationRepository.save(presentation);

        PresentationDTO.SummaryResponse response = presentationService.analyzePresentation(savedPresentation, request.getAudio());
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/{presentationId}/re-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PresentationDTO.SummaryResponse> reAnalyzeRecording(
            @PathVariable Long presentationId,
            @RequestParam("audio") MultipartFile audio,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        PresentationDTO.SummaryResponse response = presentationService.reAnalyzePresentation(presentationId, audio, customUserDetails.getUser());
        return ApiResponse.success(response);
    }

    @GetMapping("/analyze/{analysisResultId}/words")
    public ApiResponse<PresentationDTO.WordDetailResponse> getWordDetails(@PathVariable Long analysisResultId) {
        return ApiResponse.success(presentationService.getWordDetails(analysisResultId));
    }

    @DeleteMapping("/analyze/{analysisResultId}")
    public ApiResponse<Void> cancelAnalysis(@PathVariable Long analysisResultId) {
        presentationService.deleteAnalysisResult(analysisResultId);
        return ApiResponse.success();
    }
}