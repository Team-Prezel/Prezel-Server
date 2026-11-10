package com.finger.handoff.domain.presentation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.prompt.PresentationPromptBuilder;
import com.finger.handoff.domain.presentation.prompt.GeminiRequestFactory;
import com.finger.handoff.domain.presentation.prompt.ScriptCorrectionNormalizer;
import com.finger.handoff.domain.presentation.prompt.PresentationPromptInput;
import com.finger.handoff.domain.presentation.prompt.CorrectionReviewPromptBuilder;
import com.finger.handoff.domain.presentation.prompt.QuotedComparisonGuard;
import com.finger.handoff.domain.presentation.prompt.NounSpellingGuard;
import com.finger.handoff.domain.presentation.prompt.PriceTemporalSpellingRule;
import com.finger.handoff.domain.presentation.prompt.PresentationPromptPolicy;
import com.finger.handoff.domain.presentation.prompt.EditorialSpellingSynchronizer;
import com.finger.handoff.domain.presentation.prompt.FeedbackReviewPromptBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.Map;

@Slf4j
@Service
@EnableRetry
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.correction-review.enabled:true}")
    private boolean correctionReviewEnabled = true;

    @Value("${gemini.correction-review.mode:DECISIONS}")
    private String correctionReviewMode = "DECISIONS";

    @Value("${gemini.optimized-prompt.enabled:true}")
    private boolean optimizedPromptEnabled = true;

    @Value("${gemini.feedback-review.enabled:true}")
    private boolean feedbackReviewEnabled = true;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final PresentationPromptBuilder promptBuilder;
    private final CorrectionReviewPromptBuilder correctionReviewPromptBuilder;
    private final PresentationPromptPolicy promptPolicy;
    private final FeedbackReviewPromptBuilder feedbackReviewPromptBuilder;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.promptBuilder = new PresentationPromptBuilder(objectMapper);
        this.correctionReviewPromptBuilder = new CorrectionReviewPromptBuilder(objectMapper);
        this.promptPolicy = new PresentationPromptPolicy(objectMapper);
        this.feedbackReviewPromptBuilder = new FeedbackReviewPromptBuilder(objectMapper);

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
        return analyze(azureResult, originalScript, customInstruction, null);
    }

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public GeminiAllInOneResponse analyzeAll(AzureSpeechService.AzureAnalysisDto azureResult, String originalScript,
                                            String customInstruction, Presentation presentation) {
        return analyze(azureResult, originalScript, customInstruction, presentation);
    }

    private GeminiAllInOneResponse analyze(AzureSpeechService.AzureAnalysisDto azureResult, String originalScript,
                                           String customInstruction, Presentation presentation) {
        String prompt = buildAllInOnePrompt(azureResult, originalScript, customInstruction, presentation);

        Map<String, Object> requestBody = GeminiRequestFactory.create(prompt,
                originalScript != null && !originalScript.isBlank(), PresentationPromptBuilder.Task.ALL);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(responseStr);
            String content = extractResponseText(rootNode);

            log.info("Gemini All-in-One 응답 수신 완료 (응답 길이: {}자)", content != null ? content.length() : 0);
            log.debug("Gemini All-in-One 응답 원문: {}", content);

            int jsonStartIndex = content.indexOf("{");
            int jsonEndIndex = content.lastIndexOf("}");
            if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex < jsonEndIndex) {
                content = content.substring(jsonStartIndex, jsonEndIndex + 1);
            } else {
                log.error("Gemini 응답에서 JSON 객체를 찾을 수 없습니다. 응답 내용: {}", content);
                throw new RuntimeException("JSON 객체를 찾을 수 없습니다.");
            }

            JsonNode aiData = objectMapper.readTree(content);
            JsonNode editorialData = aiData;

            String summary = aiData.path("summaryFeedback").asText("피드백을 생성하지 못했습니다.");

            JsonNode proposedCorrections = ScriptCorrectionNormalizer.normalize(objectMapper,
                    aiData.path("scriptErrors"), originalScript);
            if (correctionReviewEnabled && originalScript != null && !originalScript.isBlank()) {
                boolean reviewFeedback = optimizedPromptEnabled && feedbackReviewEnabled;
                var mode = reviewFeedback ? CorrectionReviewPromptBuilder.Mode.DECISIONS
                        : CorrectionReviewPromptBuilder.Mode.valueOf(correctionReviewMode);
                JsonNode reviewed = executeJsonRequest(reviewFeedback
                        ? feedbackReviewPromptBuilder.build(PresentationPromptInput.from(azureResult, originalScript, presentation),
                                proposedCorrections, aiData, customInstruction)
                        : correctionReviewPromptBuilder.build(originalScript, proposedCorrections, mode));
                proposedCorrections = correctionReviewPromptBuilder.resolve(proposedCorrections, reviewed, mode);
                if (reviewFeedback) {
                    feedbackReviewPromptBuilder.validate(reviewed);
                    editorialData = reviewed;
                    summary = reviewed.path("summaryFeedback").asText();
                }
            }
            JsonNode scriptErrorsNode = PriceTemporalSpellingRule.complete(objectMapper,
                    NounSpellingGuard.filter(QuotedComparisonGuard.filter(
                    ScriptCorrectionNormalizer.normalize(objectMapper, proposedCorrections, originalScript), originalScript), originalScript), originalScript);
            String scriptDetailsJson = scriptErrorsNode.toString();

            JsonNode expectedQuestionsNode = editorialData.path("expectedQuestions");
            if (optimizedPromptEnabled) {
                if (originalScript != null && !originalScript.isBlank()) {
                    feedbackReviewPromptBuilder.validateExpectedQuestions(expectedQuestionsNode);
                } else {
                    expectedQuestionsNode = objectMapper.createArrayNode();
                }
                summary = EditorialSpellingSynchronizer.rewrite(originalScript, scriptErrorsNode, summary);
                expectedQuestionsNode = expectedQuestionsNode.deepCopy();
                if (expectedQuestionsNode.isArray()) for (JsonNode question : expectedQuestionsNode) {
                    if (question.isObject()) for (String field : java.util.List.of("question", "answer")) {
                        if (question.path(field).isTextual()) ((ObjectNode) question).put(field,
                                EditorialSpellingSynchronizer.rewrite(originalScript, scriptErrorsNode, question.path(field).asText()));
                    }
                }
            }
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

        } catch (org.springframework.web.client.HttpStatusCodeException httpEx) {
            log.error("Gemini API HTTP 에러 발생 - StatusCode: {}, ResponseBody: {}",
                    httpEx.getStatusCode(), httpEx.getResponseBodyAsString(), httpEx);
            throw new RuntimeException("AI 연동 실패 (HTTP " + httpEx.getStatusCode() + ")", httpEx);
        } catch (Exception e) {
            log.error("Gemini 통합 분석 중 에러 발생: ", e);
            throw new RuntimeException("AI 연동 실패", e);
        }
    }

    private String buildAllInOnePrompt(AzureSpeechService.AzureAnalysisDto result, String originalScript, String customInstruction) {
        return buildAllInOnePrompt(result, originalScript, customInstruction, null);
    }

    private String buildAllInOnePrompt(AzureSpeechService.AzureAnalysisDto result, String originalScript,
                                       String customInstruction, Presentation presentation) {
        var input = PresentationPromptInput.from(result, originalScript, presentation);
        if (optimizedPromptEnabled) {
            return promptBuilder.build(input, promptPolicy.instructions(input, customInstruction), PresentationPromptBuilder.Task.ALL);
        }
        String summary = customInstruction == null || customInstruction.isBlank()
                ? getSummaryFeedbackInstruction() : customInstruction;
        return promptBuilder.build(PresentationPromptInput.from(result, originalScript, presentation),
                new PresentationPromptBuilder.Instructions(null, summary, getScriptErrorInstruction(),
                        getExpectedQuestionsInstruction(), null), PresentationPromptBuilder.Task.ALL);
    }


    // 테스트 타입을 구분하기 위한 Enum
    public enum PromptTestType {
        SUMMARY, SCRIPT, QUESTION
    }

    // [기획자용] 단일 기능 테스트 로직 (실제 운영 로직의 데이터 구조를 똑같이 주입)
    public JsonNode testSinglePrompt(AzureSpeechService.AzureAnalysisDto result, String originalScript, String instruction, PromptTestType type) {
        return testSinglePrompt(result, originalScript, instruction, type, null);
    }

    public JsonNode testSinglePrompt(AzureSpeechService.AzureAnalysisDto result, String originalScript,
                                     String instruction, PromptTestType type, Presentation presentation) {
        var input = PresentationPromptInput.from(result, originalScript, presentation);
        var instructions = new PresentationPromptBuilder.Instructions(null,
                type == PromptTestType.SUMMARY ? instruction : null,
                type == PromptTestType.SCRIPT ? instruction : null,
                type == PromptTestType.QUESTION ? instruction : null, null);
        if (optimizedPromptEnabled) {
            var defaults = promptPolicy.instructions(input, type == PromptTestType.SUMMARY ? instruction : null);
            instructions = new PresentationPromptBuilder.Instructions(defaults.common(), defaults.summary(),
                    type == PromptTestType.SCRIPT ? instruction : defaults.scriptErrors(),
                    type == PromptTestType.QUESTION ? instruction : defaults.questions(), defaults.finalCheck());
        }
        String prompt = promptBuilder.build(input,
                instructions, PresentationPromptBuilder.Task.valueOf(type.name()));
        JsonNode response = executeRawGeminiRequest(prompt, input.hasScript(),
                PresentationPromptBuilder.Task.valueOf(type.name()));
        if (optimizedPromptEnabled && input.hasScript() && type == PromptTestType.QUESTION) {
            feedbackReviewPromptBuilder.validateExpectedQuestions(response.path("expectedQuestions"));
        }
        return response;
    }

    // [테스트 전용] 제미나이 API 호출 및 순수 JSON(JsonNode) 반환 로직
    private JsonNode executeRawGeminiRequest(String prompt, boolean hasScript, PresentationPromptBuilder.Task task) {
        return executeJsonRequest(GeminiRequestFactory.create(prompt, hasScript, task));
    }

    private JsonNode executeJsonRequest(Map<String, Object> requestBody) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(responseStr);
            String content = extractResponseText(rootNode);

            log.info("Gemini JSON 응답 수신 완료 (응답 길이: {}자)", content.length());

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
            log.error("Gemini JSON 요청 처리 중 에러 발생: ", e);
            throw new RuntimeException("AI JSON 연동 실패", e);
        }
    }

    private String extractResponseText(JsonNode response) {
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        StringBuilder text = new StringBuilder();
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (!part.path("thought").asBoolean(false) && part.path("text").isTextual()) {
                    text.append(part.path("text").asText());
                }
            }
        }
        if (text.toString().isBlank()) {
            throw new IllegalStateException("Gemini 응답에 최종 텍스트가 없습니다.");
        }
        return text.toString();
    }
}
