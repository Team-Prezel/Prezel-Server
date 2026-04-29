package com.finger.handoff.domain.practice.dto;

import lombok.Builder;
import lombok.Getter;

public class PracticeDto {

    @Getter
    @Builder
    public static class SentenceResponse {
        private String sentence;
    }

    @Getter
    @Builder
    public static class AnalysisResponse {
        private Double accuracyScore;
        private String speedEvaluation;
        private String overallEvaluation;
    }
}
