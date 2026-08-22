package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spawns a configured objective and records only valid entity ids in the runtime.
 * An objective spawn that produced no usable entity is an activation failure, not a
 * completed objective.
 */
public final class ScriptedSpawnComponent implements ExplorationComponent {
    @Override public String type() { return "scripted_spawn"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String mobId = spec.string("mob-id", "");
        if (mobId.isBlank()) throw new IllegalArgumentException("scripted_spawn requires mob-id");

        Collection<UUID> spawned = context.ports().mobs().spawn(
                mobId,
                ComponentLocations.relative(context, spec),
                Math.max(1, spec.integer("count", 1)),
                spec.options());
        List<UUID> validIds = spawned == null
                ? List.of()
                : spawned.stream().filter(java.util.Objects::nonNull).toList();

        if (context.plugin() != null) {
            context.plugin().getLogger().info("Exploration scripted spawn: mob=" + mobId
                    + ", requested=" + Math.max(1, spec.integer("count", 1))
                    + ", spawned=" + validIds.size());
        }

        if (spec.bool("objective", false) && validIds.isEmpty()) {
            throw new IllegalStateException("scripted_spawn objective produced no valid entity");
        }

        validIds.forEach(context.runtime().tracker()::trackEntity);
        if (spec.bool("objective", false)) context.runtime().trackObjectives(validIds);
    }
}
