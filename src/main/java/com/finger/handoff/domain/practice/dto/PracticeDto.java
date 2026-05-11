package com.finger.handoff.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class PracticeDto {

    @Getter
    @Builder
    @Schema(description = "연습용 랜덤 문장 응답 DTO")
    public static class SentenceResponse {
        private String sentence;
    }

    @Getter
    @Builder
    @Schema(description = "연습녹음 분석 결과 응답 DTO")
    public static class AnalysisResponse {
        @Schema(description = "발화 % 점수 (0~100)", example = "85.5")
        private Double accuracyScore;

        @Schema(description = "발화 속도 평가", example = "적당해요", allowableValues = {"느려요", "적당해요", "빨라요"})
        private String speedEvaluation;

        @Schema(description = "종합 평가 등급", example = "Good", allowableValues = {"Perfect", "Good", "Try"})
        private String overallEvaluation;
    }
}
