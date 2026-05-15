package com.finger.handoff.domain.presentation.entity;

import com.finger.handoff.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Presentation {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    private LocalDate presentationDate;

    @Enumerated(EnumType.STRING)
    private PresentationType type;

    @Enumerated(EnumType.STRING)
    private PresentationPurpose purpose;

    @Enumerated(EnumType.STRING)
    private PresentationStyle style;

    @Enumerated(EnumType.STRING)
    private PresentationAudience audience;

    private String script;

    @OneToOne(mappedBy = "presentation", cascade = CascadeType.ALL, orphanRemoval = true)// 일단 일대일 연결(추구 변경될듯)
    private AnalysisResult analysisResult;


    @Builder
    public Presentation(User user, String title, LocalDate presentationDate,
                        PresentationType type, PresentationPurpose purpose,
                        PresentationStyle style, PresentationAudience audience, String script) {
        this.user = user;
        this.title = title;
        this.presentationDate = presentationDate;
        this.type = type;
        this.purpose = purpose;
        this.style = style;
        this.audience = audience;
        this.script = script;
    }

    public void setAnalysisResult(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }
}
