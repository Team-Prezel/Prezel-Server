package com.finger.handoff.domain.user.dto;

import com.finger.handoff.domain.user.entity.ReasonCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWithdrawRequest {
    private ReasonCategory reasonCategory;
    private String reasonText;
}