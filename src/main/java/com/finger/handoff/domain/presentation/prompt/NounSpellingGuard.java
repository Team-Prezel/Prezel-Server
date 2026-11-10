package com.finger.handoff.domain.presentation.prompt;

import com.fasterxml.jackson.databind.node.ArrayNode;

/** Reject the price noun becoming the temporal adverb before an unambiguous noun particle. */
public final class NounSpellingGuard {
    private NounSpellingGuard() { }

    public static ArrayNode filter(ArrayNode corrections, String source) {
        ArrayNode result = corrections.arrayNode();
        for (var correction : corrections) {
            String original = correction.path("originalText").asText();
            String corrected = correction.path("correctedText").asText();
            boolean reject = false;
            if (source != null && original.contains("금새") && original.replace("금새", "금세").equals(corrected)) {
                int start = correction.path("startIndex").asInt(-1);
                int end = correction.path("endIndex").asInt(-1);
                if (start >= 0 && end <= source.length() && end > start && source.substring(start, end).equals(original)) {
                    for (int index = original.indexOf("금새"); index >= 0; index = original.indexOf("금새", index + 2)) {
                        int following = start + index + 2;
                        if (following < source.length() && "가를의".indexOf(source.charAt(following)) >= 0) reject = true;
                    }
                }
            }
            if (!reject) result.add(correction.deepCopy());
        }
        return result;
    }
}
