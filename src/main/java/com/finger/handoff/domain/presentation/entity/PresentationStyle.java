package com.finger.handoff.domain.presentation.entity;

public enum PresentationStyle {
    FORMAL("전문적인"),
    FRIENDLY("친근한"),
    CALM("차분한"),
    CASUAL("편안한");

    private final String description;

    PresentationStyle(String description) {
        this.description = description;
    }
}
