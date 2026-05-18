package com.finger.handoff.domain.presentation.service;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.global.audio.AudioConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class PresentationService {

    private final AudioConverter audioConverter;
    private final AzureSpeechService azureSpeechService;

    // 분석 후 DTO 반환, DB 저장은 하지 않음
    public PresentationDTO.AnalysisResponse analyzePresentation(PresentationDTO.PresentationRequest request) {
        File wavFile = null;
        try {
            // 1. 오디오 포맷 변환 (.wav)
            wavFile = audioConverter.convertToWav(request.getAudio());

            // 2. Azure Speech API 분석 (대본 유/무 자동 분기)
            AzureSpeechService.AzureAnalysisDto azureResult =
                    azureSpeechService.analyzePronunciation(wavFile.getAbsolutePath(), request.getScript());

            // 3. 요약 피드백 생성 (현재는 임시 문자열, 추후 LLM 연동 예정)
            String summaryFeedback = "전달은 안정적으로 잘 되고 있어요. 이제 속도와 리듬을 조금만 다듬어볼게요. 문장 흐름이 더 좋아지려면 문장 속에 키워드가 3개 이상 들어가지 않는 게 좋아요. 지금처럼만 하면 전달력은 계속 좋아질 수 있어요.";

            // 4. 응답 DTO 구성 및 반환
            return PresentationDTO.AnalysisResponse.builder()
                    .name(request.getName())
                    .type(request.getType())
                    .purpose(request.getPurpose())
                    .style(request.getStyle())
                    .audience(request.getAudience())
                    .durationSeconds(azureResult.getDurationSeconds())
                    .spm(azureResult.getSpm())
                    .speedEval(azureResult.getSpeedEval())
                    .summaryFeedback(summaryFeedback)
                    // 대본이 없을 경우 아래 3개 필드는 null 처리됨
                    .accuracyScore(azureResult.getAccuracyScore())
                    .scriptMatchRate(azureResult.getScriptMatchRate())
                    .wordDetails(azureResult.getWordDetails())
                    .build();

        } finally {
            // 5. 사용한 변환 파일 폐기 (메모리 및 서버 용량 확보)
            // AudioConverter에서 생성된 임시 파일들은 자체적으로 삭제하지만,
            // 반환받은 최종 wav 파일은 여기서 삭제해야 합니다.
            if (wavFile != null && wavFile.exists()) {
                wavFile.delete();
            }
        }
    }
}