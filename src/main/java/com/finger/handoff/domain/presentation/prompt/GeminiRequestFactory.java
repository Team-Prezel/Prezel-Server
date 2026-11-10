package com.finger.handoff.domain.presentation.prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The same response contract is used by the application and prompt experiments. */
public final class GeminiRequestFactory {
    private GeminiRequestFactory() { }

    public static Map<String, Object> create(String prompt, boolean hasScript, PresentationPromptBuilder.Task task) {
        return Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json",
                        "responseJsonSchema", responseSchema(hasScript, task)));
    }

    public static Map<String, Object> responseSchema(boolean hasScript, PresentationPromptBuilder.Task task) {
        var fields = new LinkedHashMap<String, Object>();
        if (task == PresentationPromptBuilder.Task.ALL || task == PresentationPromptBuilder.Task.SUMMARY) {
            fields.put("summaryFeedback", Map.of("type", "string"));
        }
        if (task == PresentationPromptBuilder.Task.SCRIPT || hasScript && task == PresentationPromptBuilder.Task.ALL) {
            var error = new LinkedHashMap<String, Object>();
            error.put("errorType", Map.of("type", "string", "enum", List.of("SPELLING", "GRAMMAR")));
            for (String key : List.of("sentence", "originalText", "correctedText", "reason")) {
                error.put(key, Map.of("type", "string"));
            }
            fields.put("scriptErrors", array(object(error), hasScript ? null : 0));
        }
        if (task == PresentationPromptBuilder.Task.QUESTION || hasScript && task == PresentationPromptBuilder.Task.ALL) {
            var question = new LinkedHashMap<String, Object>();
            question.put("question", Map.of("type", "string"));
            question.put("answer", Map.of("type", "string"));
            var questions = new LinkedHashMap<>(array(object(question), hasScript ? 3 : 0));
            if (hasScript) questions.put("minItems", 1);
            fields.put("expectedQuestions", questions);
        }
        return object(fields);
    }

    private static Map<String, Object> array(Map<String, Object> items, Integer maxItems) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "array");
        schema.put("items", items);
        if (maxItems != null) schema.put("maxItems", maxItems);
        return schema;
    }

    private static Map<String, Object> object(LinkedHashMap<String, Object> properties) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }
}
