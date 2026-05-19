package com.finger.handoff.domain.presentation.repository;

import com.finger.handoff.domain.presentation.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
}
