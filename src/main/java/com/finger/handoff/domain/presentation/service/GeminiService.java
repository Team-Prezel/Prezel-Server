package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate(); // 간단한 호출을 위해 직접 생성

    public String generateSummaryFeedback(AzureSpeechService.AzureAnalysisDto azureResult) {
        // 1. 프롬프트 생성 (Azure 분석 데이터를 문장으로 풀어서 전달)
        String prompt = buildPrompt(azureResult);

        // 2. Gemini API 요청 본문(Body) 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", prompt)
                ))
        ));

        // 3. HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. API 호출
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            // 5. JSON 응답 파싱 및 피드백 텍스트 추출
            JsonNode rootNode = objectMapper.readTree(responseStr);
            return rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: ", e);
            return "현재 AI 피드백을 생성할 수 없습니다. 잠시 후 다시 시도해주세요."; // 장애 발생 시 기본 메시지
        }
    }

    // 💡 AI에게 전달할 프롬프트(명령어)를 조합하는 메서드
    private String buildPrompt(AzureSpeechService.AzureAnalysisDto result) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 따뜻하고 전문적인 발표 스피치 코치야. 다음은 사용자의 발표 음성 분석 데이터야.\n");
        sb.append("- 총 발표 시간: ").append(result.getDurationSeconds()).append("초\n");
        sb.append("- 발화 속도(SPM): ").append(result.getSpm()).append("자/분 (평가: ").append(result.getSpeedEval()).append(")\n");

        if (result.getAccuracyScore() != null) {
            sb.append("- 발음 정확도 점수: ").append(String.format("%.1f", result.getAccuracyScore())).append("점 / 100점\n");
            sb.append("- 대본 일치율: ").append(String.format("%.1f", result.getScriptMatchRate())).append("%\n");
        }

        // 나쁜 습관(더듬음, 추임새) 개수 카운트
        if (result.getWordDetails() != null) {
            long stutterCount = result.getWordDetails().stream().filter(w -> "Stutter".equals(w.getStatus())).count();
            long insertionCount = result.getWordDetails().stream().filter(w -> "Insertion".equals(w.getStatus())).count();
            sb.append("- 더듬거나 반복한 횟수: ").append(stutterCount).append("회\n");
            sb.append("- 불필요한 추임새(어, 음 등) 횟수: ").append(insertionCount).append("회\n");
        }

        sb.append("\n이 데이터를 바탕으로 발표자에게 도움이 될 만한 '종합 요약 피드백'을 3~4문장 분량으로 자연스럽게 작성해줘.");
        sb.append("장점은 칭찬해주고, 개선할 점(속도, 발음, 더듬음 등)은 부드럽게 조언해주는 말투로 써줘. 마크다운 기호 없이 순수 텍스트로만 반환해.");

        return sb.toString();
    }
}