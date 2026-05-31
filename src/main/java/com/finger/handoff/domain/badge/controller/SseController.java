package com.finger.handoff.domain.badge.controller;

import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "SSE Stream", description = "실시간 알림(SSE) API")
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class SseController {

    public static final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Operation(
            summary = "실시간 뱃지 획득 알림 구독",
            description = "클라이언트가 서버로부터 실시간으로 뱃지 획득 알림(SSE)을 받기 위해 연결을 맺습니다.<br><br>" +
                    "### 📡 프론트엔드 수신 이벤트(Event) 메시지 형식<br>" +
                    "스트림을 통해 전달되는 이벤트의 이름(`name`)과 데이터(`data`) 구조는 총 3가지입니다.<br><br>" +
                    "**1. 연결 성공 (최초 1회)**<br>" +
                    "- **이벤트명 (`name`)**: `connect`<br>" +
                    "- **데이터 (`data`)**: `connected!` (순수 텍스트)<br><br>" +
                    "**2. 연결 유지용 Ping (약 45초 주기)**<br>" +
                    "- **이벤트명 (`name`)**: `ping`<br>" +
                    "- **데이터 (`data`)**: `keep-alive` (순수 텍스트)<br>" +
                    "- *참고: 브라우저 타임아웃을 막기 위한 빈 패킷이므로 UI 처리는 무시하시면 됩니다.*<br><br>" +
                    "**3. 뱃지 획득 (조건 달성 시 즉시 푸시)**<br>" +
                    "- **이벤트명 (`name`)**: `badge_unlocked`<br>" +
                    "- **데이터 (`data`)**: 뱃지 정보 JSON 객체 (`BadgeResponse`)<br>" +
                    "- **JSON Payload 예시**:<br>" +
                    "```json\n" +
                    "{\n" +
                    "  \"badgeCode\": \"START\",\n" +
                    "  \"badgeName\": \"출발하기\",\n" +
                    "  \"introduction\": \"첫 발표를 등록했어요\",\n" +
                    "  \"imageUrl\": \"[https://s3.ap-northeast-2.amazonaws.com/.../start.png](https://s3.ap-northeast-2.amazonaws.com/.../start.png)\"\n" +
                    "}\n" +
                    "```"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 연결 성공 (응답 타입: text/event-stream)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 권한 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "U001", description = "유효하지 않은 토큰", value = "{\"status\": 401, \"code\": \"U001\", \"message\": \"인증이 필요합니다.\"}")))
    })
    @GetMapping(value = "/badges", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        // 타임아웃 1시간(3600000ms)짜리 연결 통로 생성
        SseEmitter emitter = new SseEmitter(3600000L);

        // 메모리에 저장
        emitters.put(userId, emitter);

        // 연결이 끊기거나 타임아웃 나면 메모리에서 삭제하도록 콜백 설정
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        try {
            // 1. 최초 연결 성공 시 connect 이벤트 발송
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    // 2. 프론트 연결 유지를 위한 주기적 Ping 이벤트
    @Scheduled(fixedRate = 45000)
    public void sendPing() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("keep-alive"));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        });
    }
}