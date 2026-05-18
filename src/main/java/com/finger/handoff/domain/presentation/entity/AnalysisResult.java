package com.finger.handoff.domain.presentation.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presentation_id")
    private Presentation presentation;

    @Column(nullable = true)
    private Integer durationSeconds;

    private String speedEval;
    private Integer spm;

    @Column(nullable = true)
    private Double accuracyScore;

    @Column(nullable = true)
    private Double scriptMatchRate;

    @Column(columnDefinition = "TEXT")
    private String summaryFeedback;

    //성장 그래프 추가 (nullable = true)
    //대본 분석 추가 (nullable = true)
    //예상 질문 추가 (nullable = true)

    @Builder
    public AnalysisResult(Presentation presentation, int durationSeconds,
                          String speedEval, int spm,
                          double accuracyScore, double scriptMatchRate, String summaryFeedback) {
        this.presentation = presentation;
        this.durationSeconds = durationSeconds;
        this.speedEval = speedEval;
        this.spm = spm;
        this.accuracyScore = accuracyScore;
        this.scriptMatchRate = scriptMatchRate;
        this.summaryFeedback = summaryFeedback;
    }
}
