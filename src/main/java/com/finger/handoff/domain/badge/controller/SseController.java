package com.finger.handoff.domain.badge.controller;

import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
            description = "클라이언트가 서버로부터 실시간으로 뱃지 획득 알림(SSE)을 받기 위해 연결을 맺습니다. 연결 성공 시 최초 'connect' 이벤트가 전송되며, 이후 뱃지 조건 달성 시 'badge_unlocked' 이벤트가 푸시됩니다."
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
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }
}