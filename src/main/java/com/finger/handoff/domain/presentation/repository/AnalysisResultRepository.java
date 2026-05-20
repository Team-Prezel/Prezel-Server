package com.finger.handoff.domain.presentation.repository;

import com.finger.handoff.domain.presentation.entity.AnalysisResult;
import com.finger.handoff.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    @Query("SELECT a FROM AnalysisResult a JOIN a.presentation p WHERE p.user = :user AND p.title = :title ORDER BY a.createdAt ASC")
    List<AnalysisResult> findHistoryByUserAndTitle(@Param("user") User user, @Param("title") String title);
}
