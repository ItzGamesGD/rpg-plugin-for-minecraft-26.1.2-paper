package com.hyunseo.hyunseorpg.exploration.component;

/**
 * Schedules one runtime-owned transition. The phase identifier is deliberately
 * a bounded configured name, not an expression or command language.
 */
@FunctionalInterface
public interface ExplorationSequenceScheduler {
    boolean schedule(ExplorationEventContext context, String actionId,
                     long delayTicks, String nextPhase);
}
