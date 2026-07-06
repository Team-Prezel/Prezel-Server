package com.finger.handoff.domain.curation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DDayRange {
    D_7_PLUS("D-7일 이상"),
    D_6_TO_3("D-6~D-3"),
    D_2_TO_1("D-2~D-1"),
    D_DAY("D-day");

    private final String description;
}