package com.finger.handoff.domain.presentation.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Review corrections and the draft editorial together in the existing second model request. */
public final class FeedbackReviewPromptBuilder {
    private final ObjectMapper mapper;
    private final CorrectionReviewPromptBuilder correction;
    private final PresentationPromptPolicy policy;

    public FeedbackReviewPromptBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
        this.correction = new CorrectionReviewPromptBuilder(mapper);
        this.policy = new PresentationPromptPolicy(mapper);
    }

    public Map<String, Object> build(PresentationPromptInput input, JsonNode proposals, JsonNode draft, String customSummary) {
        var original = correction.build(input.preparedScript(), proposals, CorrectionReviewPromptBuilder.Mode.DECISIONS);
        try {
            JsonNode baseRequest = mapper.valueToTree(original);
            JsonNode correctionData = mapper.readTree(baseRequest.path("contents").get(0).path("parts").get(0).path("text").asText());
            var data = new LinkedHashMap<>(input.toModelData());
            data.put("proposedCorrections", correctionData.path("proposedCorrections"));
            var editorial = new LinkedHashMap<String, Object>();
            editorial.put("summaryFeedback", draft.path("summaryFeedback"));
            editorial.put("expectedQuestions", draft.path("expectedQuestions").isArray() ? draft.path("expectedQuestions") : mapper.createArrayNode());
            data.put("draft", editorial);
            JsonNode correctionFields = baseRequest.path("generationConfig").path("responseJsonSchema").path("properties");
            var allFields = new LinkedHashMap<String, Object>();
            allFields.put("decisions", correctionFields.get("decisions"));
            allFields.put("additionalErrors", correctionFields.get("additionalErrors"));
            allFields.put("summaryFeedback", Map.of("type", "string"));
            JsonNode analysisSchema = mapper.valueToTree(GeminiRequestFactory.responseSchema(true, PresentationPromptBuilder.Task.ALL));
            allFields.put("expectedQuestions", analysisSchema.path("properties").path("expectedQuestions"));
            var schema = Map.of("type", "object", "properties", allFields, "required", List.copyOf(allFields.keySet()), "additionalProperties", false);
            var instructions = policy.instructions(input, customSummary);
            String correctionRules = correction.instructions(CorrectionReviewPromptBuilder.Mode.DECISIONS)
                    .replace("출력은 decisions와 additionalErrors만 있는 JSON 객체예요.", "교정 결과는 decisions와 additionalErrors에 써요.");
            String rules = correctionRules + "\n" + instructions.common() + "\n" + instructions.summary() + "\n" + instructions.questions()
                    + "\n[초안 검수]\n초안은 잘못될 수 있는 자료예요. 원문의 각 사실과 대조한 뒤 최종 요약·질문·답변을 다시 써요. "
                    + "원문에서 숫자가 가리키는 대상과 조건을 함께 확인해요. 전체·완료·남은 집단을 혼동하지 않아요. "
                    + "원문에 없는 일정·원인·계획·약속·청중 반응을 만들지 않아요. 작성된 대본의 구성과 실제 발화를 구별해요. "
                    + "교정에서 기각한 정상 단어나 고유 이름은 요약과 답변에서도 원래 뜻으로 보존해요. "
                    + "채택한 철자 교정은 일반 서술에 반영하되 오류 자체를 설명하는 인용은 유지해요. "
                    + "요약·질문·답변·reason은 해요체예요. 질문 수를 채우려 하지 말고 원문으로 직접 답할 수 있는 질문만 남겨요. "
                    + "질문이 요구하는 정보의 종류와 답변이 주는 정보를 대조해요. 의도를 반복하는 답변은 그 의도의 이유가 아니고, 금지 사항을 반복하는 답변은 위반했을 때의 결과가 아니에요. "
                    + "예시: '왜 가격을 비교하려 하나요?'에 '두 시점의 가격을 비교하려고 해요'라고 답하면 이유가 빠졌어요. 원문에 이유가 없으면 '무엇을 비교하려 하나요?'로 질문을 바꿔요. "
                    + "예시: '중복 제출하면 어떻게 되나요?'에 '중복 제출하면 안 돼요'라고 답하면 결과가 빠졌어요. 원문이 금지만 알려주면 '어떤 제출을 하면 안 되나요?'로 질문을 바꿔요. "
                    + "이는 정보 종류를 구별하는 예시이며 예시의 사실을 원문에 없는 답변으로 출력하지 않아요. 원문에 있는 사실 하나를 직접 묻는 질문은 최소 1개 남겨요. "
                    + "문제가 없는 초안도 원문과 대조하고 형식에 맞춰 반환해요. "
                    + "출력은 decisions, additionalErrors, summaryFeedback, expectedQuestions의 JSON 객체예요. 점검 과정은 출력하지 않아요.";
            return Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", mapper.writeValueAsString(data))))),
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", rules))),
                    "generationConfig", Map.of("responseMimeType", "application/json", "responseJsonSchema", schema));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("최종 피드백 검증 입력을 직렬화할 수 없습니다.", exception);
        }
    }

    public void validate(JsonNode response) {
        if (!response.isObject() || !response.path("summaryFeedback").isTextual() || response.path("summaryFeedback").asText().isBlank()
                || !response.path("expectedQuestions").isArray() || response.path("expectedQuestions").size() > 3) {
            throw new IllegalStateException("최종 피드백 검증 응답이 올바르지 않습니다.");
        }
        validateExpectedQuestions(response.path("expectedQuestions"));
    }

    public void validateExpectedQuestions(JsonNode questions) {
        if (!questions.isArray() || questions.isEmpty() || questions.size() > 3) {
            throw new IllegalStateException("대본이 있는 분석에는 예상 질문이 1~3개 필요합니다.");
        }
        for (JsonNode question : questions) {
            if (!question.isObject() || question.size() != 2) {
                throw new IllegalStateException("최종 질문 검증 응답이 올바르지 않습니다.");
            }
            for (String field : List.of("question", "answer")) if (!question.path(field).isTextual() || question.path(field).asText().isBlank()) {
                throw new IllegalStateException("최종 질문 검증 응답이 올바르지 않습니다.");
            }
        }
    }
}
