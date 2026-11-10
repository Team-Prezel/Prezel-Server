package com.finger.handoff.domain.presentation.prompt;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Preserve explicit quoted comparisons; quotation marks alone do not protect a spelling error. */
public final class QuotedComparisonGuard {
    private record Quote(int start, int end, String text) { }
    private static final Pattern QUOTED = Pattern.compile("\"([^\"\\r\\n]*)\"|'([^'\\r\\n]*)'|“([^”\\r\\n]*)”|‘([^’\\r\\n]*)’");
    private static final Pattern EXPLANATION = Pattern.compile("표기|맞춤법|띄어쓰기|틀린|잘못된|바른|차이|비교");

    private QuotedComparisonGuard() { }

    public static ArrayNode filter(ArrayNode corrections, String source) {
        ArrayNode result = corrections.arrayNode();
        for (var correction : corrections) {
            String sentence = correction.path("sentence").asText();
            String original = correction.path("originalText").asText();
            String corrected = correction.path("correctedText").asText();
            boolean preserve = false;
            int sourceStart = correction.path("startIndex").asInt(-1);
            int sourceEnd = correction.path("endIndex").asInt(-1);
            int sentenceStart = source == null ? -1 : source.indexOf(sentence);
            while (sentenceStart >= 0 && (sourceStart < sentenceStart || sourceEnd > sentenceStart + sentence.length())) {
                sentenceStart = source.indexOf(sentence, sentenceStart + 1);
            }
            if (sentenceStart >= 0 && EXPLANATION.matcher(sentence).find()) {
                List<Quote> quotes = new ArrayList<>();
                var matcher = QUOTED.matcher(sentence);
                while (matcher.find()) quotes.add(new Quote(matcher.start(), matcher.end(), matcher.group()));
                for (Quote from : quotes) {
                    // A literal replacement within one quoted form must produce another quoted form.
                    // This does not infer whether arbitrary quoted speech is correct.
                    if (sourceStart - sentenceStart < from.start() || sourceEnd - sentenceStart > from.end()
                            || !from.text().contains(original)) continue;
                    String replacement = from.text().replace(original, corrected);
                    String content = unquote(replacement);
                    if (QUOTED.matcher(replacement).matches() && !replacement.equals(from.text()) && quotes.stream().anyMatch(to -> to.start() != from.start()
                            && unquote(to.text()).equals(content))) {
                        preserve = true;
                        break;
                    }
                }
            }
            if (!preserve) result.add(correction.deepCopy());
        }
        return result;
    }

    private static String unquote(String value) {
        if (value.length() < 2) return value;
        return value.substring(1, value.length() - 1);
    }
}
