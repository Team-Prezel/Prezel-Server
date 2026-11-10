package com.finger.handoff.domain.presentation.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

/** Conservative source-only supplement for an explicit price subject and a change predicate. */
public final class PriceTemporalSpellingRule {
    private static final Pattern TEMPORAL = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:물건값|가격|시세|금액|값)(?:이|가|은|는)\\h+(금새)\\h+"
                    + "(?:올랐|올라|오르(?=[다면는고지기며겠죠니나던])|오릅(?=니다)|내렸|내려|내리(?=[다면는고지기며겠죠니나던])|내립(?=니다)|(?:상승|하락)(?=[하했합해되됐]))");
    private static final Pattern QUOTED = Pattern.compile("\"[^\"\\r\\n]*\"|'[^'\\r\\n]*'|“[^”\\r\\n]*”|‘[^’\\r\\n]*’");
    private static final Pattern TEACHING = Pattern.compile("표기|맞춤법|띄어쓰기|오타|틀린|잘못된");

    private PriceTemporalSpellingRule() { }

    public static ArrayNode complete(ObjectMapper mapper, ArrayNode corrections, String source) {
        ArrayNode result = mapper.createArrayNode();
        var edits = new ArrayList<JsonNode>();
        corrections.forEach(edit -> edits.add(edit.deepCopy()));
        if (source != null) {
            var matches = TEMPORAL.matcher(source);
            while (matches.find()) {
                int start = matches.start(1), end = matches.end(1);
                if (edits.stream().anyMatch(e -> start < e.path("endIndex").asInt() && end > e.path("startIndex").asInt())) continue;
                var quotes = QUOTED.matcher(source);
                boolean quoted = false;
                while (quotes.find()) if (start >= quotes.start() && end <= quotes.end()) quoted = true;
                if (quoted) continue;
                int sentenceStart = start, sentenceEnd = end;
                while (sentenceStart > 0 && ".!?。\r\n".indexOf(source.charAt(sentenceStart - 1)) < 0) sentenceStart--;
                while (sentenceEnd < source.length() && ".!?。\r\n".indexOf(source.charAt(sentenceEnd)) < 0) sentenceEnd++;
                if (sentenceEnd < source.length()) sentenceEnd++;
                String sentence = source.substring(sentenceStart, sentenceEnd).strip();
                if (TEACHING.matcher(sentence).find()) continue;
                edits.add(mapper.createObjectNode().put("errorType", "SPELLING").put("sentence", sentence)
                        .put("originalText", "금새").put("correctedText", "금세")
                        .put("reason", "가격 변화가 빠르게 일어났다는 뜻의 부사는 '금세'로 써요.")
                        .put("startIndex", start).put("endIndex", end));
            }
        }
        edits.sort(Comparator.comparingInt(e -> e.path("startIndex").asInt()));
        edits.forEach(result::add);
        return result;
    }
}
