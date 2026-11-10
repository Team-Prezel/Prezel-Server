package com.finger.handoff.domain.presentation.prompt;

/** The existing application classification, not a universal ideal speaking rate. */
public final class SpeechSpeedPolicy {
    private SpeechSpeedPolicy() {
    }

    public static String evaluate(double spm) {
        if (!Double.isFinite(spm) || spm < 0) return null;
        if (spm <= 210) return "느려요";
        if (spm >= 260) return "빨라요";
        return "적당해요";
    }
}
