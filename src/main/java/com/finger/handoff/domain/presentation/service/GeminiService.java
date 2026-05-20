package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
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
    private final RestTemplate restTemplate = new RestTemplate();

    @Getter
    @Builder
    public static class ScriptErrorResult {
        private int spellErrorCount;
        private int grammarErrorCount;
        private String scriptDetailsJson;
    }

    // 1️⃣ 기존 요약 피드백 생성 로직 (유지)
    public String generateSummaryFeedback(AzureSpeechService.AzureAnalysisDto azureResult) {
        String prompt = buildPrompt(azureResult);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(responseStr);
            return rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

        } catch (Exception e) {
            log.error("Gemini 피드백 호출 중 오류 발생: ", e);
            return "현재 AI 서버 혼잡으로 피드백을 생성할 수 없습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    public ScriptErrorResult analyzeScriptErrors(String originalScript) {
        String prompt = buildScriptAnalysisPrompt(originalScript);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(responseStr);
            String content = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            int startIndex = content.indexOf("[");
            int endIndex = content.lastIndexOf("]");
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                content = content.substring(startIndex, endIndex + 1);
            } else {
                content = "[]";
            }

            JsonNode errorsNode = objectMapper.readTree(content);
            int spellCount = 0;
            int grammarCount = 0;

            if (errorsNode.isArray()) {
                for (JsonNode error : errorsNode) {
                    String type = error.path("errorType").asText();
                    if ("SPELLING".equalsIgnoreCase(type)) spellCount++;
                    if ("GRAMMAR".equalsIgnoreCase(type)) grammarCount++;
                }
            }

            return ScriptErrorResult.builder()
                    .spellErrorCount(spellCount)
                    .grammarErrorCount(grammarCount)
                    .scriptDetailsJson(content)
                    .build();

        } catch (Exception e) {
            log.error("Gemini 대본 분석 호출 중 오류 발생: ", e);
            return ScriptErrorResult.builder()
                    .spellErrorCount(0)
                    .grammarErrorCount(0)
                    .scriptDetailsJson("[]")
                    .build();
        }
    }

    private String buildPrompt(AzureSpeechService.AzureAnalysisDto result) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 따뜻하고 전문적인 발표 스피치 코치야. 다음은 사용자의 발표 음성 분석 데이터야.\n");
        sb.append("- 총 발표 시간: ").append(result.getDurationSeconds()).append("초\n");
        sb.append("- 발화 속도(SPM): ").append(result.getSpm()).append("자/분 (평가: ").append(result.getSpeedEval()).append(")\n");

        if (result.getAccuracyScore() != null) {
            sb.append("- 발음 정확도 점수: ").append(String.format("%.1f", result.getAccuracyScore())).append("점 / 100점\n");
            sb.append("- 대본 일치율: ").append(String.format("%.1f", result.getScriptMatchRate())).append("%\n");
        }

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

    private String buildScriptAnalysisPrompt(String originalScript) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 깐깐하고 전문적인 한국어 맞춤법 및 주술 호응 교정기야.\n");
        sb.append("다음은 사용자가 작성한 '원본 대본(Script)'이야.\n\n");

        sb.append("[원본 대본]\n").append(originalScript).append("\n\n");

        sb.append("위 대본을 꼼꼼하게 분석해서 '맞춤법(SPELLING)' 오류와 '주술 호응(GRAMMAR)' 오류를 모두 찾아내.\n");
        sb.append("결과는 반드시 아래 JSON 배열 형식으로만 응답해야 해. 마크다운 기호나 추가 설명은 절대 넣지 마.\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"errorType\": \"SPELLING\",\n");
        sb.append("    \"sentence\": \"오늘 제가 맡은 발표할 주제는 인공지능입니다.\", // 🔥 오류가 발생한 '정확한 원본 문장 전체' (프론트에서 위치 찾기용)\n");
        sb.append("    \"originalText\": \"맡은 발표할\", // 틀린 부분\n");
        sb.append("    \"correctedText\": \"발표할\", // 올바르게 교정된 텍스트\n");
        sb.append("    \"reason\": \"'맡은'이라는 표현이 문맥상 불필요하므로 삭제하는 것이 자연스럽습니다.\"\n");
        sb.append("  }\n");
        sb.append("]\n");
        sb.append("오류가 전혀 없다면 빈 배열 [] 을 반환해.");

        return sb.toString();
    }
}