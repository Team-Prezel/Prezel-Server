package com.finger.handoff.domain.v2.async.service;

import com.finger.handoff.domain.user.entity.User;
import com.finger.handoff.domain.v2.async.entity.AnalysisStatus;
import com.finger.handoff.domain.v2.async.entity.FcmToken;
import com.finger.handoff.domain.v2.async.repository.FcmTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
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
        fcmTokenRepository.findByToken(token).ifPresentOrElse(
                fcmToken -> {
                    // 동일한 디바이스 토큰인데 소유자가 변경된 경우 (예: 동일 기기에서 다른 계정으로 로그인)
                    if (!fcmToken.getUser().getId().equals(user.getId())) {
                        log.info("FCM 기기 토큰 소유자 변경: 이전 userId={} -> 새 userId={} (token={})",
                                fcmToken.getUser().getId(), user.getId(), token);
                        fcmToken.updateUser(user);
                    } else {
                        log.info("기존 FCM 기기 토큰 유지: userId={}, token={}", user.getId(), token);
                    }
                },
                () -> {
                    fcmTokenRepository.save(FcmToken.builder()
                            .user(user)
                            .token(token)
                            .build());
                    log.info("새 FCM 기기 토큰 등록 성공: userId={}, token={}", user.getId(), token);
                }
        );
    }

    @Transactional
    public void removeToken(String token) {
        fcmTokenRepository.deleteByToken(token);
    }

    public void sendAnalysisResultPush(Long userId, Long presentationId, Long analysisResultId, AnalysisStatus status) {
        sendAnalysisResultPush(userId, presentationId, analysisResultId, status, null, null);
    }

    public void sendAnalysisResultPush(Long userId, Long presentationId, Long analysisResultId, AnalysisStatus status, String script, String audioUrl) {
        List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            log.warn("FCM 발송 건너뜀: 해당 유저에게 등록된 FCM 기기 토큰이 없습니다. (userId: {}, presentationId: {}, analysisResultId: {})",
                    userId, presentationId, analysisResultId);
            return;
        }

        log.info("FCM 발송 시도 - 대상 userId: {}, 등록된 기기 토큰 수: {}", userId, tokens.size());

        String title = status == AnalysisStatus.COMPLETED ? "분석 완료" : "분석 실패";
        String body = switch (status) {
            case COMPLETED -> "분석이 성공적으로 완료되었습니다.";
            case ERR_VOICE_RECOG -> "분석할 음성을 인식하지 못했어요. 조용한 환경에서 다시 녹음해 주세요.";
            case ERR_FILE_RECOG -> "음성 파일을 찾지 못했어요. 다른 음성 파일로 다시 시도해 주세요.";
            case ERR_ANALYSIS -> "분석 중 문제가 발생했어요. 일시적인 오류로 분석을 완료하지 못했어요. 다시 시도해 주세요.";
            default -> "분석 중 오류가 발생했습니다.";
        };

        String cardStatus = status == AnalysisStatus.COMPLETED ? "complete" : "fail";
        String errorType = status != AnalysisStatus.COMPLETED ? status.name() : "";
        String finalScript = script != null ? script : "";
        if (finalScript.length() > 3000) {
            finalScript = finalScript.substring(0, 3000);
        }
        String finalAudioUrl = (status == AnalysisStatus.ERR_ANALYSIS && audioUrl != null) ? audioUrl : "";

        for (FcmToken fcmToken : tokens) {
            if (FirebaseApp.getApps().isEmpty()) {
                log.warn("[FCM Mock 모드] Firebase가 초기화되지 않았습니다(서버에 firebase-adminsdk.json 미등록). 실제 전송 대신 로그로 기록합니다. (userId: {}, 토큰: {}, presentationId: {}, analysisResultId: {})",
                        userId, fcmToken.getToken(), presentationId, analysisResultId);
                continue;
            }

            try {
                Message message = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", "ANALYSIS_RESULT")
                        .putData("presentationId", presentationId.toString())
                        .putData("analysisResultId", analysisResultId.toString())
                        .putData("status", status.name())
                        .putData("cardStatus", cardStatus)
                        .putData("errorType", errorType)
                        .putData("script", finalScript)
                        .putData("audioUrl", finalAudioUrl)
                        .build();

                String messageId = FirebaseMessaging.getInstance().send(message);
                log.info("FCM 푸시 발송 성공 - messageId: {}, userId: {}, 토큰: {}", messageId, userId, fcmToken.getToken());
            } catch (FirebaseMessagingException fe) {
                log.error("FCM 발송 실패 (FirebaseMessagingException) - userId: {}, 토큰: {}, errorCode: {}, message: {}",
                        userId, fcmToken.getToken(), fe.getMessagingErrorCode(), fe.getMessage(), fe);

                // 유효하지 않거나 앱 삭제 등으로 등록 취소된 토큰인 경우에만 DB에서 제거
                if (fe.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || fe.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.info("만료/무효화된 FCM 토큰 DB 삭제: userId={}, token={}", userId, fcmToken.getToken());
                    fcmTokenRepository.delete(fcmToken);
                }
            } catch (Exception e) {
                log.error("FCM 발송 중 예상치 못한 오류 발생 - userId: {}, 토큰: {}", userId, fcmToken.getToken(), e);
            }
        }
    }
}