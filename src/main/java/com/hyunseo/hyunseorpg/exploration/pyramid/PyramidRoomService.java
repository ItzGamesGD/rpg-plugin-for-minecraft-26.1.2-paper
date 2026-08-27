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
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Pyramid-only underground room planner, builder and lifecycle owner. */
public final class PyramidRoomService {
    private final JavaPlugin plugin;
    private final StructureRepository repository;
    private final Map<UUID, RoomSession> sessions = new LinkedHashMap<>();
    private final Map<UUID, PendingReveal> pendingReveals = new LinkedHashMap<>();
    private Consumer<ExplorationEventContext> revealCompletion = context -> { };

    public PyramidRoomService(JavaPlugin plugin, StructureRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /** Registers the explicit happy-path continuation into the pillar component. */
    public synchronized void setRevealCompletion(Consumer<ExplorationEventContext> continuation) {
        this.revealCompletion = continuation == null ? context -> { } : continuation;
    }

    /**
     * Resolves and persists a safe candidate without changing any world block.
     * Loot handling calls this before the delayed reveal so a later failure
     * cannot leave a half-carved room behind.
     */
    public synchronized PyramidRoomCandidate prepare(ExplorationEventContext context,
                                                      ExplorationComponentSpec spec) throws IOException {
        UUID structureId = context.runtime().structureId();
        RoomSession existing = sessions.get(structureId);
        if (existing != null) return existing.candidate();

        World world = context.world().orElseThrow(() -> new IllegalStateException("pyramid world is not loaded"));
        StructureBounds bounds = context.record().bounds();
        PyramidTreasureCenterPolicy.Center treasureCenter = PyramidTreasureCenterPolicy.from(bounds);
        StructureRecord effectiveRecord = context.record();
        if (Boolean.parseBoolean(effectiveRecord.activationMetadata()
                .getOrDefault("pyramid-reveal-in-progress", "false"))) {
            throw new IllegalStateException("Pyramid reveal is marked recovery-required");
        }
        int radius = clamp(persistedInt(effectiveRecord, "pyramid-room-radius", spec.integer("room-radius", 4)), 2, 5);
        int height = clamp(persistedInt(effectiveRecord, "pyramid-room-height", spec.integer("room-height", 4)), 3, 6);
        int shell = clamp(spec.integer("safety-shell", 2), 1, 3);
        boolean createdMetadata = Boolean.parseBoolean(effectiveRecord.activationMetadata()
                .getOrDefault("pyramid-room-created", "false"));
        Optional<PyramidRoomCandidate> persisted = readPersistedCandidate(effectiveRecord);
        if (createdMetadata && persisted.isPresent()
                && physicalRoomValid(world, persisted.get().origin(), radius, height)) {
            PyramidRoomCandidate candidate = persisted.get();
            RoomSession restored = new RoomSession(context.runtime(), world, candidate, radius, height, shell,
                    List.of(), true, false);
            sessions.put(structureId, restored);
            context.runtime().sequence().setFlag("pyramid.room.created");
            return candidate;
        }
        if (createdMetadata) {
            // A committed-room signature mismatch is external corruption. Keep
            // the durable evidence for diagnostics and freeze progression; never
            // silently discard metadata and re-carve a different room.
            repository.save(effectiveRecord
                    .withMetadata("pyramid-failure-state", "RECOVERY_REQUIRED")
                    .withMetadata("pyramid-failure-reason", "committed-room-signature-mismatch"));
            throw new IllegalStateException("Pyramid committed room is corrupted; administrative reset required");
        }
        final StructureRecord preparedRecord = effectiveRecord;
        // Staged carving is presentation-only; no per-layer durable progress is
        // trusted or replayed after a reload. A prepared room must still be intact.
        PyramidRoomCandidate candidate = readPersistedCandidate(preparedRecord).filter(value ->
                buried(world, value.origin(), radius, height, shell)).orElseGet(() ->
                Optional.ofNullable(findBuriedCandidate(world, bounds, radius, height, shell,
                        persistedInt(preparedRecord, "pyramid-treasure-center-x", treasureCenter.x()),
                        persistedInt(preparedRecord, "pyramid-treasure-center-z", treasureCenter.z())))
                        .orElseThrow(() -> new IllegalStateException("no safe buried Desert Pyramid room candidate")));

        if (!shaftSafe(world, candidate.origin(), bounds)) {
            throw new IllegalStateException("no safe Desert Pyramid access shaft");
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("pyramid-room-prepared", "true");
        // Trigger chest establishes identity; the direct shaft is always the treasure-chamber centre.
        metadata.put("pyramid-treasure-center-x", Integer.toString(treasureCenter.x()));
        metadata.put("pyramid-treasure-center-z", Integer.toString(treasureCenter.z()));
        metadata.put("pyramid-room-origin", encode(candidate.origin()));
        metadata.put("pyramid-room-radius", Integer.toString(radius));
        metadata.put("pyramid-room-height", Integer.toString(height));
        repository.save(withMetadata(preparedRecord, metadata));
        RoomSession session = new RoomSession(context.runtime(), world, candidate, radius, height, shell,
                List.of(), false, true);
        sessions.put(structureId, session);
        context.runtime().sequence().setFlag("pyramid.room.prepared");
        plugin.getLogger().info("Desert Pyramid underground room prepared: structure=" + structureId
                + ", origin=" + encode(candidate.origin()) + ", reveal=delayed");
        return candidate;
    }

    /** Performs the previously preflighted block mutation as a bounded staged reveal. */
    public synchronized PyramidRoomCandidate reveal(ExplorationEventContext context,
                                                     ExplorationComponentSpec spec) throws IOException {
        UUID structureId = context.runtime().structureId();
        RoomSession existing = sessions.get(structureId);
        if (existing != null && existing.persisted) return existing.candidate();
        PendingReveal already = pendingReveals.get(structureId);
        if (already != null) return already.candidate;
        if (existing == null) {
            prepare(context, spec);
            existing = sessions.get(structureId);
        }
        if (existing == null) throw new IllegalStateException("pyramid room preparation is unavailable");
        World world = existing.world;
        PyramidBlockPosition origin = existing.candidate.origin();
        StructureRecord currentRecord = repository.get(structureId).orElse(context.record());
        if (!shaftSafe(world, origin, currentRecord.bounds())
                || !buried(world, origin, existing.radius, existing.height, existing.shell)) {
            plugin.getLogger().warning("Desert Pyramid reveal refused after final validation: structure="
                    + structureId + ", origin=" + encode(origin));
            throw new IllegalStateException("Pyramid final reveal validation failed; retryable");
        }
        List<BlockSnapshot> snapshots = snapshot(world, origin, existing.radius, existing.height,
                currentRecord.bounds());
        // Mark presentation as in-flight before world mutation. A reload in this
        // window is handled as failed-closed rather than guessing partial progress.
        StructureRecord revealRecord = currentRecord.withMetadata("pyramid-reveal-in-progress", "true");
        repository.save(revealRecord);
        PendingReveal pending = new PendingReveal(context, revealRecord, spec, existing.candidate, existing.radius,
                existing.height, existing.shell, snapshots, currentRecord.bounds().minY() - 1);
        pendingReveals.put(structureId, pending);
        context.runtime().sequence().setFlag("pyramid.room.reveal.in_progress");
        scheduleRevealLayer(pending, 0);
        plugin.getLogger().info("Desert Pyramid staged reveal armed: structure=" + structureId
                + ", layers=3x3, interval=" + Math.max(2, spec.integer("reveal-layer-interval-ticks", 3)) + " ticks");
        return pending.candidate;
    }

    private void scheduleRevealLayer(PendingReveal pending, int index) {
        long interval = Math.max(2L, pending.spec.integer("reveal-layer-interval-ticks", 3));
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (PyramidRoomService.this) {
                UUID structureId = pending.context.runtime().structureId();
                if (!pendingReveals.containsKey(structureId) || pending.context.runtime().tracker().isClosed()) return;
                try {
                    int y = pending.startY - index;
                    int endY = pending.candidate.origin().y() + pending.height;
                    if (y >= endY) {
                        if (index == 0) nudgePlayersFromOpening(pending.world, pending.candidate.origin(), y);
                        carveShaftLayer(pending.world, pending.candidate.origin(), y);
                        pending.context.world().ifPresent(world -> world.playSound(
                                new Location(world, pending.candidate.origin().x() + 0.5D, y,
                                        pending.candidate.origin().z() + 0.5D),
                                org.bukkit.Sound.BLOCK_SAND_BREAK, 0.65F, 0.7F));
                        scheduleRevealLayer(pending, index + 1);
                        return;
                    }
                    carveRoom(pending.world, pending.candidate.origin(), pending.radius, pending.height);
                    // Merge underground-owned fields into the latest record so a
                    // concurrent guardian update cannot be overwritten.
                    StructureRecord latest = repository.get(structureId).orElse(pending.record);
                    StructureRecord record = withMetadata(latest, Map.of(
                            "pyramid-room-created", "true",
                            "pyramid-room-prepared", "true",
                            "pyramid-room-origin", encode(pending.candidate.origin()),
                            "pyramid-room-radius", Integer.toString(pending.radius),
                            "pyramid-room-height", Integer.toString(pending.height),
                            "pyramid-room-created-at", Instant.now().toString()))
                            .withMetadata("pyramid-reveal-in-progress", null);
                    repository.save(record);
                    sessions.put(structureId, new RoomSession(pending.context.runtime(), pending.world,
                            pending.candidate, pending.radius, pending.height, pending.shell,
                            pending.snapshots, false, false));
                    pending.context.runtime().sequence().clearFlag("pyramid.room.reveal.in_progress");
                    pending.context.runtime().sequence().setFlag("pyramid.room.created");
                    pending.context.runtime().sequence().setFlag("pyramid.room.revealed");
                    pendingReveals.remove(structureId);
                    plugin.getLogger().info("Desert Pyramid staged reveal complete: structure=" + structureId);
                    // Continue directly into the puzzle; heartbeat recovery remains only
                    // a restart fallback and is not part of the normal happy path.
                    revealCompletion.accept(pending.context);
                } catch (Exception exception) {
                    restore(pending.world, pending.snapshots);
                    pendingReveals.remove(structureId);
                    pending.context.runtime().sequence().clearFlag("pyramid.room.reveal.in_progress");
                    try {
                        StructureRecord latest = repository.get(structureId).orElse(pending.record);
                        repository.save(latest.withMetadata("pyramid-reveal-in-progress", null)
                                .withMetadata("pyramid-failure-state", "RECOVERY_REQUIRED")
                                .withMetadata("pyramid-failure-reason", "staged-reveal-failed"));
                    } catch (Exception ignored) { }
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Desert Pyramid staged reveal failed closed: " + structureId, exception);
                }
            }
        }, index == 0 ? 0L : interval);
        pending.context.runtime().tracker().track(task::cancel);
    }

    private void nudgePlayersFromOpening(World world, PyramidBlockPosition origin, int y) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getUID().equals(world.getUID())) continue;
            Location location = player.getLocation();
            if (location.getBlockY() != y + 1
                    || Math.abs(location.getBlockX() - origin.x()) > 1
                    || Math.abs(location.getBlockZ() - origin.z()) > 1) continue;
            Location[] options = {
                    location.clone().add(2.0D, 0.0D, 0.0D), location.clone().add(-2.0D, 0.0D, 0.0D),
                    location.clone().add(0.0D, 0.0D, 2.0D), location.clone().add(0.0D, 0.0D, -2.0D)
            };
            for (Location safe : options) {
                Block feet = safe.getBlock();
                Block head = world.getBlockAt(feet.getX(), feet.getY() + 1, feet.getZ());
                Block floor = world.getBlockAt(feet.getX(), feet.getY() - 1, feet.getZ());
                if (feet.isPassable() && head.isPassable() && floor.getType().isSolid()) {
                    player.teleport(safe);
                    break;
                }
            }
        }
    }

    private void carveShaftLayer(World world, PyramidBlockPosition origin, int y) {
        for (int x = origin.x() - 1; x <= origin.x() + 1; x++) {
            for (int z = origin.z() - 1; z <= origin.z() + 1; z++) {
                world.getBlockAt(x, y, z).setType(Material.AIR, false);
            }
        }
    }

    private void carveRoom(World world, PyramidBlockPosition origin, int radius, int height) {
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

    /** Backward-compatible immediate creation entry point for existing callers. */
    public synchronized PyramidRoomCandidate create(ExplorationEventContext context,
                                                     ExplorationComponentSpec spec) throws IOException {
        prepare(context, spec);
        return reveal(context, spec);
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
        PendingReveal pending = pendingReveals.remove(structureId);
        if (pending != null) {
            restore(pending.world, pending.snapshots);
            pending.context.runtime().sequence().clearFlag("pyramid.room.reveal.in_progress");
        }
        RoomSession session = sessions.remove(structureId);
        if (session == null || session.runtime.sequence().flag("pyramid.puzzle.solved") || session.persisted) return;
        boolean revealed = repository.get(structureId)
                .map(record -> Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-room-created", "false")))
                .orElse(false);
        // A committed room is world-persistent: disable/reload removes runtime objects only.
        if (revealed) return;
        restore(session.world, session.snapshots);
        plugin.getLogger().info("Desert Pyramid underground room rolled back: structure=" + structureId);
    }

    public synchronized void stopAll() {
        for (UUID structureId : List.copyOf(sessions.keySet())) cleanup(structureId);
    }

    private PyramidRoomCandidate findBuriedCandidate(World world, StructureBounds bounds,
                                                      int radius, int height, int shell,
                                                      int anchorX, int anchorZ) {
        // The clicked chest identifies vanilla loot, but the shaft must remain at the
        // shared treasure-chamber centre, never beneath one of its four corner chests.
        int centerX = anchorX;
        int centerZ = anchorZ;
        int highest = bounds.minY() - 4;
        int lowest = Math.max(world.getMinHeight() + shell + 2, highest - 32);
        int x = centerX;
        int z = centerZ;
        for (int y = highest; y >= lowest; y--) {
            PyramidBlockPosition origin = new PyramidBlockPosition(x, y, z);
            if (buried(world, origin, radius, height, shell)) {
                return new PyramidRoomCandidate(PyramidRoomCandidate.Slot.CENTER, origin, PyramidRoomOrientation.NORTH);
            }
        }
        return null;
    }

    private boolean buried(World world, PyramidBlockPosition origin, int radius, int height, int shell) {
        int outer = radius + shell;
        int minY = origin.y() - 1 - shell;
        int maxY = origin.y() + height + shell;
        int minChunkX = (origin.x() - outer) >> 4;
        int maxChunkX = (origin.x() + outer) >> 4;
        int minChunkZ = (origin.z() - outer) >> 4;
        int maxChunkZ = (origin.z() + outer) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) return false;
            }
        }
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

    private List<BlockSnapshot> snapshot(World world, PyramidBlockPosition origin, int radius, int height,
                                         StructureBounds bounds) {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        for (int x = origin.x() - radius; x <= origin.x() + radius; x++) {
            for (int z = origin.z() - radius; z <= origin.z() + radius; z++) {
                for (int y = origin.y() - 1; y <= origin.y() + height; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    snapshots.add(new BlockSnapshot(x, y, z, block.getBlockData().clone()));
                }
            }
        }
        for (int y = origin.y() + height; y < bounds.minY(); y++) {
            for (int x = origin.x() - 1; x <= origin.x() + 1; x++) {
                for (int z = origin.z() - 1; z <= origin.z() + 1; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    snapshots.add(new BlockSnapshot(x, y, z, block.getBlockData().clone()));
                }
            }
        }
        return List.copyOf(snapshots);
    }

    private void restore(World world, List<BlockSnapshot> snapshots) {
        for (BlockSnapshot snapshot : snapshots) {
            world.getBlockAt(snapshot.x(), snapshot.y(), snapshot.z()).setBlockData(snapshot.data(), false);
        }
    }

    /** Bounded signature check used before trusting persisted room-created metadata. */
    private boolean physicalRoomValid(World world, PyramidBlockPosition origin, int radius, int height) {
        if (origin == null || !world.isChunkLoaded(origin.x() >> 4, origin.z() >> 4)) return false;
        Material[] wallTypes = {Material.SANDSTONE, Material.CHISELED_SANDSTONE};
        java.util.function.Predicate<Material> wall = type -> java.util.Arrays.stream(wallTypes).anyMatch(type::equals);
        for (int y : new int[] {origin.y(), origin.y() + height}) {
            for (int x : new int[] {origin.x() - radius, origin.x() + radius}) {
                for (int z : new int[] {origin.z() - radius, origin.z() + radius}) {
                    if (!wall.test(world.getBlockAt(x, y, z).getType())) return false;
                }
            }
        }
        Block floor = world.getBlockAt(origin.x(), origin.y() - 1, origin.z());
        Block interior = world.getBlockAt(origin.x(), origin.y() + 1, origin.z());
        return floor.getType().isSolid() && !floor.isLiquid() && interior.getType().isAir();
    }

    private boolean protectedBlock(BlockState state) {
        return state instanceof org.bukkit.inventory.InventoryHolder
                || state.getType() == Material.TNT
                || state.getType().name().contains("SPAWNER")
                || state.getType().name().contains("PORTAL");
    }

    private boolean shaftSafe(World world, PyramidBlockPosition origin, StructureBounds bounds) {
        for (int x = origin.x() - 1; x <= origin.x() + 1; x++) {
            for (int z = origin.z() - 1; z <= origin.z() + 1; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            }
        }
        for (int y = origin.y() + 1; y < bounds.minY(); y++) {
            for (int x = origin.x() - 1; x <= origin.x() + 1; x++) {
                for (int z = origin.z() - 1; z <= origin.z() + 1; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isLiquid() || protectedBlock(block.getState())) return false;
                }
            }
        }
        return true;
    }

    private StructureRecord withMetadata(StructureRecord base, Map<String, String> values) {
        var record = base;
        for (var entry : values.entrySet()) record = record.withMetadata(entry.getKey(), entry.getValue());
        return record;
    }

    private StructureRecord withMetadata(ExplorationEventContext context, Map<String, String> values) {
        return withMetadata(context.record(), values);
    }

    private Optional<PyramidRoomCandidate> readPersistedCandidate(StructureRecord record) {
        String encoded = record.activationMetadata().get("pyramid-room-origin");
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

    private int persistedInt(StructureRecord record, String key, int fallback) {
        String value = record.activationMetadata().get(key);
        if (value == null) return fallback;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private record BlockSnapshot(int x, int y, int z, org.bukkit.block.data.BlockData data) { }

    private static final class PendingReveal {
        private final ExplorationEventContext context;
        private final StructureRecord record;
        private final ExplorationComponentSpec spec;
        private final PyramidRoomCandidate candidate;
        private final int radius;
        private final int height;
        private final int shell;
        private final List<BlockSnapshot> snapshots;
        private final int startY;
        private final World world;

        private PendingReveal(ExplorationEventContext context, StructureRecord record, ExplorationComponentSpec spec,
                              PyramidRoomCandidate candidate, int radius, int height, int shell,
                              List<BlockSnapshot> snapshots, int startY) {
            this.context = context;
            this.record = record;
            this.spec = spec;
            this.candidate = candidate;
            this.radius = radius;
            this.height = height;
            this.shell = shell;
            this.snapshots = snapshots;
            this.startY = startY;
            this.world = context.world().orElseThrow();
        }
    }

    private static final class RoomSession {
        private final com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime;
        private final World world;
        private final PyramidRoomCandidate candidate;
        private final int radius;
        private final int height;
        private final int shell;
        private final List<BlockSnapshot> snapshots;
        private final boolean persisted;
        private final boolean prepared;

        private RoomSession(com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime, World world,
                            PyramidRoomCandidate candidate, int radius, int height, int shell,
                            List<BlockSnapshot> snapshots, boolean persisted) {
            this(runtime, world, candidate, radius, height, shell, snapshots, persisted, false);
        }

        private RoomSession(com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime,
                            World world, PyramidRoomCandidate candidate, int radius, int height, int shell,
                            List<BlockSnapshot> snapshots, boolean persisted, boolean prepared) {
            this.runtime = runtime;
            this.world = world;
            this.candidate = candidate;
            this.radius = radius;
            this.height = height;
            this.shell = shell;
            this.snapshots = snapshots;
            this.persisted = persisted;
            this.prepared = prepared;
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
