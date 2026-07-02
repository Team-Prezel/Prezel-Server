package com.finger.handoff.domain.curation.controller;

import com.finger.handoff.domain.curation.dto.CurationResponse;
import com.finger.handoff.domain.curation.service.CurationService;
import com.finger.handoff.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Curation API", description = "발표 맞춤형 참고자료 큐레이션 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/presentations")
public class CurationController {

    private final CurationService curationService;

    @Operation(
            summary = "발표 D-Day 맞춤형 큐레이션 조회",
            description = "발표 ID를 기반으로 남은 일자를 자동 계산하여 맞춤형 참고 자료를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "에러 발생 시",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "P003",
                                            summary = "발표 데이터 없음",
                                            value = "{\"status\": 404, \"code\": \"P003\", \"data\": null, \"message\": \"존재하지 않는 발표입니다.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "A002",
                                            summary = "큐레이션 자료 없음",
                                            value = "{\"status\": 404, \"code\": \"C001\", \"data\": null, \"message\": \"해당 조건에 맞는 큐레이션 자료를 찾을 수 없습니다.\"}"
                                    )
                            }
                    )
            )
    })
    @GetMapping("/{presentationId}/curations")
    public ApiResponse<List<CurationResponse>> getCurations(@PathVariable Long presentationId) {

        List<CurationResponse> response = curationService.getCurationList(presentationId);

        return ApiResponse.success(response);
    }
}