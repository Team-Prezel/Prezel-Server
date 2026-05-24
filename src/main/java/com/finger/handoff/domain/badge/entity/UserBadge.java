package com.finger.handoff.domain.badge.entity;

import com.finger.handoff.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_badge")
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🌟 뱃지 테이블 대신 Enum을 직접 저장! (EnumType.STRING 필수)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeType badgeType;

    public UserBadge(User user, BadgeType badgeType) {
        this.user = user;
        this.badgeType = badgeType;
    }
}