package com.finger.handoff.domain.presentation.prompt;

import com.finger.handoff.domain.presentation.dto.PresentationDTO;
import com.finger.handoff.domain.presentation.entity.Presentation;
import com.finger.handoff.domain.presentation.service.AzureSpeechService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Internal model input only. This record is never returned by an app endpoint. */
public record PresentationPromptInput(
        String preparedScript, Integer durationSeconds, Integer spm, String speedEval,
        Double accuracyScore, Long stutterTagCount, Long insertionTagCount,
        String type, String purpose, String style, String audience) {

    public static PresentationPromptInput from(AzureSpeechService.AzureAnalysisDto result,
                                               String script, Presentation presentation) {
        Objects.requireNonNull(result, "Azure analysis input is required");
        Integer duration = nonNegative(result.getDurationSeconds());
        Integer speed = nonNegative(result.getSpm());
        Double accuracy = result.getAccuracyScore();
        if (accuracy != null && (!Double.isFinite(accuracy) || accuracy < 0 || accuracy > 100)) accuracy = null;

        List<PresentationDTO.SentenceAnalysisDetail> sentences = result.getSentenceDetails();
        boolean hasWordData = sentences != null && sentences.stream().filter(Objects::nonNull)
                .anyMatch(sentence -> sentence.getWordDetails() != null);
        Long stutter = null;
        Long insertion = null;
        if (hasWordData) {
            var words = sentences.stream().filter(Objects::nonNull)
                    .filter(sentence -> sentence.getWordDetails() != null)
                    .flatMap(sentence -> sentence.getWordDetails().stream()).filter(Objects::nonNull).toList();
            stutter = words.stream().filter(word -> "Stutter".equals(word.getStatus())).count();
            insertion = words.stream().filter(word -> "Insertion".equals(word.getStatus())).count();
        }

        return new PresentationPromptInput(script == null || script.isBlank() ? null : script,
                duration, speed, speed == null ? null : SpeechSpeedPolicy.evaluate(speed), accuracy, stutter, insertion,
                presentation == null || presentation.getType() == null ? null : presentation.getType().name(),
                presentation == null || presentation.getPurpose() == null ? null : presentation.getPurpose().name(),
                presentation == null || presentation.getStyle() == null ? null : presentation.getStyle().name(),
                presentation == null || presentation.getAudience() == null ? null : presentation.getAudience().name());
    }

    public boolean hasScript() {
        return preparedScript != null;
    }

    public Map<String, Object> toModelData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("preparedScript", preparedScript);
        data.put("durationSeconds", durationSeconds);
        data.put("spm", spm);
        data.put("speedEval", speedEval);
        data.put("accuracyScore", accuracyScore);
        data.put("stutterTagCount", stutterTagCount);
        data.put("insertionTagCount", insertionTagCount);
        Map<String, Object> conditions = new LinkedHashMap<>();
        conditions.put("type", type);
        conditions.put("purpose", purpose);
        conditions.put("style", style);
        conditions.put("audience", audience);
        data.put("presentationConditions", conditions);
        return data;
    }

    private static Integer nonNegative(Integer value) {
        return value == null || value < 0 ? null : value;
    }
}
