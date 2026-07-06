package com.finger.handoff.domain.badge.repository;

import com.finger.handoff.domain.badge.entity.BadgeType;
import com.finger.handoff.domain.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);

    List<UserBadge> findByUserId(Long userId);

    Optional<UserBadge> findByUserIdAndBadgeType(Long userId, BadgeType badgeType);

    void deleteAllByUserId(Long userId);

    List<UserBadge> findByUserIdOrderByCreatedAtDesc(Long userId);
}