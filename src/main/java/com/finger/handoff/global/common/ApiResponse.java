package com.finger.handoff.global.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private final int status;
    private final String code;
    private final T data;
    private final String message;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().status(200).data(data).build();
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder().status(200).build();
    }

    public static <T> ApiResponse<T> error(int status, String code, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .code(code)
                .message(message)
                .build();
    }

}
