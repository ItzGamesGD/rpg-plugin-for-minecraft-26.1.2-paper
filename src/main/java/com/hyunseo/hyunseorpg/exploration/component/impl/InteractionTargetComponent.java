package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

public final class InteractionTargetComponent implements ExplorationComponent {
    @Override public String type() { return "interaction_target"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }
    @Override public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String interactionId = spec.string("interaction-id", context.record().variantId());
        var id = context.ports().interactions().spawn(interactionId, ComponentLocations.relative(context, spec), spec.options());
        context.runtime().tracker().trackEntity(id);
    }
}
