package com.finger.handoff.domain.badge.dto;

public record BadgeResponse(
        String badgeCode,
        String badgeName,
        String badgeIntroduction,
        String badgeImageUrl
) {
}