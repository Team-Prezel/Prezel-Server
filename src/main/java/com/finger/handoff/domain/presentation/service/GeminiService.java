package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableRetry
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 연결 대기 5초
        factory.setReadTimeout(30000);    // 응답 대기 30초
        this.restTemplate = new RestTemplate(factory);
    }

    @Getter
    @Builder
    public static class GeminiAllInOneResponse {
        private String summaryFeedback;
        private int spellErrorCount;
        private int grammarErrorCount;
        private String scriptDetailsJson;
        private String expectedQuestionsJson;
    }

    private String getSummaryFeedbackInstruction() {
        return "[작업 1: 요약 피드백 작성]\n" +
                "장점은 칭찬해주고, 개선할 점(속도, 발음, 더듬음 등)은 부드럽게 조언해주는 따뜻한 스피치 코치의 말투로 3~4문장 분량의 '종합 요약 피드백'을 작성해.";
    }

    private String getScriptErrorInstruction() {
        return "[작업 2: 대본 오류 분석]\n" +
                "깐깐한 교정기처럼 원본 대본 내용에 있는 '맞춤법(SPELLING)' 및 '주술 호응(GRAMMAR)' 오류를 모두 찾아내. " +
                "그리고 각 오류에 대해 왜 틀렸는지, 왜 그렇게 교정했는지 'reason' 필드에 교정 이유를 구체적으로 작성해줘. " +
                "(단, 'sentence' 필드에는 오류를 정확히 매핑할 수 있도록 원본 대본에 있는 문장을 띄어쓰기 훼손 없이 정확히 그대로 복사해서 넣어줘.)";
    }

    private String getExpectedQuestionsInstruction() {
        return "[작업 3: 예상 질문 및 모범 답변 생성]\n" +
                "날카로운 면접관의 입장에서, 발표가 끝난 뒤 나올 수 있는 '핵심 예상 질문'과 '모범 답변' 3개 세트를 작성해. 원본 대본이 짧더라도 응용해서 억지로라도 3개를 만들어내.";
    }

    @Retryable(
            value = { Exception.class }, // 모든 예외에 대해
            maxAttempts = 3,             // 최대 3번 시도
            backoff = @Backoff(delay = 2000) // 실패 시 2초 대기 후 재시도
    )
    public GeminiAllInOneResponse analyzeAll(AzureSpeechService.AzureAnalysisDto azureResult, String originalScript, String customInstruction) {
        String prompt = buildAllInOnePrompt(azureResult, originalScript, customInstruction);

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

            log.info("Gemini All-in-One 응답: {}", content);

            int jsonStartIndex = content.indexOf("{");
            int jsonEndIndex = content.lastIndexOf("}");
            if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex < jsonEndIndex) {
                content = content.substring(jsonStartIndex, jsonEndIndex + 1);
            } else {
                throw new RuntimeException("JSON 객체를 찾을 수 없습니다.");
            }

            JsonNode aiData = objectMapper.readTree(content);

            String summary = aiData.path("summaryFeedback").asText("피드백을 생성하지 못했습니다.");

            JsonNode scriptErrorsNode = aiData.path("scriptErrors");

            if (scriptErrorsNode.isArray()) {
                int globalSearchCursor = 0;

                for (JsonNode error : scriptErrorsNode) {
                    if (error.isObject()) {
                        ObjectNode errorObj = (ObjectNode) error;

                        String sentence = errorObj.path("sentence").asText("");
                        String originalText = errorObj.path("originalText").asText("");

                        int errorStartIndex = -1;
                        int errorEndIndex = -1;

                        if (originalScript != null && !originalText.isEmpty()) {

                            int searchStart = Math.max(0, globalSearchCursor - (sentence.length() > 0 ? sentence.length() : 50));
                            int sentenceIndex = originalScript.indexOf(sentence, searchStart);

                            while (sentenceIndex != -1) {
                                int wordSearchStart = Math.max(sentenceIndex, globalSearchCursor);
                                errorStartIndex = originalScript.indexOf(originalText, wordSearchStart);

                                if (errorStartIndex != -1 && errorStartIndex <= sentenceIndex + sentence.length()) {
                                    break;
                                } else {
                                    sentenceIndex = originalScript.indexOf(sentence, sentenceIndex + 1);
                                    errorStartIndex = -1;
                                }
                            }

                            if (errorStartIndex == -1) {
                                errorStartIndex = originalScript.indexOf(originalText, globalSearchCursor);
                                if (errorStartIndex == -1) {
                                    errorStartIndex = originalScript.indexOf(originalText);
                                }
                            }

                            if (errorStartIndex != -1) {
                                errorEndIndex = errorStartIndex + originalText.length();
                                globalSearchCursor = errorEndIndex;
                            }
                        }

                        errorObj.put("startIndex", errorStartIndex);
                        errorObj.put("endIndex", errorEndIndex);
                    }
                }
            }

            String scriptDetailsJson = scriptErrorsNode.isMissingNode() ? "[]" : scriptErrorsNode.toString();

            JsonNode expectedQuestionsNode = aiData.path("expectedQuestions");
            String expectedQuestionsJson = expectedQuestionsNode.isMissingNode() ? "[]" : expectedQuestionsNode.toString();

            int spellCount = 0;
            int grammarCount = 0;
            if (scriptErrorsNode.isArray()) {
                for (JsonNode error : scriptErrorsNode) {
                    String type = error.path("errorType").asText("");
                    if ("SPELLING".equalsIgnoreCase(type)) spellCount++;
                    if ("GRAMMAR".equalsIgnoreCase(type)) grammarCount++;
                }
            }

            return GeminiAllInOneResponse.builder()
                    .summaryFeedback(summary)
                    .spellErrorCount(spellCount)
                    .grammarErrorCount(grammarCount)
                    .scriptDetailsJson(scriptDetailsJson)
                    .expectedQuestionsJson(expectedQuestionsJson)
                    .build();

        } catch (Exception e) {
            log.error("Gemini 통합 분석 중 에러 발생: ", e);
            throw new RuntimeException("AI 연동 실패", e);
        }
    }

    private String buildAllInOnePrompt(AzureSpeechService.AzureAnalysisDto result, String originalScript, String customInstruction) {
        StringBuilder sb = new StringBuilder();
        boolean hasScript = originalScript != null && !originalScript.trim().isEmpty();

        sb.append("다음 제공되는 사용자의 [발표 음성 데이터]와 [원본 대본]을 분석해줘.\n\n");

        sb.append("[발표 음성 데이터]\n");
        sb.append("- 총 발표 시간: ").append(result.getDurationSeconds() != null ? result.getDurationSeconds() : 0).append("초\n");
        sb.append("- 발화 속도(SPM): ").append(result.getSpm()).append("자/분\n");
        if (result.getAccuracyScore() != null) {
            sb.append("- 발음 정확도 점수: ").append(String.format("%.1f", result.getAccuracyScore())).append("점 / 100점\n");
        }

        if (result.getSentenceDetails() != null) {
            long stutterCount = 0;
            long insertionCount = 0;

            for (PresentationDTO.SentenceAnalysisDetail sentenceDetail : result.getSentenceDetails()) {
                if (sentenceDetail.getWordDetails() != null) {
                    stutterCount += sentenceDetail.getWordDetails().stream()
                            .filter(w -> "Stutter".equals(w.getStatus())).count();
                    insertionCount += sentenceDetail.getWordDetails().stream()
                            .filter(w -> "Insertion".equals(w.getStatus())).count();
                }
            }

            sb.append("- 더듬거나 반복한 횟수: ").append(stutterCount).append("회\n");
            sb.append("- 불필요한 추임새 횟수: ").append(insertionCount).append("회\n");
        }

        if (hasScript) {
            sb.append("\n[원본 대본]\n").append(originalScript).append("\n\n");
            sb.append("위 데이터를 바탕으로 다음 3가지 요청 사항을 순서대로 수행해.\n");

            if (customInstruction != null && !customInstruction.trim().isEmpty()) {
                sb.append("[작업 1: 요약 피드백 작성]\n").append(customInstruction).append("\n\n");
            } else {
                sb.append(getSummaryFeedbackInstruction()).append("\n\n");
            }

            sb.append(getScriptErrorInstruction()).append("\n\n");
            sb.append(getExpectedQuestionsInstruction()).append("\n\n");
        } else {
            sb.append("\n[원본 대본]\n").append("없음(사용자가 대본을 제공하지 않음)\n\n");
            sb.append("위 데이터를 바탕으로 다음 1가지 요청 사항을 수행해.\n");

            if (customInstruction != null && !customInstruction.trim().isEmpty()) {
                sb.append("[작업 1: 요약 피드백 작성]\n").append(customInstruction).append("\n\n");
            } else {
                sb.append(getSummaryFeedbackInstruction()).append("\n\n");
            }
        }

        sb.append("결과는 반드시 아래의 단일 JSON 객체(Object) 형식으로만 응답해. 절대 ```json 이나 추가 설명을 덧붙이지 마.\n");
        sb.append("{\n");
        sb.append("  \"summaryFeedback\": \"요약 피드백 텍스트\"");

        if (hasScript) {
            sb.append(",\n");
            sb.append("  \"scriptErrors\": [\n");
            sb.append("    {\n");
            sb.append("      \"errorType\": \"SPELLING\",\n");
            sb.append("      \"sentence\": \"틀린 부분이 포함된 전체 원본 문장\",\n");
            sb.append("      \"originalText\": \"틀린 부분\",\n");
            sb.append("      \"correctedText\": \"교정된 텍스트\",\n");
            sb.append("      \"reason\": \"교정 이유\"\n");
            sb.append("    }\n");
            sb.append("  ],\n");
            sb.append("  \"expectedQuestions\": [\n");
            sb.append("    {\n");
            sb.append("      \"question\": \"예상 질문 1\",\n");
            sb.append("      \"answer\": \"모범 답변 1\"\n");
            sb.append("    }\n");
            sb.append("  ]\n");
        } else {
            sb.append("\n");
        }
        sb.append("}");

        return sb.toString();
    }


    // 테스트 타입을 구분하기 위한 Enum
    public enum PromptTestType {
        SUMMARY, SCRIPT, QUESTION
    }

    // [기획자용] 단일 기능 테스트 로직 (실제 운영 로직의 데이터 구조를 똑같이 주입)
    public JsonNode testSinglePrompt(AzureSpeechService.AzureAnalysisDto result, String originalScript, String instruction, PromptTestType type) {
        StringBuilder sb = new StringBuilder();
        boolean hasScript = originalScript != null && !originalScript.trim().isEmpty();

        // 실서버와 동일하게 STT 데이터 매핑
        sb.append("다음 제공되는 사용자의 [발표 음성 데이터]와 [원본 대본]을 분석해줘.\n\n");
        sb.append("[발표 음성 데이터]\n");
        sb.append("- 총 발표 시간: ").append(result.getDurationSeconds() != null ? result.getDurationSeconds() : 0).append("초\n");
        sb.append("- 발화 속도(SPM): ").append(result.getSpm()).append("자/분\n");
        if (result.getAccuracyScore() != null) {
            sb.append("- 발음 정확도 점수: ").append(String.format("%.1f", result.getAccuracyScore())).append("점 / 100점\n");
        }

        // 실서버와 동일하게 더듬음/추임새 횟수 계산
        if (result.getSentenceDetails() != null) {
            long stutterCount = 0;
            long insertionCount = 0;
            for (PresentationDTO.SentenceAnalysisDetail sentenceDetail : result.getSentenceDetails()) {
                if (sentenceDetail.getWordDetails() != null) {
                    stutterCount += sentenceDetail.getWordDetails().stream()
                            .filter(w -> "Stutter".equals(w.getStatus())).count();
                    insertionCount += sentenceDetail.getWordDetails().stream()
                            .filter(w -> "Insertion".equals(w.getStatus())).count();
                }
            }
            sb.append("- 더듬거나 반복한 횟수: ").append(stutterCount).append("회\n");
            sb.append("- 불필요한 추임새 횟수: ").append(insertionCount).append("회\n");
        }

        sb.append("\n[원본 대본]\n").append(hasScript ? originalScript : "없음(사용자가 대본을 제공하지 않음)\n\n");

        // 기획자가 입력한 프롬프트 지시사항 주입
        sb.append("위 데이터를 바탕으로 다음 1가지 요청 사항을 수행해.\n");
        sb.append("[작업 지시사항]\n").append(instruction).append("\n\n");

        // 해당 기능에 맞는 JSON 스키마만 강제
        sb.append("결과는 반드시 아래의 단일 JSON 객체(Object) 형식으로만 응답해. 절대 ```json 이나 추가 설명을 덧붙이지 마.\n{\n");
        if (type == PromptTestType.SUMMARY) {
            sb.append("  \"summaryFeedback\": \"요약 피드백 텍스트\"\n");
        } else if (type == PromptTestType.SCRIPT) {
            sb.append("  \"scriptErrors\": [\n    {\n      \"errorType\": \"SPELLING\",\n      \"sentence\": \"틀린 부분이 포함된 전체 원본 문장\",\n      \"originalText\": \"틀린 부분\",\n      \"correctedText\": \"교정된 텍스트\",\n      \"reason\": \"교정 이유\"\n    }\n  ]\n");
        } else if (type == PromptTestType.QUESTION) {
            sb.append("  \"expectedQuestions\": [\n    {\n      \"question\": \"예상 질문 1\",\n      \"answer\": \"모범 답변 1\"\n    }\n  ]\n");
        }
        sb.append("}");

        // 테스트 전용 메서드 호출
        return executeRawGeminiRequest(sb.toString());
    }

    // [테스트 전용] 제미나이 API 호출 및 순수 JSON(JsonNode) 반환 로직
    private JsonNode executeRawGeminiRequest(String prompt) {
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

            log.info("Gemini Single Test 응답: {}", content);

            // 제미나이 응답에서 순수 JSON 객체 부분만 추출
            int jsonStartIndex = content.indexOf("{");
            int jsonEndIndex = content.lastIndexOf("}");
            if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex < jsonEndIndex) {
                content = content.substring(jsonStartIndex, jsonEndIndex + 1);
            } else {
                throw new RuntimeException("JSON 객체를 찾을 수 없습니다.");
            }

            return objectMapper.readTree(content);

        } catch (Exception e) {
            log.error("단일 프롬프트 테스트 중 제미나이 연동 에러 발생: ", e);
            throw new RuntimeException("AI 연동 실패 (Test)", e);
        }
    }
}