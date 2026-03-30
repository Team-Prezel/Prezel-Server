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
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U004", "이미 사용 중인 닉네임입니다."),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "T001", "유효하지 않거나 만료된 토큰입니다."),
    TOKEN_STOLEN(HttpStatus.UNAUTHORIZED, "T002", "토큰 탈취가 의심되어 강제 로그아웃 되었습니다."),

    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "TR001", "존재하지 않는 약관입니다."),
    REQUIRED_TERMS_DISAGREED(HttpStatus.BAD_REQUEST, "TR002", "필수 약관에는 반드시 동의해야 합니다."),
  
    FILE_IS_EMPTY(HttpStatus.NOT_FOUND, "F001", "파일이 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.FAILED_DEPENDENCY, "F002", "파일 업로드 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
