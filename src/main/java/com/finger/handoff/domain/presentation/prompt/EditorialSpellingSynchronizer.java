package com.finger.handoff.domain.presentation.prompt;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** Reuse approved spelling edits only when a unique source context is copied into editorial text. */
public final class EditorialSpellingSynchronizer {
    private record Range(int start, int end) { }
    private record Edit(int start, int end, String replacement) { }
    private static final Pattern WORD = Pattern.compile("\\S+");
    private static final Pattern QUOTE = Pattern.compile("\"[^\"\\r\\n]*\"|'[^'\\r\\n]*'|“[^”\\r\\n]*”|‘[^’\\r\\n]*’");

    private EditorialSpellingSynchronizer() { }

    public static String rewrite(String source, JsonNode corrections, String text) {
        if (source == null || source.isBlank() || text == null || text.isBlank() || !corrections.isArray()) return text;
        List<Range> quotations = new ArrayList<>();
        var quoteMatcher = QUOTE.matcher(text);
        while (quoteMatcher.find()) quotations.add(new Range(quoteMatcher.start(), quoteMatcher.end()));
        List<Edit> edits = new ArrayList<>();
        for (JsonNode correction : corrections) {
            if (!correction.path("errorType").asText().equals("SPELLING")) continue;
            String original = correction.path("originalText").asText();
            String replacement = correction.path("correctedText").asText();
            String sentence = correction.path("sentence").asText();
            int start = correction.path("startIndex").asInt(-1);
            int end = correction.path("endIndex").asInt(-1);
            if (original.isBlank() || replacement.isBlank() || original.equals(replacement)
                    || start < 0 || end <= start || end > source.length() || !source.substring(start, end).equals(original)) continue;
            int sentenceStart = source.indexOf(sentence);
            while (sentenceStart >= 0 && (start < sentenceStart || end > sentenceStart + sentence.length())) {
                sentenceStart = source.indexOf(sentence, sentenceStart + 1);
            }
            if (sentenceStart < 0) continue;
            int localStart = start - sentenceStart;
            int localEnd = end - sentenceStart;
            List<Range> before = new ArrayList<>();
            List<Range> after = new ArrayList<>();
            var words = WORD.matcher(sentence);
            int contextEnd = localEnd;
            while (words.find()) {
                if (words.end() <= localStart) before.add(new Range(words.start(), words.end()));
                if (words.start() >= localEnd && words.end() < sentence.length()) after.add(new Range(words.start(), words.end()));
                if (words.start() < localEnd && localEnd <= words.end()) {
                    // Keep attached particles, but avoid copying a sentence-final speech-level ending.
                    String suffix = sentence.substring(localEnd, words.end());
                    if (suffix.matches("[은는이가을를의도만에]*")) contextEnd = words.end();
                }
            }
            int contextStart = before.isEmpty() ? localStart : before.get(Math.max(0, before.size() - 2)).start();
            if (!after.isEmpty()) contextEnd = after.get(Math.min(1, after.size() - 1)).end();
            String anchor = sentence.substring(contextStart, contextEnd);
            String context = sentence.substring(contextStart, localStart) + sentence.substring(localEnd, contextEnd);
            if (context.codePoints().filter(Character::isLetterOrDigit).count() < 2 || source.indexOf(anchor) != source.lastIndexOf(anchor)) continue;
            for (int at = text.indexOf(anchor); at >= 0; at = text.indexOf(anchor, at + 1)) {
                int editStart = at + localStart - contextStart;
                int editEnd = editStart + original.length();
                boolean quoted = quotations.stream().anyMatch(q -> editStart < q.end() && editEnd > q.start());
                if (!quoted) edits.add(new Edit(editStart, editEnd, replacement));
            }
        }
        edits.sort(Comparator.comparingInt(Edit::start).reversed());
        String result = text;
        for (Edit edit : edits) {
            long sameRange = edits.stream().filter(other -> other.start() == edit.start() && other.end() == edit.end()).count();
            boolean overlaps = edits.stream().anyMatch(other -> other != edit && other.start() < edit.end() && other.end() > edit.start());
            if (sameRange > 1 || overlaps) continue;
            result = result.substring(0, edit.start()) + edit.replacement() + result.substring(edit.end());
        }
        return result;
    }
}
