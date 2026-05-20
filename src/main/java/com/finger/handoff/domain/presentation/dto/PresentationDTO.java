package com.finger.handoff.domain.presentation.dto;

import com.finger.handoff.domain.presentation.entity.PresentationAudience;
import com.finger.handoff.domain.presentation.entity.PresentationPurpose;
import com.finger.handoff.domain.presentation.entity.PresentationStyle;
import com.finger.handoff.domain.presentation.entity.PresentationType;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public class PresentationDTO {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresentationRequest {
        private String name;
        private LocalDateTime date;
        private PresentationType type;
        private PresentationPurpose purpose;
        private PresentationStyle style;
        private PresentationAudience audience;
        private String script;
        private MultipartFile audio;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private Long presentationId;
        private Long analysisResultId;
        private String name;
        private PresentationType type;
        private PresentationPurpose purpose;
        private PresentationStyle style;
        private PresentationAudience audience;

        private LocalDateTime analysisDate;
        private Integer durationSeconds;
        private String formattedDuration;
        private Integer spm;
        private String speedEval;
        private String summaryFeedback;

        private Double accuracyScore;
        private Double scriptMatchRate;

        private Integer spellErrorCount;
        private Integer grammarErrorCount;
        private Integer totalErrorCount;

        private List<GrowthData> growthGraph;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordDetailResponse {
        private Long presentationId;
        private String audioUrl;
        private List<WordAnalysisDetail> wordDetails;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordAnalysisDetail {
        private String word;
        private String status;
        private String description;
        private Double accuracy;
        private Long startTimeMs;
        private Long endTimeMs;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptDetailResponse {
        private Long presentationId;
        private String audioUrl;
        private List<ScriptAnalysisDetail> scriptDetails;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptAnalysisDetail {
        private String errorType; // "SPELLING" (맞춤법) 또는 "GRAMMAR" (주술호응)
        private String originalText; // 원본 텍스트
        private String correctedText; // 교정된 텍스트
        private String reason; // 교정 이유
        private Long startTimeMs; // 시작 시간
        private Long endTimeMs; // 종료 시간
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthData {
        private Integer attempt;
        private Double accuracyScore;
        private Double scriptMatchRate;
    }
}