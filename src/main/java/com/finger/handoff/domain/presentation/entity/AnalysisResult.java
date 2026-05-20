package com.finger.handoff.domain.presentation.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Column(length = 1000)
    private String audioUrl;

    @Column(columnDefinition = "LONGTEXT")
    private String wordDetailsJson;

    @Column(nullable = true)
    private Integer spellErrorCount;

    @Column(nullable = true)
    private Integer grammarErrorCount;

    @Column(columnDefinition = "LONGTEXT")
    private String scriptDetailsJson;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public AnalysisResult(Presentation presentation, Integer durationSeconds,
                          String speedEval, Integer spm, Double accuracyScore,
                          Double scriptMatchRate, String summaryFeedback,
                          String audioUrl, String wordDetailsJson,
                          Integer spellErrorCount, Integer grammarErrorCount, String scriptDetailsJson) { // 생성자 추가
        this.presentation = presentation;
        this.durationSeconds = durationSeconds;
        this.speedEval = speedEval;
        this.spm = spm;
        this.accuracyScore = accuracyScore;
        this.scriptMatchRate = scriptMatchRate;
        this.summaryFeedback = summaryFeedback;
        this.audioUrl = audioUrl;
        this.wordDetailsJson = wordDetailsJson;
        this.spellErrorCount = spellErrorCount;
        this.grammarErrorCount = grammarErrorCount;
        this.scriptDetailsJson = scriptDetailsJson;
    }
}