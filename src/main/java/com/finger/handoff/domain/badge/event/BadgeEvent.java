package com.finger.handoff.domain.badge.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BadgeEvent {
    private Long userId;
    private String actionType; // 행동 종류 (예: "PRESENTATION_CREATED")
}