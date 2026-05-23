package com.finger.handoff.domain.review.service;

import com.finger.handoff.domain.badge.event.BadgeEvent;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.review.entity.Review;
import com.finger.handoff.domain.review.dto.ReviewDto;
import com.finger.handoff.domain.review.repository.ReviewRepository;
import com.finger.handoff.global.error.exception.BusinessException;
import com.finger.handoff.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PresentationRepository presentationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewDto.Response saveReview(Long presentationId, Long userId, ReviewDto.Request request) {

        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESENTATION_NOT_FOUND));

        if (!presentation.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (reviewRepository.existsById(presentationId)) {
            throw new BusinessException(ErrorCode.ALREADY_REVIEWED);
        }

        if (request.content().length() > 200) {
            throw new BusinessException(ErrorCode.CONTENT_TOO_LONG);
        }

        Review review = Review.builder()
                .presentation(presentation)
                .userId(userId)
                .content(request.content())
                .build();

        reviewRepository.save(review);

        eventPublisher.publishEvent(new BadgeEvent(userId, "REVIEW_SAVED"));

        return ReviewDto.Response.from(review);
    }

    public ReviewDto.Response getReview(Long presentationId, Long userId) {
        Review review = reviewRepository.findByIdAndUserId(presentationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewDto.Response.from(review);
    }
}