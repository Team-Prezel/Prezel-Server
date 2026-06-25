package com.finger.handoff.domain.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

public class PromptTestDTO {

    @Getter
    @Setter
    public static class SingleRequest {
        @Schema(description = "테스트에 사용할 기존 발표 ID", example = "1")
        private Long presentationId;

        @Schema(description = "테스트할 프롬프트 지시문", example = "이곳에 작성할 프롬프트를 입력하세요.")
        private String instruction;
    }
}