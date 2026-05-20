package com.finger.handoff.domain.presentation.entity;

public enum PresentationAudience {
    GENERAL("일반 청중"),
    PROFESSIONAL("전문가"),
    TEAMMATE("팀/동료");

    private final String description;

    PresentationAudience(String description) {
        this.description = description;
    }
}
