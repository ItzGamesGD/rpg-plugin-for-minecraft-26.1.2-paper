package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import java.util.Locale;

/** Mutates only the two permitted sequence values: named flags and integer counters. */
public final class SequenceStateComponent implements ExplorationComponent {
    @Override public String type() { return "sequence_state"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String operation = spec.string("operation", "").trim().toLowerCase(Locale.ROOT);
        String key = spec.string("key", "").trim();
        if (key.isBlank()) throw new IllegalArgumentException("sequence_state requires key");
        switch (operation) {
            case "set-flag" -> context.runtime().sequence().setFlag(key);
            case "clear-flag" -> context.runtime().sequence().clearFlag(key);
            case "increment-counter" -> context.runtime().sequence().incrementCounter(key);
            default -> throw new IllegalArgumentException("unsupported sequence_state operation: " + operation);
        }
    }
}
