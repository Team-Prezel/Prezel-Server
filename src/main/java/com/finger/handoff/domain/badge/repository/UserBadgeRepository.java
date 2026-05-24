package com.finger.handoff.domain.badge.repository;

import com.finger.handoff.domain.badge.entity.BadgeType;
import com.finger.handoff.domain.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);
}