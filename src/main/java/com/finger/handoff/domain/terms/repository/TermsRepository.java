package com.finger.handoff.domain.terms.repository;

import com.finger.handoff.domain.terms.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermsRepository extends JpaRepository<Terms, Long> {
    List<Terms> findByIsRequiredTrue();
}