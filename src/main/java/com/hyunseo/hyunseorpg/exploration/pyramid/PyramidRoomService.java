package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Pyramid-only underground room planner, builder and lifecycle owner. */
public final class PyramidRoomService {
    private final JavaPlugin plugin;
    private final StructureRepository repository;
    private final Map<UUID, RoomSession> sessions = new LinkedHashMap<>();

    public PyramidRoomService(JavaPlugin plugin, StructureRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public synchronized PyramidRoomCandidate create(ExplorationEventContext context,
                                                     ExplorationComponentSpec spec) throws IOException {
        UUID structureId = context.runtime().structureId();
        RoomSession existing = sessions.get(structureId);
        if (existing != null) return existing.candidate();

        World world = context.world().orElseThrow(() -> new IllegalStateException("pyramid world is not loaded"));
        StructureBounds bounds = context.record().bounds();
        int radius = clamp(spec.integer("room-radius", 3), 2, 5);
        int height = clamp(spec.integer("room-height", 4), 3, 6);
        int shell = clamp(spec.integer("safety-shell", 2), 1, 3);
        PyramidRoomCandidate candidate = readPersistedCandidate(context, world).orElseGet(() ->
                Optional.ofNullable(findBuriedCandidate(world, bounds, radius, height, shell))
                        .orElseThrow(() -> new IllegalStateException("no safe buried Desert Pyramid room candidate")));

        if (context.record().activationMetadata().containsKey("pyramid-room-created")) {
            RoomSession restored = new RoomSession(context.runtime(), world, candidate, radius, height, shell,
                    List.of(), true);
            sessions.put(structureId, restored);
            context.runtime().sequence().setFlag("pyramid.room.created");
            return candidate;
        }

        List<BlockSnapshot> snapshots = snapshot(world, candidate.origin(), radius, height);
        try {
            carve(world, candidate.origin(), radius, height);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("pyramid-room-created", "true");
            metadata.put("pyramid-room-origin", encode(candidate.origin()));
            metadata.put("pyramid-room-radius", Integer.toString(radius));
            metadata.put("pyramid-room-height", Integer.toString(height));
            metadata.put("pyramid-room-created-at", Instant.now().toString());
            repository.save(withMetadata(context, metadata));
        } catch (Exception exception) {
            restore(world, snapshots);
            if (exception instanceof IOException io) throw io;
            throw new IllegalStateException("unable to create Desert Pyramid room", exception);
        }
        RoomSession session = new RoomSession(context.runtime(), world, candidate, radius, height, shell,
                snapshots, false);
        sessions.put(structureId, session);
        context.runtime().sequence().setFlag("pyramid.room.created");
        plugin.getLogger().info("Desert Pyramid underground room created: structure=" + structureId
                + ", origin=" + encode(candidate.origin()) + ", radius=" + radius + ", height=" + height);
        return candidate;
    }

    public synchronized Optional<PyramidRoomCandidate> room(UUID structureId) {
        return Optional.ofNullable(sessions.get(structureId)).map(RoomSession::candidate);
    }

    public synchronized Location guardianSpawn(ExplorationEventContext context, Player player) {
        RoomSession session = sessions.get(context.runtime().structureId());
        if (session == null || player == null) throw new IllegalStateException("pyramid room is not active");
        Location playerLocation = player.getLocation().clone();
        Vector away = playerLocation.toVector().subtract(session.location(session.candidate.origin()).toVector());
        away.setY(0.0D);
        if (away.lengthSquared() < 0.01D) away = playerLocation.getDirection().setY(0.0D);
        if (away.lengthSquared() < 0.01D) away = new Vector(0.0D, 0.0D, 1.0D);
        away.normalize();
        for (int distance = 3; distance <= 10; distance++) {
            Location candidate = playerLocation.clone().add(away.clone().multiply(distance));
            candidate.setY(playerLocation.getY());
            if (session.safeEntityLocation(candidate, context.record().bounds())) return candidate;
        }
        return playerLocation.add(away.multiply(3.0D));
    }

    public synchronized void cleanup(UUID structureId) {
        RoomSession session = sessions.remove(structureId);
        if (session == null || session.runtime.sequence().flag("pyramid.puzzle.solved") || session.persisted) return;
        restore(session.world, session.snapshots);
        plugin.getLogger().info("Desert Pyramid underground room rolled back: structure=" + structureId);
    }

    public synchronized void stopAll() {
        for (UUID structureId : List.copyOf(sessions.keySet())) cleanup(structureId);
    }

    private PyramidRoomCandidate findBuriedCandidate(World world, StructureBounds bounds,
                                                      int radius, int height, int shell) {
        int centerX = (int) Math.floor(bounds.centerX());
        int centerZ = (int) Math.floor(bounds.centerZ());
        int highest = bounds.minY() - 4;
        int lowest = Math.max(world.getMinHeight() + shell + 2, highest - 32);
        for (PyramidRoomCandidate.Slot slot : PyramidRoomCandidate.Slot.values()) {
            int x = centerX;
            int z = centerZ;
            switch (slot) {
                case NORTH -> z -= 6;
                case SOUTH -> z += 6;
                case EAST -> x += 6;
                case WEST -> x -= 6;
                case CENTER -> { }
            }
            for (int y = highest; y >= lowest; y--) {
                PyramidBlockPosition origin = new PyramidBlockPosition(x, y, z);
                if (buried(world, origin, radius, height, shell)) {
                    return new PyramidRoomCandidate(slot, origin, PyramidRoomOrientation.NORTH);
                }
            }
        }
        return null;
    }

    private boolean buried(World world, PyramidBlockPosition origin, int radius, int height, int shell) {
        int outer = radius + shell;
        int minY = origin.y() - 1 - shell;
        int maxY = origin.y() + height + shell;
        if (!world.isChunkLoaded(origin.x() >> 4, origin.z() >> 4)) return false;
        for (int x = origin.x() - outer; x <= origin.x() + outer; x++) {
            for (int z = origin.z() - outer; z <= origin.z() + outer; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isSolid() || block.isLiquid() || protectedBlock(block.getState())) return false;
                }
            }
        }
        return true;
    }

    private List<BlockSnapshot> snapshot(World world, PyramidBlockPosition origin, int radius, int height) {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        for (int x = origin.x() - radius; x <= origin.x() + radius; x++) {
            for (int z = origin.z() - radius; z <= origin.z() + radius; z++) {
                for (int y = origin.y() - 1; y <= origin.y() + height; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    snapshots.add(new BlockSnapshot(x, y, z, block.getBlockData().clone()));
                }
            }
        }
        return List.copyOf(snapshots);
    }

    private void carve(World world, PyramidBlockPosition origin, int radius, int height) {
        Material wall = Material.SANDSTONE;
        Material trim = Material.CHISELED_SANDSTONE;
        for (int x = origin.x() - radius; x <= origin.x() + radius; x++) {
            for (int z = origin.z() - radius; z <= origin.z() + radius; z++) {
                boolean edge = Math.abs(x - origin.x()) == radius || Math.abs(z - origin.z()) == radius;
                for (int y = origin.y(); y < origin.y() + height; y++) {
                    world.getBlockAt(x, y, z).setType(edge ? wall : Material.AIR, false);
                }
                world.getBlockAt(x, origin.y() - 1, z).setType(wall, false);
                world.getBlockAt(x, origin.y() + height, z).setType(trim, false);
            }
        }
    }

    private void restore(World world, List<BlockSnapshot> snapshots) {
        for (BlockSnapshot snapshot : snapshots) {
            world.getBlockAt(snapshot.x(), snapshot.y(), snapshot.z()).setBlockData(snapshot.data(), false);
        }
    }

    private boolean protectedBlock(BlockState state) {
        return state instanceof org.bukkit.inventory.InventoryHolder
                || state.getType().name().contains("SPAWNER")
                || state.getType().name().contains("PORTAL");
    }

    private StructureRecord withMetadata(ExplorationEventContext context, Map<String, String> values) {
        var record = context.record();
        for (var entry : values.entrySet()) record = record.withMetadata(entry.getKey(), entry.getValue());
        return record;
    }

    private Optional<PyramidRoomCandidate> readPersistedCandidate(ExplorationEventContext context, World world) {
        String encoded = context.record().activationMetadata().get("pyramid-room-origin");
        if (encoded == null) return Optional.empty();
        String[] parts = encoded.split(",");
        if (parts.length != 3) return Optional.empty();
        try {
            return Optional.of(new PyramidRoomCandidate(PyramidRoomCandidate.Slot.CENTER,
                    new PyramidBlockPosition(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2])), PyramidRoomOrientation.NORTH));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private String encode(PyramidBlockPosition position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private record BlockSnapshot(int x, int y, int z, org.bukkit.block.data.BlockData data) { }

    private static final class RoomSession {
        private final com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime;
        private final World world;
        private final PyramidRoomCandidate candidate;
        private final int radius;
        private final int height;
        private final int shell;
        private final List<BlockSnapshot> snapshots;
        private final boolean persisted;

        private RoomSession(com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime, World world,
                            PyramidRoomCandidate candidate, int radius, int height, int shell,
                            List<BlockSnapshot> snapshots, boolean persisted) {
            this.runtime = runtime;
            this.world = world;
            this.candidate = candidate;
            this.radius = radius;
            this.height = height;
            this.shell = shell;
            this.snapshots = snapshots;
            this.persisted = persisted;
        }

        private PyramidRoomCandidate candidate() { return candidate; }
        private Location location(PyramidBlockPosition position) {
            return new Location(world, position.x() + 0.5D, position.y(), position.z() + 0.5D);
        }
        private boolean safeEntityLocation(Location location, StructureBounds bounds) {
            if (bounds.contains(location.getX(), location.getY(), location.getZ())) return false;
            Block feet = location.getBlock();
            Block head = world.getBlockAt(feet.getX(), feet.getY() + 1, feet.getZ());
            Block floor = world.getBlockAt(feet.getX(), feet.getY() - 1, feet.getZ());
            return feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid()
                    && floor.getType().isSolid();
        }
    }
}
