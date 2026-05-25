package com.finger.handoff.domain.badge.entity;

import com.finger.handoff.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeType badgeType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public UserBadge(User user, BadgeType badgeType) {
        this.user = user;
        this.badgeType = badgeType;
    }
}