package com.finger.handoff.domain.presentation.repository;

import com.finger.handoff.domain.presentation.entity.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PresentationRepository extends JpaRepository<Presentation, Long> {
    int countByUserId(Long userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Presentation p " +
            "WHERE p.user.id = :userId AND SIZE(p.analysisResults) >= :count")
    boolean existsByUserIdAndAnalysisResultsCountGreaterThanEqual(@Param("userId") Long userId, @Param("count") long count);

    List<Presentation> findByUserIdAndPresentationDateGreaterThanEqualOrderByPresentationDateAsc(Long userId, LocalDate today);

    List<Presentation> findByUserIdAndPresentationDateLessThanOrderByPresentationDateDesc(Long userId, LocalDate today);

    List<Presentation> findTop3ByUserIdAndPresentationDateGreaterThanEqualOrderByPresentationDateAsc(Long userId, LocalDate cutoffDate);

    void deleteAllByUserId(Long userId);
}
