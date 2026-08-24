package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidBlockPosition;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomCandidate;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomPreflight;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomOrientation;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * First bounded Desert Pyramid runtime slice. It validates a room candidate
 * before spawning the configured guardian through the existing mob port.
 * No blocks are changed by this component.
 */
public final class PyramidGuardianComponent implements ExplorationComponent {
    @Override public String type() { return "pyramid_guardian"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_guardian requires desert_pyramid");
        }
        World world = context.world().orElseThrow(() -> new IllegalStateException("pyramid world is not loaded"));
        PyramidRoomCandidate candidate = findRoom(context, spec, world)
                .orElseThrow(() -> new IllegalStateException("no safe Desert Pyramid room candidate"));

        context.runtime().sequence().setFlag("pyramid.preflight.passed");
        Location spawnLocation = new Location(world,
                candidate.origin().x() + 0.5D,
                candidate.origin().y(),
                candidate.origin().z() + 0.5D);
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
                    + context.record().structureId() + ", room=" + candidate.slot()
                    + ", origin=" + candidate.origin() + ", mob=" + mobId
                    + ", objectives=" + validIds.size());
        }
    }

    private Optional<PyramidRoomCandidate> findRoom(ExplorationEventContext context,
                                                     ExplorationComponentSpec spec,
                                                     World world) {
        StructureBounds bounds = context.record().bounds();
        int radius = Math.max(0, Math.min(2, spec.integer("room-radius", 1)));
        int height = Math.max(1, Math.min(3, spec.integer("room-height", 2)));
        boolean rejectContainers = spec.bool("reject-containers", false);
        int centerX = (int) Math.floor(bounds.centerX());
        int centerZ = (int) Math.floor(bounds.centerZ());
        int minY = Math.max(world.getMinHeight() + 1, bounds.minY() + 1);
        int maxY = Math.min(world.getMaxHeight() - height - 1, bounds.maxY() - height);
        if (minY > maxY) return Optional.empty();

        List<PyramidRoomCandidate> candidates = new ArrayList<>();
        for (PyramidRoomCandidate.Slot slot : PyramidRoomCandidate.Slot.values()) {
            int x = centerX;
            int z = centerZ;
            switch (slot) {
                case NORTH -> z -= 4;
                case SOUTH -> z += 4;
                case EAST -> x += 4;
                case WEST -> x -= 4;
                case CENTER -> { }
            }
            for (int y = minY; y <= maxY; y++) {
                candidates.add(new PyramidRoomCandidate(slot,
                        new PyramidBlockPosition(x, y, z), PyramidRoomOrientation.NORTH));
            }
        }
        return PyramidRoomPreflight.firstUsable(candidates,
                candidate -> usable(world, bounds, candidate.origin(), radius, height, rejectContainers));
    }

    private boolean usable(World world, StructureBounds bounds, PyramidBlockPosition origin,
                           int radius, int height, boolean rejectContainers) {
        if (!bounds.contains(origin.x(), origin.y(), origin.z())) return false;
        if (!world.isChunkLoaded(origin.x() >> 4, origin.z() >> 4)) return false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    int x = origin.x() + dx;
                    int y = origin.y() + dy;
                    int z = origin.z() + dz;
                    if (!bounds.contains(x, y, z)) return false;
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isPassable() || block.isLiquid()
                            || protectedBlock(block.getState(), rejectContainers)) return false;
                }
                Block floor = world.getBlockAt(origin.x() + dx, origin.y() - 1, origin.z() + dz);
                if (!floor.getType().isSolid() || floor.isLiquid()) return false;
            }
        }
        return true;
    }

    private boolean protectedBlock(BlockState state, boolean rejectContainers) {
        return (rejectContainers && state instanceof InventoryHolder)
                || state.getType().name().contains("PORTAL")
                || state.getType().name().contains("SPAWNER");
    }
}
