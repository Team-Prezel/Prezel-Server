package com.finger.handoff.domain.practice.controller;

import com.finger.handoff.domain.practice.dto.PracticeDto;
import com.finger.handoff.domain.practice.service.PracticeService;
import com.finger.handoff.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Practice API", description = "연습녹음 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/recording/practice")
public class PracticeController {

    private final PracticeService practiceService;

    @Operation(
            summary = "랜덤 연습 문장 조회",
            description = "DB에 저장된 연습용 대본 중 하나를 무작위로 가져옵니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대본 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"code\": \"S001\",\n  \"data\": null,\n  \"message\": \"연습할 문장을 찾을 수 없습니다.\"\n}")
            ))
    })
    @GetMapping("/sentence")
    public ResponseEntity<ApiResponse<PracticeDto.SentenceResponse>> getRandomSentence() {
        String sentence = practiceService.getRandomSentence();

        PracticeDto.SentenceResponse response = PracticeDto.SentenceResponse.builder()
                .sentence(sentence)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "사용자 음성 발음 분석",
            description = "사용자의 음성 파일과 읽은 대본을 받아 발음 정확도와 속도를 분석합니다. 제한 용량은 10MB로 연습녹음엔 지장X."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "음성 인식 실패", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 400,\n  \"code\": \"V001\",\n  \"data\": null,\n  \"message\": \"분석할 음성을 인식하지 못했어요.\"\n}")
            )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "파일 없음", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"code\": \"F001\",\n  \"data\": null,\n  \"message\": \"파일이 없습니다.\"\n}")
            )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류 (음성 분석 실패)", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = "{\n  \"status\": 500,\n  \"code\": \"V002\",\n  \"data\": null,\n  \"message\": \"분석 중 문제가 발생했어요.\"\n}")
            ))
    })
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PracticeDto.AnalysisResponse> analyzeAudio(
            @Parameter(description = "분석할 사용자 음성 녹음 파일 (.wav, .m4a, .mp3 등 포맷 무관합니다.)")
            @RequestPart("audio") MultipartFile audio,
            @RequestParam("referenceText") String referenceText) {

        PracticeDto.AnalysisResponse response = practiceService.analyzePracticeVoice(audio, referenceText);
        return ApiResponse.success(response);
    }
}
