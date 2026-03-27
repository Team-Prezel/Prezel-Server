package com.finger.handoff.domain.terms.repository;

import com.finger.handoff.domain.terms.entity.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {
}