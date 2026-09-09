package com.finger.handoff.domain.presentation.entity;

import com.finger.handoff.domain.v2.async.entity.AnalysisStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

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

    @Column(columnDefinition = "LONGTEXT")
    private String expectedQuestionsJson;

    @Column(nullable = false)
    private Boolean isViewed = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.isViewed == null) {
            this.isViewed = false;
        }
        if (this.status == null) {
            this.status = AnalysisStatus.COMPLETED;
        }
    }

    public void markAsViewed() {
        this.isViewed = true;
    }

    public void updateScriptDetails(String scriptDetailsJson, Integer spellErrorCount, Integer grammarErrorCount) {
        this.scriptDetailsJson = scriptDetailsJson;
        this.spellErrorCount = spellErrorCount;
        this.grammarErrorCount = grammarErrorCount;
    }

    @Builder
    public AnalysisResult(Presentation presentation, AnalysisStatus status, Integer durationSeconds,
                          String speedEval, Integer spm, Double accuracyScore,
                          Double scriptMatchRate, String summaryFeedback,
                          String audioUrl, String wordDetailsJson,
                          Integer spellErrorCount, Integer grammarErrorCount,
                          String scriptDetailsJson, String expectedQuestionsJson, Boolean isViewed) {
        this.presentation = presentation;
        this.status = status != null ? status : AnalysisStatus.COMPLETED;
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
        this.expectedQuestionsJson = expectedQuestionsJson;
        this.isViewed = isViewed != null ? isViewed : false;
    }

    public void updateAnalysisData(AnalysisStatus status, Integer durationSeconds, Integer spm, String speedEval,
                                   Double accuracyScore, Double scriptMatchRate, String summaryFeedback,
                                   String wordDetailsJson, Integer spellErrorCount, Integer grammarErrorCount,
                                   String scriptDetailsJson, String expectedQuestionsJson) {
        this.status = status;
        this.durationSeconds = durationSeconds;
        this.spm = spm;
        this.speedEval = speedEval;
        this.accuracyScore = accuracyScore;
        this.scriptMatchRate = scriptMatchRate;
        this.summaryFeedback = summaryFeedback;
        this.wordDetailsJson = wordDetailsJson;
        this.spellErrorCount = spellErrorCount;
        this.grammarErrorCount = grammarErrorCount;
        this.scriptDetailsJson = scriptDetailsJson;
        this.expectedQuestionsJson = expectedQuestionsJson;
    }

    public void updateStatus(AnalysisStatus status) {
        this.status = status;
    }

    public void updateAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}