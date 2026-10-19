package com.finger.handoff.domain.terms.repository;

import com.finger.handoff.domain.terms.entity.Terms;
import com.finger.handoff.domain.terms.entity.UserTermsAgreement;
import com.finger.handoff.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {
    Optional<UserTermsAgreement> findByUserAndTerms(User user, Terms terms);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserTermsAgreement uta WHERE uta.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}