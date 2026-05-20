package com.finger.handoff.domain.presentation.entity;

import com.finger.handoff.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Presentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(columnDefinition = "TEXT")
    private String script;

    @OneToMany(mappedBy = "presentation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisResult> analysisResults = new ArrayList<>();

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
}