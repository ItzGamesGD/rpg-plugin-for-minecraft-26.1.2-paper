package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Bukkit;

public final class ForcedRelocationComponent implements ExplorationComponent {
    @Override public String type() { return "forced_relocation"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }
    @Override public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        var destination = ComponentLocations.relative(context, spec);
        long exemption = Math.max(20L, spec.integer("teleport-exemption-ticks", 60));
        for (var playerId : context.runtime().participants()) {
            var player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;
            context.teleportExemptions().exempt(context.record().structureId(), playerId, context.currentTick(), exemption);
            context.ports().teleports().teleport(player, destination, spec.options());
        }
    }
}
