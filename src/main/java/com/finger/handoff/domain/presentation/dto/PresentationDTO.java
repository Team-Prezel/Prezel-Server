package com.finger.handoff.domain.presentation.dto;

import com.finger.handoff.domain.presentation.entity.PresentationAudience;
import com.finger.handoff.domain.presentation.entity.PresentationPurpose;
import com.finger.handoff.domain.presentation.entity.PresentationStyle;
import com.finger.handoff.domain.presentation.entity.PresentationType;
import lombok.*;
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
        private LocalDate date;
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
    public static class AnalysisResponse {
        // 기본 정보 (대본 유무 무관하게 공통)
        private String name;
        private PresentationType type;
        private PresentationPurpose purpose;
        private PresentationStyle style;
        private PresentationAudience audience;

        // 공통 분석 지표
        private Integer durationSeconds;
        private Integer spm;
        private String speedEval;
        private String summaryFeedback;

        // 대본이 있는 경우에만 반환되는 지표 (대본이 없으면 null)
        private Double accuracyScore;
        private Double scriptMatchRate;
        private List<WordAnalysisDetail> wordDetails;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordAnalysisDetail {
        private String word;

        // 상태 코드: Stutter, Insertion, Omission, Mispronunciation, Excellent, Good
        private String status;

        // AzureSpeechService2에서 사용했던 상세 설명
        private String description;

        // 단어별 정확도 점수
        private Double accuracy;
    }
}