package com.finger.handoff.domain.v2.async.repository;

import com.finger.handoff.domain.v2.async.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findAllByUserId(Long userId);
    boolean existsByToken(String token);
    void deleteByToken(String token);
}