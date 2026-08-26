package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Spawns the exterior guardian from structure bounds and the real entry actor. */
public final class PyramidGuardianComponent implements ExplorationComponent {
    @Override public String type() { return "pyramid_guardian"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.PYRAMID_GUARDIAN_SPAWN; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_guardian requires desert_pyramid");
        }
        UUID actor = context.runtime().entryActor();
        Player player = actor == null ? null : org.bukkit.Bukkit.getPlayer(actor);
        if (player == null || !player.isOnline() || player.isDead()) {
            throw new IllegalStateException("pyramid guardian entry actor is unavailable");
        }
        World world = player.getWorld();
        Location spawn = safeSurfaceSpawn(world, player.getLocation(), context);
        context.runtime().sequence().setFlag("pyramid.guardian.spawned");

        Map<String, Object> options = new LinkedHashMap<>(spec.options());
        options.putIfAbsent("glowing", true);
        options.putIfAbsent("invulnerable", false);
        options.put("target-player-uuid", player.getUniqueId().toString());

        String mobId = spec.string("mob-id", "custom:stone_armored_zombie");
        int count = Math.max(1, spec.integer("count", 1));
        Collection<UUID> spawned = context.ports().mobs().spawn(mobId, spawn, count, options);
        List<UUID> ids = spawned == null ? List.of() : spawned.stream().filter(java.util.Objects::nonNull).toList();
        if (ids.isEmpty()) throw new IllegalStateException("pyramid guardian spawn produced no valid entity");
        ids.forEach(context.runtime().tracker()::trackEntity);
        if (spec.bool("objective", true)) context.runtime().trackObjectives(ids);
        context.runtime().setRaidTarget(player.getUniqueId());
        context.runtime().snapshotRaidOrigin(player.getLocation());
        context.plugin().getLogger().info("Desert Pyramid guardian activated: structure="
                + context.record().structureId() + ", target=" + player.getUniqueId()
                + ", spawn=" + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ()
                + ", objectives=" + ids.size());
    }

    private Location safeSurfaceSpawn(World world, Location near, ExplorationEventContext context) {
        int baseX = near.getBlockX(), baseZ = near.getBlockZ();
        for (int radius = 3; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int x = baseX + dx, z = baseZ + dz;
                    int y = world.getHighestBlockYAt(x, z) + 1;
                    Location candidate = new Location(world, x + 0.5D, y, z + 0.5D);
                    if (candidate.getBlock().isPassable() && world.getBlockAt(x, y - 1, z).getType().isSolid()
                            && !context.record().bounds().contains(candidate.getX(), candidate.getY(), candidate.getZ())) {
                        return candidate;
                    }
                }
            }
        }
        throw new IllegalStateException("no safe exterior Pyramid guardian surface spawn");
    }
}
