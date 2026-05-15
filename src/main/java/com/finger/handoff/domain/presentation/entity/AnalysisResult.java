package com.finger.handoff.domain.presentation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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

    private int durationSeconds; //초가 아니라 시분초로 바꾸기

    private String speedEval;
    private double spm;

    private double accuracyScore;
    private double scriptMatchRate;

    //요약 피드백 추가
    //성장 그래프 추가
    //대본 분석 추가
    //예상 질문 추가

    @Builder
    public AnalysisResult(Presentation presentation, int durationSeconds,
                          String speedEval, double spm,
                          double accuracyScore, double scriptMatchRate) {
        this.presentation = presentation;
        this.durationSeconds = durationSeconds;
        this.speedEval = speedEval;
        this.spm = spm;
        this.accuracyScore = accuracyScore;
        this.scriptMatchRate = scriptMatchRate;
    }
}
