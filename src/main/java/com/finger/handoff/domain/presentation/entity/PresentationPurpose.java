package com.finger.handoff.domain.presentation.entity;

public enum PresentationPurpose {
    INFO("정보 전달"),
    UNDERSTANDING("이해 증진"),
    EMPATHY("공감 형성");

    private final String description;

    PresentationPurpose(String description) {
        this.description = description;
    }
}
