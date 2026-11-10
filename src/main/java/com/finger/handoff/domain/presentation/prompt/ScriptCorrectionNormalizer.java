package com.finger.handoff.domain.presentation.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Validate source ranges without attempting to judge or invent linguistic corrections. */
public final class ScriptCorrectionNormalizer {
    private record Edit(int start, int end, ObjectNode value) { }

    private ScriptCorrectionNormalizer() { }

    public static ArrayNode normalize(ObjectMapper mapper, JsonNode proposed, String source) {
        ArrayNode result = mapper.createArrayNode();
        if (source == null || source.isBlank() || !proposed.isArray()) return result;
        List<Edit> edits = new ArrayList<>();
        for (JsonNode raw : proposed) {
            if (!raw.isObject()) continue;
            ObjectNode item = groundSingleVariableTemplate(((ObjectNode) raw).deepCopy());
            if (!Set.of("SPELLING", "GRAMMAR").contains(item.path("errorType").asText())) continue;
            if (!text(item, "sentence") || !text(item, "originalText") || !text(item, "correctedText") || !text(item, "reason")) continue;
            String sentence = item.path("sentence").asText();
            String original = item.path("originalText").asText();
            if (!sentence.contains(original) || original.equals(item.path("correctedText").asText())) continue;
            Edit anchored = null;
            int sentenceStart = source.indexOf(sentence);
            while (sentenceStart >= 0 && anchored == null) {
                int relative = sentence.indexOf(original);
                while (relative >= 0) {
                    int start = sentenceStart + relative;
                    int end = start + original.length();
                    // Repeated occurrences in the source need separate ranges. Exact duplicates do not.
                    boolean used = edits.stream().anyMatch(edit -> edit.start() == start && edit.end() == end
                            && edit.value().path("correctedText").asText().equals(item.path("correctedText").asText()));
                    if (!used) {
                        ObjectNode value = ((ObjectNode) item).deepCopy();
                        if (original.replaceAll("(?U)\\s+", "").equals(value.path("correctedText").asText().replaceAll("(?U)\\s+", ""))) {
                            // Spacing-only corrections belong to SPELLING in the app's existing counter contract.
                            value.put("errorType", "SPELLING");
                        }
                        anchored = new Edit(start, end, value);
                        break;
                    }
                    relative = sentence.indexOf(original, relative + 1);
                }
                sentenceStart = source.indexOf(sentence, sentenceStart + 1);
            }
            if (anchored != null) edits.add(anchored);
        }
        edits.sort(Comparator.comparingInt(Edit::start));
        for (int i = 0; i < edits.size(); i++) {
            Edit edit = edits.get(i);
            boolean conflicts = false;
            for (int j = 0; j < edits.size(); j++) {
                if (i != j && edit.start() < edits.get(j).end() && edit.end() > edits.get(j).start()) {
                    conflicts = true;
                    break;
                }
            }
            if (conflicts) continue;
            edit.value().put("startIndex", edit.start());
            edit.value().put("endIndex", edit.end());
            result.add(edit.value());
        }
        return result;
    }

    private static boolean text(JsonNode item, String field) {
        return item.path(field).isTextual() && !item.path(field).asText().isBlank();
    }

    private static ObjectNode groundSingleVariableTemplate(ObjectNode item) {
        if (!item.path("errorType").asText().equals("GRAMMAR")) return item;
        String sentence = item.path("sentence").asText();
        String original = item.path("originalText").asText();
        String corrected = item.path("correctedText").asText();
        if (sentence.contains(original) || !original.contains(" ~ ") || !corrected.contains(" ~ ")
                || original.indexOf('~') != original.lastIndexOf('~') || corrected.indexOf('~') != corrected.lastIndexOf('~')) return item;
        int marker = original.indexOf('~');
        String prefix = original.substring(0, marker);
        String suffix = original.substring(marker + 1);
        if (prefix.isBlank() || suffix.isBlank() || !corrected.substring(0, corrected.indexOf('~')).equals(prefix)
                || sentence.indexOf(prefix) < 0 || sentence.indexOf(prefix) != sentence.lastIndexOf(prefix)
                || sentence.indexOf(suffix) < 0 || sentence.indexOf(suffix) != sentence.lastIndexOf(suffix)) return item;
        var match = Pattern.compile(Pattern.quote(prefix) + "(.+?)" + Pattern.quote(suffix)).matcher(sentence);
        if (!match.find() || match.group(1).matches("(?s).*[.!?。\\r\\n].*")) return item;
        String actual = match.group();
        String replacement = corrected.replace("~", match.group(1));
        if (actual.equals(replacement)) return item;
        item.put("originalText", actual);
        item.put("correctedText", replacement);
        return item;
    }
}
