package com.finger.handoff.domain.presentation.entity;

public enum PresentationType {
    EDUCATION("학술,교육"),
    WORK("업무,보고"),
    OFFER("설득,제안"),
    EVENT("행사,공개");

    private final String description;

    PresentationType(String description) {
        this.description = description;
    }
}
