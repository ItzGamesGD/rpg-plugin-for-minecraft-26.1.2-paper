package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.raid.RaidMobDefinition;
import com.hyunseo.hyunseorpg.exploration.raid.RaidWavePlanner;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationRegistry;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Spawns one YAML-selected, bounded raid pool after a player has chosen a tier. */
public final class RaidWaveSpawnComponent implements ExplorationComponent {
    private final ExplorationRegistry registry;
    private final RaidWavePlanner planner;

    public RaidWaveSpawnComponent(ExplorationRegistry registry) {
        this(registry, new RaidWavePlanner());
    }

    RaidWaveSpawnComponent(ExplorationRegistry registry, RaidWavePlanner planner) {
        this.registry = registry;
        this.planner = planner;
    }

    @Override public String type() { return "raid_wave_spawn"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.CHOICE_TIER_1; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        if (context.runtime().objectiveMode()) {
            throw new IllegalStateException("raid wave cannot be spawned twice");
        }
        String poolId = spec.string("pool-id", "");
        var pool = registry.raidPool(poolId)
                .orElseThrow(() -> new IllegalArgumentException("unknown raid pool: " + poolId));
        List<RaidMobDefinition> units = planner.plan(pool, ThreadLocalRandom.current());
        if (units.isEmpty()) throw new IllegalStateException("raid pool selected no units: " + poolId);

        List<UUID> objectives = new ArrayList<>();
        int index = 0;
        for (RaidMobDefinition unit : units) {
            double angle = (Math.PI * 2.0D * index) / Math.max(1, units.size());
            double radius = 2.5D + (index % 2) * 1.5D;
            Location location = ComponentLocations.raidOrigin(context, spec, angle, 12.0D + (index % 3) * 4.0D);
            HashMap<String, Object> options = new HashMap<>(spec.options());
            options.put("level", unit.level());
            Collection<UUID> spawned = context.ports().mobs().spawn("custom:" + unit.mobId(), location, 1, options);
            List<UUID> valid = spawned == null ? List.of() : spawned.stream().filter(java.util.Objects::nonNull).toList();
            if (valid.isEmpty()) throw new IllegalStateException("raid unit spawn failed: " + unit.mobId());
            valid.forEach(context.runtime().tracker()::trackEntity);
            // The ravager is spawned first; its rider is tracked for cleanup but does not duplicate the objective unit.
            if (com.hyunseo.hyunseorpg.mob.OutpostRaiderPolicy.isMountedHeavyUnit(unit.mobId())) objectives.add(valid.get(0));
            else objectives.addAll(valid);
            index++;
        }
        if (objectives.isEmpty()) throw new IllegalStateException("raid wave produced no objectives");
        context.runtime().trackObjectives(objectives);
        context.plugin().getLogger().info("Exploration raid wave spawned: structure="
                + context.record().structureId() + ", pool=" + pool.id() + ", units=" + units.size()
                + ", objectives=" + objectives.size());
    }
}
