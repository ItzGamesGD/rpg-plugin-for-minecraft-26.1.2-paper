package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

/** Schedules one later phase through the owning structure runtime only. */
public final class SequenceDelayComponent implements ExplorationComponent {
    @Override public String type() { return "sequence_delay"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String actionId = spec.string("action-id", "").trim();
        long delayTicks = Math.max(0L, spec.integer("delay-ticks", 0));
        String nextPhase = spec.string("next-phase", "").trim();
        if (actionId.isBlank() || nextPhase.isBlank()) {
            throw new IllegalArgumentException("sequence_delay requires action-id and next-phase");
        }
        if (!context.sequenceScheduler().schedule(context, actionId, delayTicks, nextPhase)) {
            throw new IllegalStateException("sequence_delay rejected duplicate action " + actionId);
        }
    }
}
