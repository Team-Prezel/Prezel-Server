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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final UserBadgeRepository userBadgeRepository;


    public List<BadgeDto.BadgeListResponse> getBadgeList(Long userId, String sort) {

        // 1. sort 파라미터에 따라 레포지토리 메서드를 다르게 호출 (획득한 뱃지만 조회)
        List<UserBadge> unlockedBadges = "acquired".equalsIgnoreCase(sort)
                ? userBadgeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : userBadgeRepository.findByUserId(userId);

        // 2. 획득한 뱃지들을 먼저 DTO로 변환하여 리스트에 담음 (DB 정렬 순서 유지)
        List<BadgeDto.BadgeListResponse> result = unlockedBadges.stream()
                .map(badge -> BadgeDto.BadgeListResponse.of(badge.getBadgeType(), true, badge.getCreatedAt()))
                .collect(Collectors.toList());

        // 3. 빠른 조회를 위해 획득한 뱃지 타입만 Set으로 추출
        Set<BadgeType> unlockedTypes = unlockedBadges.stream()
                .map(UserBadge::getBadgeType)
                .collect(Collectors.toSet());

        // 4. 미획득 뱃지들을 찾아서 결과 리스트 맨 뒤에 추가
        for (BadgeType type : BadgeType.values()) {
            if (!unlockedTypes.contains(type)) {
                result.add(BadgeDto.BadgeListResponse.of(type, false, null));
            }
        }

        return result;
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