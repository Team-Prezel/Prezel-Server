package com.finger.handoff.domain.curation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PresentationType {
    EDUCATION("학술,교육"), // ACADEMIC -> EDUCATION 으로 변경
    WORK("업무,보고"),     // BUSINESS -> WORK 로 변경
    OFFER("설득,제안"),    // PERSUASION -> OFFER 로 변경
    EVENT("행사,공개");

    private final String description;
}