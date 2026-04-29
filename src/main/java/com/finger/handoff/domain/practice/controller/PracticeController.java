package com.finger.handoff.domain.practice.controller;

import com.finger.handoff.domain.practice.dto.PracticeDto;
import com.finger.handoff.domain.practice.service.PracticeService;
import com.finger.handoff.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/practice")
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/sentence")
    public ApiResponse<PracticeDto.SentenceResponse> getRandomSentence() {
        String sentence = practiceService.getRandomSentence();
        PracticeDto.SentenceResponse response = PracticeDto.SentenceResponse.builder()
                .sentence(sentence)
                .build();
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PracticeDto.AnalysisResponse> analyzeAudio(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam("referenceText") String referenceText) {

        PracticeDto.AnalysisResponse response = practiceService.analyzePracticeVoice(audio, referenceText);
        return ApiResponse.success(response);
    }
}
