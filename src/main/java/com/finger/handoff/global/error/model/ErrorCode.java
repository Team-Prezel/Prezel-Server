package com.finger.handoff.global.error.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류입니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "U002", "접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "존재하지 않는 유저입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U004", "이미 사용 중인 닉네임입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
