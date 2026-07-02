package com.finger.handoff.domain.review.repository;

import com.finger.handoff.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndUserId(Long presentationId, Long userId);

    boolean existsById(Long presentationId);

    void deleteAllByUserId(Long userId);

    Optional<Review> findByPresentationId(Long presentationId);
}