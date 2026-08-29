package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import java.util.Locale;
import java.util.Set;

/**
 * A single bounded gate between configured component phases. It is deliberately
 * not an expression language: only known runtime facts can be awaited.
 */
public final class SequenceWaitComponent implements ExplorationComponent {
    private static final Set<String> CONDITIONS = Set.of(
            "objectives-clear", "objective-count", "counter", "flag", "movement");

    @Override public String type() { return "sequence_wait"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String actionId = spec.string("action-id", "").trim();
        String condition = spec.string("wait-for", "").trim().toLowerCase(Locale.ROOT);
        String key = spec.string("key", "").trim();
        int threshold = Math.max(0, spec.integer("threshold", 0));
        String nextPhase = spec.string("next-phase", "").trim();
        if (actionId.isBlank() || nextPhase.isBlank() || !CONDITIONS.contains(condition)) {
            throw new IllegalArgumentException("sequence_wait requires action-id, supported wait-for, and next-phase");
        }
        if (("counter".equals(condition) || "flag".equals(condition)) && key.isBlank()) {
            throw new IllegalArgumentException("sequence_wait " + condition + " requires key");
        }
        if (!context.runtime().sequence().armWait(actionId, condition, key, threshold, nextPhase)) {
            throw new IllegalStateException("sequence_wait rejected duplicate or concurrent action " + actionId);
        }
    }
}
