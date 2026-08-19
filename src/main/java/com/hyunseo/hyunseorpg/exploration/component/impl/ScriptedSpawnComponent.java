package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

public final class ScriptedSpawnComponent implements ExplorationComponent {
    @Override public String type() { return "scripted_spawn"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String mobId = spec.string("mob-id", "");
        if (mobId.isBlank()) throw new IllegalArgumentException("scripted_spawn requires mob-id");
        var ids = context.ports().mobs().spawn(mobId, ComponentLocations.relative(context, spec),
                Math.max(1, spec.integer("count", 1)), spec.options());
        ids.forEach(context.runtime().tracker()::trackEntity);
        if (spec.bool("objective", false)) context.runtime().trackObjectives(ids);
    }
}
