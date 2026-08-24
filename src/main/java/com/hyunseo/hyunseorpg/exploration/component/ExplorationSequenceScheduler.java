package com.hyunseo.hyunseorpg.exploration.component;

@FunctionalInterface
public interface ExplorationSequenceScheduler {
    boolean schedule(ExplorationEventContext context, String actionId,
                     long delayTicks, ExplorationComponentPhase nextPhase);
}
