package com.hyunseo.hyunseorpg.alchemy.catalyst;

/** Pure timing/state transitions used by bounded catalyst runtimes and their contract tests. */
public final class CatalystRuntimePhase {
    private CatalystRuntimePhase() { }

    public enum Shock { FREE, ROOT_20T }
    public enum Slime { BOUNCE, FINAL_SPLASH, FINISHED }

    public static Shock shockAt(long elapsedTicks, long pulseInterval, long rootDuration) {
        if (elapsedTicks < pulseInterval) return Shock.FREE;
        long sincePulse = (elapsedTicks - pulseInterval) % Math.max(1L, pulseInterval);
        return sincePulse < Math.max(1L, rootDuration) ? Shock.ROOT_20T : Shock.FREE;
    }

    public static Slime slimeAfterCollision(Slime current, int bounceCount, int maxBounceCount) {
        if (current == Slime.FINAL_SPLASH) return Slime.FINISHED;
        if (current == Slime.FINISHED) return Slime.FINISHED;
        return bounceCount >= Math.max(1, maxBounceCount) ? Slime.FINAL_SPLASH : Slime.BOUNCE;
    }

    public static boolean sculkPropagationDue(long currentTick, long nextPropagationTick) {
        return currentTick >= nextPropagationTick;
    }
}
