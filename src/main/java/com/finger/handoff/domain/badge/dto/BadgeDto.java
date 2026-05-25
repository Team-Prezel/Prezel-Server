package com.finger.handoff.domain.badge.dto;

import com.finger.handoff.domain.badge.entity.BadgeType;
import lombok.Builder;
import java.time.LocalDateTime;

public class BadgeDto {

    @Builder
    public record BadgeListResponse(
            String badgeName,
            String imageUrl,
            boolean isUnlocked,
            LocalDateTime unlockedAt
    ) {
        public static BadgeListResponse of(BadgeType type, boolean isUnlocked, LocalDateTime unlockedAt) {
            return BadgeListResponse.builder()
                    .badgeName(type.getBadgeName())
                    .imageUrl(type.getImageUrl())
                    .isUnlocked(isUnlocked)
                    .unlockedAt(unlockedAt)
                    .build();
        }
    }

    @Builder
    public record BadgeDetailResponse(
            String badgeName,
            String detailDescription,
            String conditionText,
            String imageUrl,
            boolean isUnlocked,
            LocalDateTime unlockedAt
    ) {
        public static BadgeDetailResponse of(BadgeType type, boolean isUnlocked, LocalDateTime unlockedAt) {
            return BadgeDetailResponse.builder()
                    .badgeName(type.getBadgeName())
                    .detailDescription(type.getDetailDescription())
                    .conditionText(type.getConditionText())
                    .imageUrl(type.getImageUrl())
                    .isUnlocked(isUnlocked)
                    .unlockedAt(unlockedAt)
                    .build();
        }
    }
}