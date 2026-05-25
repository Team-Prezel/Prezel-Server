package com.finger.handoff.domain.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.finger.handoff.domain.presentation.entity.PresentationAudience;
import com.finger.handoff.domain.presentation.entity.PresentationPurpose;
import com.finger.handoff.domain.presentation.entity.PresentationStyle;
import com.finger.handoff.domain.presentation.entity.PresentationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public class PresentationDTO {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresentationRequest {
        private String name;

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private PresentationType type;
        private PresentationPurpose purpose;
        private PresentationStyle style;
        private PresentationAudience audience;
        private String script;
        private MultipartFile scriptFile;
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

        private LocalDate analysisDate;
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
        private List<ExpectedQuestionData> expectedQuestions;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExpectedQuestionData {
        private String question;
        private String answer;
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
        private String originalScript;
        private List<ScriptAnalysisDetail> scriptDetails;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptAnalysisDetail {
        private String errorType;     // "SPELLING" 또는 "GRAMMAR"
        private String sentence;      // 매핑을 위한 오류가 포함된 전체 문장
        private String originalText;  // 원본 텍스트
        private String correctedText; // 교정된 텍스트
        private String reason;        // 교정 이유
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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresentationListResponse {
        private Long presentationId;
        private String title;
        private LocalDate presentationDate;
        private String dDay;

        private PresentationType type;
        private PresentationPurpose purpose;
        private PresentationStyle style;
        private PresentationAudience audience;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingDetailResponse {
        private SummaryResponse analysisResult;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PastDetailResponse {
        private SummaryResponse analysisResult;
        private String reviewContent;
        private long practiceCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainScreenResponse {
        private Long presentationId;
        private PresentationType type;
        private LocalDate presentationDate;
        private String title;
        private int practiceCount;
        private String dDay;

        @Schema(description = "하루가 지난 발표(D+1)인지 여부")
        private Boolean isPast;

        private List<GrowthData> growthGraph;

        @Schema(description = "첫 녹음 대비 마지막 녹음 발화 정확도 변화율")
        private Integer accuracyScoreChange;

        @Schema(description = "첫 녹음 대비 마지막 녹음 대본 일치율 변화율")
        private Integer scriptMatchRateChange;
    }
}