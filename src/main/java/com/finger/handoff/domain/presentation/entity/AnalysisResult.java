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
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Presentation presentation;

    @Column(nullable = true)
    private Integer durationSeconds; //초가 아니라 시분초로 바꾸기

    private String speedEval;
    private Integer spm;

    @Column(nullable = true)
    private Double accuracyScore;

    @Column(nullable = true)
    private Double scriptMatchRate;

    //요약 피드백 추가
    //성장 그래프 추가 (nullable = true)
    //대본 분석 추가 (nullable = true)
    //예상 질문 추가 (nullable = true)

    @Builder
    public AnalysisResult(Presentation presentation, int durationSeconds,
                          String speedEval, int spm,
                          double accuracyScore, double scriptMatchRate) {
        this.presentation = presentation;
        this.durationSeconds = durationSeconds;
        this.speedEval = speedEval;
        this.spm = spm;
        this.accuracyScore = accuracyScore;
        this.scriptMatchRate = scriptMatchRate;
    }
}
