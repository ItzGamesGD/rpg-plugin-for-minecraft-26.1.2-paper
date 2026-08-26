package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Repels the actual exterior entrant along the inverse of their crossing direction. */
public final class PyramidRepelComponent implements ExplorationComponent {
    @Override public String type() { return "pyramid_repel"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.PYRAMID_GUARDIAN_SPAWN; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_repel requires desert_pyramid");
        }
        Player player = context.runtime().entryActor() == null ? null
                : org.bukkit.Bukkit.getPlayer(context.runtime().entryActor());
        if (player == null || !player.isOnline() || player.isDead()) {
            throw new IllegalStateException("pyramid entry actor is unavailable");
        }
        Vector away = new Vector(-context.runtime().entryDeltaX(), 0.0D, -context.runtime().entryDeltaZ());
        if (away.lengthSquared() < 0.0001D) {
            Location center = new Location(player.getWorld(), context.record().bounds().centerX(),
                    player.getLocation().getY(), context.record().bounds().centerZ());
            away = player.getLocation().toVector().subtract(center.toVector()).setY(0.0D);
        }
        if (away.lengthSquared() < 0.0001D) away = new Vector(0.0D, 0.0D, 1.0D);
        away.normalize().multiply(Math.max(0.35D, spec.decimal("horizontal-strength", 0.75D)));
        away.setY(Math.max(0.15D, spec.decimal("vertical-strength", 0.32D)));
        player.setVelocity(away);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_SAND_BREAK, 1.0F, 0.65F);
    }
}
