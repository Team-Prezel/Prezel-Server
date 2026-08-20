package com.finger.handoff.domain.presentation.test.dto;

import com.finger.handoff.domain.presentation.service.GeminiService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

public class PromptTestDTO {

    @Getter
    @Setter
    public static class SingleRequest {
        @Schema(description = "테스트에 사용할 기존 발표 ID", example = "1")
        private Long presentationId;

        @Schema(description = "테스트할 프롬프트 지시문", example = "이곳에 작성할 프롬프트를 입력하세요.")
        private String instruction;
    }

    @Getter
    @Setter
    public static class CombinationRequest {
        @Schema(description = "테스트에 사용할 기존 발표 데이터 ID", example = "1")
        private Long presentationId;

        @Schema(description = "테스트 유형 (SUMMARY: 요약, SCRIPT: 대본, QUESTION: 예상질문)", example = "SUMMARY")
        private GeminiService.PromptTestType testType;

        @Schema(description = "기본 프롬프트 (필수 뼈대)", example = "당신은 전문적인 코치입니다. 다음 기준에 따라 평가해주세요.")
        private String basePrompt;

        @Schema(description = "발표 유형 프롬프트 모듈", example = "청중이 새로운 개념을 쉽게 이해할 수 있도록 명확한 설명과 적절한 예시를 사용했는지 평가해 주세요.")
        private String typePrompt;

        @Schema(description = "발표 목적 프롬프트 모듈", example = "객관적인 정보가 논리적인 구조로 전달되었는지 확인해 주세요.")
        private String purposePrompt;

        @Schema(description = "발표 스타일 프롬프트 모듈", example = "격식을 갖춘 정중한 어조가 사용되었는지 검토해 주세요.")
        private String stylePrompt;

        @Schema(description = "청중 프롬프트 모듈", example = "일반 대중이 이해하기 어려운 전문 용어를 남용하지 않았는지 확인해 주세요.")
        private String audiencePrompt;
    }
}