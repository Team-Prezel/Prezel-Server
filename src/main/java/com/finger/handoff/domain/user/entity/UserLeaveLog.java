package com.finger.handoff.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserLeaveLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private ReasonCategory reasonCategory;

    private String reasonText;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public UserLeaveLog(Long userId, ReasonCategory reasonCategory, String reasonText) {
        this.userId = userId;
        this.reasonCategory = reasonCategory;
        this.reasonText = reasonText;
    }
}