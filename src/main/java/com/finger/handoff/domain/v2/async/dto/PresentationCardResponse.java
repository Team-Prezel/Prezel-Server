package com.finger.handoff.domain.v2.async.dto;

import com.finger.handoff.domain.presentation.entity.PresentationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationCardResponse {
    private Long presentationId;

    private Long analysisResultId;

    private String title;

    @Schema(description = "발표 예정일")
    private LocalDate presentationDate;

    @com.fasterxml.jackson.annotation.JsonProperty("dDay")
    @Schema(description = "D-Day")
    private String dDay;

    @com.fasterxml.jackson.annotation.JsonProperty("dDay")
    public String getDDay() {
        return dDay;
    }

    @Schema(description = "발표 유형 (학술-교육, 비즈니스 등)")
    private PresentationType type;

    @Schema(description = "상태 (inProcess: 분석중, complete: 분석완료(미열람), default: 기본(열람완료), fail: 분석실패)")
    private String cardStatus;

    @Schema(description = "실패 시 에러 유형 (ERR_VOICE_RECOG(음성 인식 에러), ERR_FILE_RECOG(음성 파일 에러), ERR_ANALYSIS(분석 중 문제 발생), 성공 시 null")
    private String errorType;

    @Schema(description = "실패 시 재녹음/대본 유지용 대본 텍스트")
    private String script;

    @Schema(description = "ERR_ANALYSIS(분석 중 문제 발생) 시 음성 재사용용 URL, 그 외 null")
    private String audioUrl;
}
