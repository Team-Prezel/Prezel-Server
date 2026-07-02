package com.finger.handoff.domain.curation.repository;

import com.finger.handoff.domain.curation.entity.CurationData;
import com.finger.handoff.domain.curation.entity.DDayRange;
import com.finger.handoff.domain.curation.entity.PresentationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CurationRepository extends JpaRepository<CurationData, Long> {

    @Query("SELECT c FROM CurationData c " +
            "WHERE c.presentationType = :presentationType " +
            "AND c.dDayRange = :dDayRange " +
            "ORDER BY c.recommendOrder ASC")
    List<CurationData> findByPresentationTypeAndDDayRangeOrderByRecommendOrderAsc(
            @Param("presentationType") PresentationType presentationType,
            @Param("dDayRange") DDayRange dDayRange
    );
}