package com.hyunseo.hyunseorpg.special.moonlit;

import java.util.ArrayList;
import java.util.List;

/** Small deterministic state machine; runtime entity references are owned by the listener. */
public final class MoonShadowState {
    public enum Phase { RAPID_TELEPORT_SEQUENCE, WAITING_FOR_FINAL_TRIGGER, RELEASING, FINISHED }
    public record SlashSnapshot(MoonlitAfterglowMath.Point origin,
                                MoonlitAfterglowMath.Point target,
                                MoonlitAfterglowMath.Point direction) { }

    private final int expectedAttempts;
    private final List<SlashSnapshot> slashes = new ArrayList<>();
    private Phase phase = Phase.RAPID_TELEPORT_SEQUENCE;
    private int attempts;

    public MoonShadowState(int expectedAttempts) {
        if (expectedAttempts < 1) throw new IllegalArgumentException("expectedAttempts must be positive");
        this.expectedAttempts = expectedAttempts;
    }
    public Phase phase() { return phase; }
    public int attempts() { return attempts; }
    public List<SlashSnapshot> slashes() { return List.copyOf(slashes); }
    public boolean requiresLiveTarget() { return phase == Phase.RAPID_TELEPORT_SEQUENCE; }
    public boolean attempt(SlashSnapshot successfulMove) {
        if (phase != Phase.RAPID_TELEPORT_SEQUENCE || attempts >= expectedAttempts) return false;
        attempts++;
        if (successfulMove != null) slashes.add(successfulMove);
        if (attempts == expectedAttempts) phase = Phase.WAITING_FOR_FINAL_TRIGGER;
        return true;
    }
    public List<SlashSnapshot> trigger() {
        if (phase != Phase.WAITING_FOR_FINAL_TRIGGER) return List.of();
        phase = Phase.RELEASING;
        return slashes();
    }
    public void finish() { phase = Phase.FINISHED; }
}
