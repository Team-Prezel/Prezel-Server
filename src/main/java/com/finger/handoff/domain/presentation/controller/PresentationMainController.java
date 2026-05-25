package com.finger.handoff.domain.presentation.controller;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.service.PresentationService;
import com.finger.handoff.global.common.ApiResponse;
import com.finger.handoff.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Presentation Main", description = "메인 화면 관리 API")
@RestController
@RequestMapping("/main")
@RequiredArgsConstructor
public class PresentationMainController {

    private final PresentationService presentationService;

    @Operation(
            summary = "메인 화면 조회",
            description = "사용자가 등록한 발표 중 발표일이 가장 가까운 순서대로 최대 3개의 발표 정보를 리스트로 조회합니다. 발표일이 1일 지난 발표(D+1)까지 노출되며, 지난 발표는 성장 변화율 정보가 채워져 반환됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (해당하는 발표가 없을 시 빈 배열 [] 반환)")
    })
    @GetMapping
    public ApiResponse<List<PresentationDTO.MainScreenResponse>> getMainScreenData(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        List<PresentationDTO.MainScreenResponse> responses = presentationService.getMainScreenData(customUserDetails.getUser());
        return ApiResponse.success(responses);
    }
}