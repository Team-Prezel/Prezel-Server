package com.finger.handoff.domain.user.repository;

import com.finger.handoff.domain.user.entity.UserLeaveLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLeaveLogRepository extends JpaRepository<UserLeaveLog, Long> {
}