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
    // TODO: 나중에 다른 도메인 개발 시 주석 해제하고 주입받으세요.
    // private final PracticeRepository practiceRepository;
    // private final ReviewRepository reviewRepository;

    @Async
    @EventListener
    @Transactional
    public void handleBadgeEvent(BadgeEvent event) {
        Long userId = event.getUserId();
        String action = event.getActionType();

        log.info("[뱃지 이벤트 수신] 유저ID: {}, 행동 유형: {}", userId, action);

        // 🌟 확장성 포인트: 행동 유형(Action)에 따라 검사할 뱃지들을 그루핑합니다.
        switch (action) {
            case "PRESENTATION_CREATED":
                checkStartBadge(userId);       // 1. 출발하기 (완성)
                // checkRepeat10Badge(userId); // 2. 반복하기 (추후 구현)
                break;

            case "ANALYZE_COMPLETED":
                // checkAnalyzeAgainBadge(userId); // 3. 기록쌓기 (추후 구현)
                break;

            case "PRACTICE_COMPLETED":
                // checkFirstPracticeBadge(userId); // 4. 연습하기 (추후 구현)
                // checkPerfectScoreBadge(userId);  // 5. 성장하기 (추후 구현)
                break;

            case "REVIEW_SAVED":
                // checkReviewBadge(userId); // 6. 돌아보기 (추후 구현)
                break;

            default:
                log.warn("정의되지 않은 뱃지 트리거 행동입니다: {}", action);
        }
    }

    private void checkStartBadge(Long userId) {
        // 이미 획득한 뱃지인지 Enum 타입을 조건으로 바로 조회 (DB 마스터 테이블 조인 없음)
        if (userBadgeRepository.existsByUserIdAndBadgeType(userId, BadgeType.START)) {
            return;
        }

        // 유저의 총 발표 등록 횟수 조회
        int presentationCount = presentationRepository.countByUserId(userId);

        if (presentationCount == 1) {
            grantBadgeAndSendSse(userId, BadgeType.START);
        }
    }

    /**
     * 🛠️ 공통 로직: 뱃지 저장 및 실시간 푸시 발송
     */
    private void grantBadgeAndSendSse(Long userId, BadgeType badgeType) {
        User user = userRepository.findById(userId).orElseThrow();

        // 1. user_badge 테이블에 Enum 값을 문자열로 저장
        userBadgeRepository.save(new UserBadge(user, badgeType));
        log.info("🎉 [뱃지 달성] 유저ID: {} 가 '{}' 뱃지를 획득했습니다!", userId, badgeType.getBadgeName());

        // 2. SSE 채널을 통해 실시간 알림 발송
        sendBadgeSse(userId, badgeType);
    }

    /**
     * 📡 실질적인 SSE 데이터 전송
     */
    private void sendBadgeSse(Long userId, BadgeType badgeType) {
        SseEmitter emitter = SseController.emitters.get(userId);
        if (emitter != null) {
            try {
                // Enum 내부에 선언된 메타데이터를 활용해 DTO 생성
                BadgeResponse response = new BadgeResponse(
                        badgeType.name(), // "START" (프론트가 식별자로 쓸 수 있게 코드값도 같이 전달)
                        badgeType.getBadgeName(),
                        badgeType.getIntroduction(),
                        badgeType.getImageUrl()
                );

                emitter.send(SseEmitter.event()
                        .name("badge_unlocked")
                        .data(response));
                log.info("📡 [SSE 알림 전송 성공] 유저ID: {} -> 뱃지: {}", userId, badgeType.getBadgeName());
            } catch (IOException e) {
                log.error("🚨 [SSE 알림 전송 실패] 유저ID: {}", userId, e);
                SseController.emitters.remove(userId);
            }
        }
    }
}