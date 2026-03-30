package com.finger.handoff.domain.user.dto;

import com.finger.handoff.domain.user.entity.ReasonCategory;
import lombok.Getter;

@Getter
public class UserWithdrawRequest {
    private ReasonCategory reasonCategory;
    private String reasonText;
}