package com.finger.handoff.domain.badge.service;

import com.finger.handoff.domain.badge.dto.BadgeDto;
import com.finger.handoff.domain.badge.entity.BadgeType;
import com.finger.handoff.domain.badge.entity.UserBadge;
import com.finger.handoff.domain.badge.repository.UserBadgeRepository;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final UserBadgeRepository userBadgeRepository;

    /**
     * 🏅 1. 유저의 전체 뱃지 목록 조회 (해금 여부 포함)
     */
    public List<BadgeDto.BadgeListResponse> getBadgeList(Long userId) {
        // 1-1. 유저가 획득한 뱃지 리스트를 DB에서 조회
        List<UserBadge> myUnlockedBadges = userBadgeRepository.findByUserId(userId);

        // 1-2. O(1) 조회를 위해 Map 형태로 가공 (Key: BadgeType, Value: UserBadge)
        Map<BadgeType, UserBadge> badgeMap = myUnlockedBadges.stream()
                .collect(Collectors.toMap(UserBadge::getBadgeType, userBadge -> userBadge));

        // 1-3. 전체 뱃지 스펙(Enum)을 돌면서 획득 여부를 판별하여 DTO 조립
        return Arrays.stream(BadgeType.values())
                .map(type -> {
                    boolean isUnlocked = badgeMap.containsKey(type);
                    // UserBadge 엔티티에 생성일자(createdAt)가 정의되어 있다고 가정합니다.
                    // 만약 없다면 null이나 별도 일자를 대입할 수 있습니다.
                    java.time.LocalDateTime unlockedAt = isUnlocked ? badgeMap.get(type).getCreatedAt() : null;

                    return BadgeDto.BadgeListResponse.of(type, isUnlocked, unlockedAt);
                })
                .collect(Collectors.toList());
    }

    /**
     * 🔍 2. 특정 뱃지 단건 상세 조회
     */
    public BadgeDto.BadgeDetailResponse getBadgeDetail(Long userId, String badgeCode) {
        // 2-1. 문자열로 들어온 코드(예: "START")를 Enum 객체로 변환 (잘못된 코드 핸들링)
        BadgeType type;
        try {
            type = BadgeType.valueOf(badgeCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BADGE_NOT_FOUND);
        }

        // 2-2. 해당 유저가 이 뱃지를 가지고 있는지 확인
        Optional<UserBadge> userBadgeOpt = userBadgeRepository.findByUserIdAndBadgeType(userId, type);

        boolean isUnlocked = userBadgeOpt.isPresent();
        java.time.LocalDateTime unlockedAt = isUnlocked ? userBadgeOpt.get().getCreatedAt() : null;

        return BadgeDto.BadgeDetailResponse.of(type, isUnlocked, unlockedAt);
    }
}