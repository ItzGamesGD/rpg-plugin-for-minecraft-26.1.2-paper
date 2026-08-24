package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Spawns the guardian outside the generated room after the loot-trigger delay.
 */
public final class PyramidGuardianComponent implements ExplorationComponent {
    private final PyramidRoomService rooms;

    public PyramidGuardianComponent(PyramidRoomService rooms) { this.rooms = rooms; }

    @Override public String type() { return "pyramid_guardian"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.PYRAMID_GUARDIAN_SPAWN; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_guardian requires desert_pyramid");
        }
        PlayerSpawn playerSpawn = playerSpawn(context);
        Location spawnLocation = rooms.guardianSpawn(context, playerSpawn.player());

        context.runtime().sequence().setFlag("pyramid.guardian.spawned.outside");
        Map<String, Object> options = new LinkedHashMap<>(spec.options());
        options.putIfAbsent("glowing", true);
        options.putIfAbsent("invulnerable", false);
        if (!options.containsKey("target-player-uuid")) {
            context.runtime().participants().stream().findFirst()
                    .ifPresent(playerId -> options.put("target-player-uuid", playerId.toString()));
        }

        String mobId = spec.string("mob-id", "custom:stone_armored_zombie");
        int count = Math.max(1, spec.integer("count", 1));
        Collection<UUID> spawned = context.ports().mobs().spawn(mobId, spawnLocation, count, options);
        List<UUID> validIds = spawned == null
                ? List.of()
                : spawned.stream().filter(java.util.Objects::nonNull).toList();
        if (validIds.isEmpty()) {
            throw new IllegalStateException("pyramid guardian spawn produced no valid entity");
        }

        validIds.forEach(context.runtime().tracker()::trackEntity);
        if (spec.bool("objective", true)) context.runtime().trackObjectives(validIds);
        if (context.plugin() != null) {
            context.plugin().getLogger().info("Desert Pyramid guardian activated: structure="
                    + context.record().structureId() + ", spawn=" + spawnLocation.getBlockX() + ","
                    + spawnLocation.getBlockY() + "," + spawnLocation.getBlockZ() + ", mob=" + mobId
                    + ", objectives=" + validIds.size());
        }
    }

    private PlayerSpawn playerSpawn(ExplorationEventContext context) {
        UUID playerId = context.runtime().looter();
        if (playerId == null) playerId = context.runtime().participants().stream().findFirst().orElse(null);
        if (playerId == null) throw new IllegalStateException("pyramid guardian has no target player");
        Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline() || player.isDead()) {
            throw new IllegalStateException("pyramid guardian target player is unavailable");
        }
        return new PlayerSpawn(player);
    }

    private record PlayerSpawn(Player player) { }
}
