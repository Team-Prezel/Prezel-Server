package com.finger.handoff.domain.v2.async.service;

import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.v2.async.entity.AnalysisStatus;
import com.finger.handoff.domain.v2.async.entity.FcmToken;
import com.finger.handoff.domain.v2.async.repository.FcmTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public void saveToken(User user, String token) {
        if (!fcmTokenRepository.existsByToken(token)) {
            fcmTokenRepository.save(FcmToken.builder()
                    .user(user)
                    .token(token)
                    .build());
        }
    }

    @Transactional
    public void removeToken(String token) {
        fcmTokenRepository.deleteByToken(token);
    }

    public void sendAnalysisResultPush(Long userId, Long presentationId, Long analysisResultId, AnalysisStatus status) {
        List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) return;

        String title = status == AnalysisStatus.COMPLETED ? "분석 완료" : "분석 실패";
        String body = status == AnalysisStatus.COMPLETED ? "분석이 성공적으로 완료되었습니다." : "분석 중 오류가 발생했습니다.";

        for (FcmToken fcmToken : tokens) {
            String cardStatus = status == AnalysisStatus.COMPLETED ? "complete" : "fail";
            String errorType = status != AnalysisStatus.COMPLETED ? status.name() : "";

            if (FirebaseApp.getApps().isEmpty()) {
                log.info("[FCM Mock 발송] Firebase 미설정 상태 - 콘솔 로그 대체 (토큰: {}, presentationId: {}, analysisResultId: {}, cardStatus: {}, errorType: {})",
                        fcmToken.getToken(), presentationId, analysisResultId, cardStatus, errorType);
                continue;
            }

            try {
                Message message = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("presentationId", presentationId.toString())
                        .putData("analysisResultId", analysisResultId.toString())
                        .putData("status", status.name())
                        .putData("cardStatus", cardStatus)
                        .putData("errorType", errorType)
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.info("FCM 발송 성공 - 토큰: {}", fcmToken.getToken());
            } catch (Exception e) {
                log.warn("FCM 발송 실패 - 토큰: {}", fcmToken.getToken(), e);
                fcmTokenRepository.delete(fcmToken);
            }
        }
    }
}