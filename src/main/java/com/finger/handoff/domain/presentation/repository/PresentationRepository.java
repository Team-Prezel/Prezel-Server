package com.finger.handoff.domain.presentation.repository;

import com.finger.handoff.domain.presentation.entity.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresentationRepository extends JpaRepository<Presentation, Long> {
}
