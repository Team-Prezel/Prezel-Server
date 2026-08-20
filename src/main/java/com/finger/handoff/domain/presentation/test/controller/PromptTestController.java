package com.finger.handoff.domain.presentation.test.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.presentation.test.dto.PromptTestDTO;
import com.finger.handoff.domain.presentation.service.GeminiService;
import com.finger.handoff.domain.presentation.service.PresentationService;
import com.finger.handoff.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Admin Prompt Test", description = "기획자용 프롬프트 테스트 API (기능별 개별 테스트)")
@RestController
@RequestMapping("/admin/prompt")
@RequiredArgsConstructor
public class PromptTestController {

    private final PresentationService presentationService;
    private final ObjectMapper objectMapper;

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

    @PostMapping("/test/combine")
    @Operation(summary = "각 유형별 프롬프트 직접 입력 후 테스트")
    public ApiResponse<Map<String, Object>> testRealCombinationPrompt(@RequestBody PromptTestDTO.CombinationRequest request) throws JsonProcessingException {

        StringBuilder combinedInstruction = new StringBuilder();
        if (request.getBasePrompt() != null) combinedInstruction.append(request.getBasePrompt()).append("\n");
        if (request.getTypePrompt() != null) combinedInstruction.append(request.getTypePrompt()).append("\n");
        if (request.getPurposePrompt() != null) combinedInstruction.append(request.getPurposePrompt()).append("\n");
        if (request.getStylePrompt() != null) combinedInstruction.append(request.getStylePrompt()).append("\n");
        if (request.getAudiencePrompt() != null) combinedInstruction.append(request.getAudiencePrompt());

        GeminiService.GeminiAllInOneResponse response =
                presentationService.testAllInOnePrompt(request.getPresentationId(), combinedInstruction.toString());

        Map<String, Object> cleanResponse = new HashMap<>();
        cleanResponse.put("summaryFeedback", response.getSummaryFeedback());
        cleanResponse.put("spellErrorCount", response.getSpellErrorCount());
        cleanResponse.put("grammarErrorCount", response.getGrammarErrorCount());
        cleanResponse.put("scriptDetails", objectMapper.readTree(response.getScriptDetailsJson()));
        cleanResponse.put("expectedQuestions", objectMapper.readTree(response.getExpectedQuestionsJson()));

        return ApiResponse.success(cleanResponse);
    }
}