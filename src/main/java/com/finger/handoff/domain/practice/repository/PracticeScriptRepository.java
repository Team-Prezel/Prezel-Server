package com.finger.handoff.domain.practice.repository;

import com.finger.handoff.domain.practice.entity.PracticeScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PracticeScriptRepository extends JpaRepository<PracticeScript, Long> {

    @Query(value = "SELECT sentence FROM practice_script ORDER BY RAND() LIMIT 1", nativeQuery = true)
    String findRandomScript();
}
