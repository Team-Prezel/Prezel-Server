package com.finger.handoff.domain.presentation.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/** Versioned, reviewed instructions and the 14 presentation-specific modules. */
public final class PresentationPromptPolicy {
    private final JsonNode policy;

    public PresentationPromptPolicy(ObjectMapper mapper) {
        try (var resource = PresentationPromptPolicy.class.getResourceAsStream("/prompts/presentation.json")) {
            if (resource == null) throw new IllegalStateException("발표 프롬프트 리소스가 없습니다.");
            policy = mapper.readTree(resource);
            for (String field : List.of("common", "summary", "scriptErrors", "questions", "finalCheck")) {
                if (!policy.path(field).isTextual() || policy.path(field).asText().isBlank()) {
                    throw new IllegalStateException("발표 프롬프트 항목이 없습니다: " + field);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("발표 프롬프트를 읽을 수 없습니다.", exception);
        }
    }

    public PresentationPromptBuilder.Instructions instructions(PresentationPromptInput input, String customSummary) {
        String summary = customSummary == null || customSummary.isBlank() ? policy.path("summary").asText() : customSummary;
        String[] axes = {"type", "purpose", "style", "audience"};
        String[] values = {input.type(), input.purpose(), input.style(), input.audience()};
        for (int i = 0; i < axes.length; i++) {
            if (values[i] == null) continue;
            JsonNode module = policy.path("modules").path(axes[i]).path(values[i]);
            if (!module.path("name").isTextual() || !module.path("compact").isTextual()) {
                throw new IllegalArgumentException("지원하지 않는 발표 평가 항목입니다: " + axes[i] + "/" + values[i]);
            }
            summary += "\n[" + axes[i] + ": " + module.path("name").asText() + "] " + module.path("compact").asText();
        }
        return new PresentationPromptBuilder.Instructions(policy.path("common").asText(), summary,
                policy.path("scriptErrors").asText(), policy.path("questions").asText(), policy.path("finalCheck").asText());
    }
}
