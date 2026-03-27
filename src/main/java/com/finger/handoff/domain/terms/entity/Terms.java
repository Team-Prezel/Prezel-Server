package com.finger.handoff.domain.terms.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Terms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Boolean isRequired;

    @Column(nullable = false)
    private String version;

    @Builder
    public Terms(String title, String content, Boolean isRequired, String version) {
        this.title = title;
        this.content = content;
        this.isRequired = isRequired;
        this.version = version;
    }
}