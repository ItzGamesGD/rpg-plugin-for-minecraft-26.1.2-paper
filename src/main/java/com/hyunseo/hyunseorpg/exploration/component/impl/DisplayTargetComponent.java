package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

public final class DisplayTargetComponent implements ExplorationComponent {
    @Override public String type() { return "display_target"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }
    @Override public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        var id = context.ports().displays().spawn(spec.string("kind", "block"), ComponentLocations.relative(context, spec), spec.options());
        context.runtime().tracker().trackEntity(id);
    }
}
