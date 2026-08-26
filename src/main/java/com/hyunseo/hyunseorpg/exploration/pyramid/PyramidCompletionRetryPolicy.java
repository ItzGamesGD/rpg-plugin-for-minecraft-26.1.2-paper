package com.hyunseo.hyunseorpg.exploration.pyramid;

/** Bounded deterministic retry policy for durable underground completion writes. */
public final class PyramidCompletionRetryPolicy {
    public static final int MAX_ATTEMPTS = 5;
    private PyramidCompletionRetryPolicy() { }
    public static boolean shouldRetry(int attempt) { return attempt > 0 && attempt <= MAX_ATTEMPTS; }
    public static long delayTicks(int attempt) {
        if (!shouldRetry(attempt)) return -1L;
        return 40L * attempt;
    }
}
