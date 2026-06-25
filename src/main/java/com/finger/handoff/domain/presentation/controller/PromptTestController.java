package com.finger.handoff.domain.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.finger.handoff.domain.presentation.dto.PromptTestDTO;
import com.finger.handoff.domain.presentation.service.GeminiService;
import com.finger.handoff.domain.presentation.service.PresentationService;
import com.finger.handoff.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Prompt Test", description = "기획자용 프롬프트 테스트 API (기능별 개별 테스트)")
@RestController
@RequestMapping("/admin/prompt")
@RequiredArgsConstructor
public class PromptTestController {

    private final PresentationService presentationService;

    @Operation(summary = "1. 요약 피드백 프롬프트 테스트")
    @PostMapping("/test/summary")
    public ApiResponse<JsonNode> testSummaryPrompt(@RequestBody PromptTestDTO.SingleRequest request) {
        JsonNode response = presentationService.testPromptWithExistingData(
                request.getPresentationId(), request.getInstruction(), GeminiService.PromptTestType.SUMMARY);
        return ApiResponse.success(response);
    }

    @Operation(summary = "2. 대본 오류(맞춤법) 프롬프트 테스트")
    @PostMapping("/test/script")
    public ApiResponse<JsonNode> testScriptPrompt(@RequestBody PromptTestDTO.SingleRequest request) {
        JsonNode response = presentationService.testPromptWithExistingData(
                request.getPresentationId(), request.getInstruction(), GeminiService.PromptTestType.SCRIPT);
        return ApiResponse.success(response);
    }

    @Operation(summary = "3. 예상 질문 프롬프트 테스트")
    @PostMapping("/test/question")
    public ApiResponse<JsonNode> testQuestionPrompt(@RequestBody PromptTestDTO.SingleRequest request) {
        JsonNode response = presentationService.testPromptWithExistingData(
                request.getPresentationId(), request.getInstruction(), GeminiService.PromptTestType.QUESTION);
        return ApiResponse.success(response);
    }
}