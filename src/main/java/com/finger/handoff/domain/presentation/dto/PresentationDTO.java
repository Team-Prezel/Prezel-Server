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
    public static class GrowthData {
        private Integer attempt;
        private Double accuracyScore;
        private Double scriptMatchRate;
    }
}