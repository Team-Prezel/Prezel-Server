package com.finger.handoff.domain.v2.async.repository;

import com.finger.handoff.domain.v2.async.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findAllByUserId(Long userId);
    Optional<FcmToken> findByToken(String token);
    boolean existsByToken(String token);
    void deleteByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FcmToken ft WHERE ft.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}