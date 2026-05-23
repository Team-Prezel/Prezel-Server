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
    INVALID_ID_TOKEN(HttpStatus.NOT_FOUND, "T003", "idToken이 올바르지 않습니다."),

    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "TR001", "존재하지 않는 약관입니다."),
    REQUIRED_TERMS_DISAGREED(HttpStatus.BAD_REQUEST, "TR002", "필수 약관에는 반드시 동의해야 합니다."),

    FILE_IS_EMPTY(HttpStatus.NOT_FOUND, "F001", "파일이 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.FAILED_DEPENDENCY, "F002", "파일 업로드 중 오류가 발생했습니다."),
    FILE_CONVERT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "오디오 파일 변환 중 오류가 발생했습니다."),

    SCRIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "연습할 문장을 찾을 수 없습니다."),

    VOICE_RECOGNITION_FAILED(HttpStatus.BAD_REQUEST, "V001", "분석할 음성을 인식하지 못했어요."),
    VOICE_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "V002", "분석 중 문제가 발생했어요."),

    INVALID_SCRIPT_REQUEST(HttpStatus.BAD_REQUEST, "P001", "직접 입력한 대본과 대본 파일을 동시에 등록할 수 없습니다."),
    SCRIPT_FILE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P002", "대본 파일을 읽는 중 오류가 발생했습니다."),

    PRESENTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PR001", "존재하지 않는 발표입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "PR002", "해당 데이터에 접근할 권한이 없습니다."),

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "존재하지 않는 회고입니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "R002", "이미 회고를 작성한 발표입니다."),
    CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "R003", "셀프 피드백은 최대 200자까지 입력 가능합니다.");
    private final HttpStatus status;
    private final String code;
    private final String message;
}
