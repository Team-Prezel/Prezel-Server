package com.finger.handoff.domain.badge.service;

import com.finger.handoff.domain.badge.controller.SseController;
import com.finger.handoff.domain.badge.dto.BadgeResponse;
import com.finger.handoff.domain.badge.entity.BadgeType;
import com.finger.handoff.domain.badge.entity.UserBadge;
import com.finger.handoff.domain.badge.event.BadgeEvent;
import com.finger.handoff.domain.badge.repository.UserBadgeRepository;
import com.finger.handoff.domain.presentation.repository.PresentationRepository;
import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEventListener {

    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PresentationRepository presentationRepository;
    // private final PracticeRepository practiceRepository;
    // private final ReviewRepository reviewRepository;

    @Async
    @EventListener
    @Transactional
    public void handleBadgeEvent(BadgeEvent event) {
        Long userId = event.getUserId();
        String action = event.getActionType();

        switch (action) {
            case "PRESENTATION_CREATED":
                checkStartBadge(userId);
                 checkRepeat10Badge(userId);
                break;

            case "ANALYZE_COMPLETED":
                 checkAnalyzeAgainBadge(userId); // 3. 기록쌓기 (추후 구현)
                break;

            case "PRACTICE_COMPLETED":
                // checkFirstPracticeBadge(userId); // 4. 연습하기 (추후 구현)
                // checkPerfectScoreBadge(userId);  // 5. 성장하기 (추후 구현)
                break;

            case "REVIEW_SAVED":
                // checkReviewBadge(userId); // 6. 돌아보기 (추후 구현)
                break;

            default:
        }
    }

    private void checkStartBadge(Long userId) {
        if (userBadgeRepository.existsByUserIdAndBadgeType(userId, BadgeType.START)) {
            return;
        }

        int presentationCount = presentationRepository.countByUserId(userId);

        if (presentationCount == 1) {
            grantBadgeAndSendSse(userId, BadgeType.START);
        }
    }

    private void checkRepeat10Badge(Long userId) {
        if (userBadgeRepository.existsByUserIdAndBadgeType(userId, BadgeType.REPEAT_10)) {
            return;
        }

        int presentationCount = presentationRepository.countByUserId(userId);

        if (presentationCount == 10) {
            grantBadgeAndSendSse(userId, BadgeType.REPEAT_10);
        }
    }

    private void grantBadgeAndSendSse(Long userId, BadgeType badgeType) {
        User user = userRepository.findById(userId).orElseThrow();

        userBadgeRepository.save(new UserBadge(user, badgeType));

        sendBadgeSse(userId, badgeType);
    }

    private void checkAnalyzeAgainBadge(Long userId) {
        if (userBadgeRepository.existsByUserIdAndBadgeType(userId, BadgeType.ANALYZE_AGAIN)) {
            return;
        }

        boolean hasAnalyzedAgain = presentationRepository.existsByUserIdAndAnalysisResultsCountGreaterThanEqual(userId, 2L);

        if (hasAnalyzedAgain) {
            grantBadgeAndSendSse(userId, BadgeType.ANALYZE_AGAIN);
        }
    }

    private void sendBadgeSse(Long userId, BadgeType badgeType) {
        SseEmitter emitter = SseController.emitters.get(userId);
        if (emitter != null) {
            try {
                // Enum 내부에 선언된 메타데이터를 활용해 DTO 생성
                BadgeResponse response = new BadgeResponse(
                        badgeType.name(),
                        badgeType.getBadgeName(),
                        badgeType.getIntroduction(),
                        badgeType.getImageUrl()
                );

                emitter.send(SseEmitter.event()
                        .name("badge_unlocked")
                        .data(response));
            } catch (IOException e) {
                SseController.emitters.remove(userId);
            }
        }
    }
}