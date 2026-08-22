package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

public final class TemporarySealComponent implements ExplorationComponent {
    @Override public String type() { return "temporary_seal"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }
    @Override public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        Runnable restore = context.ports().worldMutations().applyTemporary(ComponentLocations.relative(context, spec), spec.options());
        context.runtime().tracker().track(restore);
    }
}
