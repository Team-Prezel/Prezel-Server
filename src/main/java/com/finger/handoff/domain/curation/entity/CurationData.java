package com.finger.handoff.domain.curation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "curation")
public class CurationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private PresentationType presentationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "d_day_type")
    private DDayRange dDayRange;

    @Column(nullable = false)
    private String guideMessage;

    @Column(nullable = false)
    private Integer recommendOrder;

    @Column(nullable = false)
    private String materialType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String sourceChannel;

    @Column(length = 500, nullable = false)
    private String linkUrl;

    @Column(length = 500, nullable = false)
    private String imageUrl;
}