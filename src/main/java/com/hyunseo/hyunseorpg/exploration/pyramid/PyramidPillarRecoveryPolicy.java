package com.hyunseo.hyunseorpg.exploration.pyramid;

/** Pure policy for retrying an already-committed Pyramid room's pillar runtime only. */
public final class PyramidPillarRecoveryPolicy {
    public static final String RETRY_PHASE = "pyramid_pillar_restore";

    private PyramidPillarRecoveryPolicy() { }

    public static Decision afterFailure(int attempt) {
        if (!PyramidCompletionRetryPolicy.shouldRetry(attempt)) {
            return new Decision(false, true, -1L, RETRY_PHASE);
        }
        return new Decision(true, false, PyramidCompletionRetryPolicy.delayTicks(attempt), RETRY_PHASE);
    }

    public record Decision(boolean retry, boolean exhausted, long delayTicks, String phase) { }
}
