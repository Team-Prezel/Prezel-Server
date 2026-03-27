package com.finger.handoff.domain.terms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermsAgreementRequest {

    @NotNull(message = "약관 ID는 필수입니다.")
    private Long termsId;

    @NotNull(message = "동의 여부는 필수입니다.")
    private Boolean isAgreed;
}