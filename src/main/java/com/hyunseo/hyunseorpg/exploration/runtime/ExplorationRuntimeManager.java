package com.hyunseo.hyunseorpg.exploration.runtime;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentRegistry;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationRegistry;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.StructureVariantDefinition;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRewardTransaction;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidTreasureCenterPolicy;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidUndergroundCompletionCoordinator;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidUndergroundCompletionState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** E3-E5 runtime coordinator. Persistent state transition always precedes runtime activation. */
public final class ExplorationRuntimeManager {
    public static final int CURRENT_PYRAMID_CONTENT_VERSION = 3;
    private static final long TELEPORT_EXEMPTION_TICKS = 60L;
    public enum ChoiceResult { ACCEPTED, FLED, NOT_FOUND, NOT_PENDING, NOT_OWNER, OUT_OF_RANGE, INVALID_CHOICE, SPAWN_FAILED }
    private final JavaPlugin plugin;
    private final ExplorationRegistry registry;
    private final StructureRepository repository;
    private final ExplorationComponentRegistry components;
    private final ExplorationPorts ports;
    private final TeleportExemptionService teleportExemptions;
    private final Map<UUID, ExplorationRuntime> active = new LinkedHashMap<>();
    private final Map<UUID, ExplorationEndReason> lastEndReasons = new LinkedHashMap<>();

    public ExplorationRuntimeManager(JavaPlugin plugin, ExplorationRegistry registry,
                                     StructureRepository repository, ExplorationComponentRegistry components,
                                     ExplorationPorts ports, TeleportExemptionService teleportExemptions) {
        this.plugin = plugin;
        this.registry = registry;
        this.repository = repository;
        this.components = components;
        this.ports = ports;
        this.teleportExemptions = teleportExemptions;
    }

    public synchronized Optional<ExplorationRuntime> get(UUID structureId) {
        return Optional.ofNullable(active.get(structureId));
    }

    public synchronized java.util.List<ExplorationRuntime> activeRuntimes() {
        return java.util.List.copyOf(active.values());
    }


    /** Returns whether a block is owned by an active Pyramid puzzle and must not be modified. */
    public synchronized boolean isPyramidPuzzleProtected(org.bukkit.block.Block block) {
        if (block == null || block.getWorld() == null) return false;
        for (ExplorationRuntime runtime : active.values()) {
            if (!runtime.sequence().flag("pyramid.puzzle.active")) continue;
            StructureRecord record = repository.get(runtime.structureId()).orElse(null);
            if (record == null || record.state() != StructureEventState.ACTIVE
                    || !"desert_pyramid".equals(record.structureType())) continue;
            Map<String, String> metadata = record.activationMetadata();
            if ("RECOVERY_REQUIRED".equals(metadata.getOrDefault("pyramid-failure-state", ""))) continue;
            if (!record.worldId().equals(block.getWorld().getUID())) continue;
            int x = block.getX(), y = block.getY(), z = block.getZ();
            String originRaw = metadata.get("pyramid-room-origin");
            int radius = parseInt(metadata.get("pyramid-room-radius"), 4);
            int height = parseInt(metadata.get("pyramid-room-height"), 4);
            if (originRaw != null) {
                String[] parts = originRaw.split(",");
                if (parts.length == 3) {
                    try {
                        int ox = Integer.parseInt(parts[0].trim());
                        int oy = Integer.parseInt(parts[1].trim());
                        int oz = Integer.parseInt(parts[2].trim());
                        if (Math.abs(x - ox) <= radius && Math.abs(z - oz) <= radius
                                && y >= oy - 1 && y < oy + height) return true;
                    } catch (NumberFormatException ignored) { return true; }
                }
            }
            int centerX = parseInt(metadata.get("pyramid-treasure-center-x"), Integer.MIN_VALUE);
            int centerZ = parseInt(metadata.get("pyramid-treasure-center-z"), Integer.MIN_VALUE);
            int treasureY = parseInt(metadata.get("pyramid-treasure-y"), Integer.MIN_VALUE);
            if (centerX != Integer.MIN_VALUE && centerZ != Integer.MIN_VALUE && treasureY != Integer.MIN_VALUE
                    && Math.abs(x - centerX) <= 1 && Math.abs(z - centerZ) <= 1
                    && y >= treasureY - 1 && y <= treasureY + 1) return true;
        }
        return false;
    }

    private int parseInt(String raw, int fallback) {
        try { return raw == null ? fallback : Integer.parseInt(raw); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    /** Builds a non-mutating diagnostic view for one persistent structure. */
    /** Clears one Pyramid's retry/progression metadata without touching other structures. */
    public synchronized boolean resetPyramid(UUID structureId) {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null || !"desert_pyramid".equals(record.structureType()) || record.state().terminal()) return false;
        ExplorationRuntime runtime = active.remove(structureId);
        if (runtime != null) safeCleanup(runtime);
        StructureRecord reset = record;
        for (String key : List.of("loot-taken", "looter", "loot-taken-at-tick", "pyramid-entry-actor",
                "pyramid-entry-dx", "pyramid-entry-dz", "pyramid.entry.prompted", "pyramid-guardian-complete",
                "pyramid-underground-complete", "pyramid-underground-completion-state", "pyramid-guardian-encounter-state", "pyramid-guardian-started",
                "pyramid-guardian-spawned", "pyramid-treasure-x", "pyramid-treasure-y", "pyramid-treasure-z",
                "pyramid-room-prepared", "pyramid-room-created", "pyramid-room-created-at", "pyramid-room-origin",
                "pyramid-room-radius", "pyramid-room-height",
                "pyramid-reward-recipient", "pyramid-reward-state", "pyramid-reward-delivered-to",
                "pyramid-reveal-in-progress", "pyramid-failure-state", "pyramid-failure-reason")) {
            reset = reset.withMetadata(key, null);
        }
        for (String key : record.activationMetadata().keySet()) {
            if (key.startsWith("pyramid-pillar-position-") || key.startsWith("pyramid-pillar-solved-")) {
                reset = reset.withMetadata(key, null);
            }
        }
        try {
            repository.save(reset);
            lastEndReasons.remove(structureId);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to reset Pyramid " + structureId, exception);
            return false;
        }
    }

    public synchronized Optional<Map<String, String>> pyramidDiagnostics(UUID structureId) {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null || !"desert_pyramid".equals(record.structureType())) return Optional.empty();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("structure", record.structureId().toString());
        result.put("state", record.state().name());
        result.put("variant", record.variantId());
        result.put("content-version", record.activationMetadata().getOrDefault("pyramid-content-version", "0"));
        for (String key : List.of("pyramid-entry-actor", "pyramid-entry-dx", "pyramid-entry-dz",
                "pyramid-guardian-complete", "pyramid-underground-complete", "pyramid-underground-completion-state", "pyramid-guardian-encounter-state",
                "loot-taken", "pyramid-treasure-x", "pyramid-treasure-y", "pyramid-treasure-z",
                "pyramid-room-prepared", "pyramid-room-created", "pyramid-room-origin", "pyramid-room-radius", "pyramid-room-height",
                "pyramid-failure-state", "pyramid-failure-reason",
                "pyramid-puzzle-ready", "pyramid-puzzle-solved", "pyramid-reward-recipient", "pyramid-reward-state", "pyramid-reward-delivered-to")) {
            result.put(key, record.activationMetadata().getOrDefault(key, "false"));
        }
        record.activationMetadata().forEach((key, value) -> {
            if (key.startsWith("pyramid-pillar-position-") || key.startsWith("pyramid-pillar-solved-")) {
                result.put(key, value);
            }
        });
        return Optional.of(Map.copyOf(result));
    }

    public synchronized Optional<ExplorationStatusSnapshot> status(UUID structureId) {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null) return Optional.empty();
        ExplorationRuntime runtime = active.get(structureId);
        return Optional.of(new ExplorationStatusSnapshot(
                record.structureId(),
                record.structureType(),
                record.variantId(),
                record.state(),
                runtime != null,
                runtime == null ? 0 : runtime.participants().size(),
                runtime == null ? 0 : runtime.objectiveEntities().size(),
                lastEndReasons.get(structureId)));
    }

    /** Returns snapshots for runtimes or structures with a recorded lifecycle outcome. */
    public synchronized java.util.List<ExplorationStatusSnapshot> statuses() {
        java.util.LinkedHashSet<UUID> ids = new java.util.LinkedHashSet<>(active.keySet());
        ids.addAll(lastEndReasons.keySet());
        return ids.stream().map(this::status).flatMap(Optional::stream).toList();
    }

    /**
     * Returns the latest lifecycle outcome observed for a structure.
     * The value is diagnostic state only and is not persisted as gameplay state.
     */
    public synchronized Optional<ExplorationEndReason> lastEndReason(UUID structureId) {
        return Optional.ofNullable(lastEndReasons.get(structureId));
    }

    /** Returns a stable snapshot for an administrative/debug surface. */
    public synchronized Map<UUID, ExplorationEndReason> lastEndReasons() {
        return Map.copyOf(lastEndReasons);
    }

    public synchronized boolean activate(StructureRecord record, Player trigger, long currentTick) {
        if (record.state().terminal()) return false;
        ExplorationRuntime existing = active.get(record.structureId());
        if (existing != null) {
            existing.addParticipant(trigger.getUniqueId());
            // Proximity heartbeats must not erase an already-running Outpost loot-exit timer.
            // Teleports and an actual return inside the loot radius clear it explicitly.
            existing.clearCombatAbandonExit();
            lastEndReasons.remove(record.structureId());
            return true;
        }
        StructureRecord persistent = record;
        try {
            if ("desert_pyramid".equals(record.structureType())) {
                persistent = migratePyramidRecord(persistent);
                persistent = reconcilePendingPyramidUndergroundCompletion(persistent);
                boolean recoveryRequired = "RECOVERY_REQUIRED".equals(persistent.activationMetadata()
                        .getOrDefault("pyramid-failure-state", ""));
                boolean revealInterrupted = Boolean.parseBoolean(persistent.activationMetadata()
                        .getOrDefault("pyramid-reveal-in-progress", "false"));
                if (revealInterrupted && !recoveryRequired) {
                    persistent = persistent.withMetadata("pyramid-reveal-in-progress", null)
                            .withMetadata("pyramid-failure-state", "RECOVERY_REQUIRED")
                            .withMetadata("pyramid-failure-reason", "reveal-interrupted-during-reload");
                    repository.save(persistent);
                    recoveryRequired = true;
                }
                if (recoveryRequired) {
                    plugin.getLogger().warning("Pyramid activation refused: recovery required for "
                            + persistent.structureId());
                    return false;
                }
            }
            if (record.state() == StructureEventState.UNDISCOVERED) {
                persistent = persistent.transitionTo(StructureEventState.ACTIVE, Instant.now())
                        .withMetadata("activated-by", trigger.getUniqueId().toString())
                        .withMetadata("activated-at", Long.toString(System.currentTimeMillis()));
                repository.save(persistent);
            }
            ExplorationRuntime runtime = new ExplorationRuntime(persistent.structureId(), persistent.variantId(), currentTick);
            runtime.addParticipant(trigger.getUniqueId());
            restoreLootState(persistent, runtime);
            restorePyramidState(persistent, runtime, trigger);
            active.put(persistent.structureId(), runtime);
            lastEndReasons.remove(persistent.structureId());
            plugin.getLogger().info("Exploration activation: structure=" + persistent.structureType()
                    + ", variant=" + persistent.variantId() + ", id=" + persistent.structureId());
            executePhase(persistent, runtime, ExplorationComponentPhase.ACTIVATE, currentTick);
            recoverPyramidGuardianEncounter(persistent, runtime, currentTick);
            if (persistent.structureType().equals("desert_pyramid")
                    && pyramidModulesComplete(persistent.structureId())) {
                complete(persistent.structureId(), currentTick);
                return true;
            }
            if (runtime.lootTaken()
                    && Boolean.parseBoolean(persistent.activationMetadata().getOrDefault("loot-exit-prompted", "false"))) {
                executePhase(persistent, runtime, ExplorationComponentPhase.LOOT_EXIT, currentTick);
                runtime.markLootExitPrompted();
            }
            if (runtime.lootTaken() && persistent.structureType().equals("desert_pyramid")
                    && !Boolean.parseBoolean(persistent.activationMetadata().getOrDefault("pyramid-underground-complete", "false"))) {
                // Delayed Bukkit tasks are intentionally not persisted. Re-arm the
                // deterministic room reveal, or restore the already-created room
                // and its puzzle displays, after a reload/reconnect.
                if (isCommittedPyramidRoomReadyForReload(persistent, runtime.sequence().flag("pyramid.puzzle.started"))) {
                    // A committed room is post-carve state. Restore the pillar
                    // component once; room reveal is never replayed.
                    continuePyramidPuzzle(new ExplorationEventContext(plugin, persistent, runtime, ports,
                            teleportExemptions, currentTick, this::scheduleSequencePhase));
                } else {
                    executePhase(persistent, runtime, ExplorationComponentPhase.PYRAMID_LOOT_TRIGGER, currentTick);
                }
            }
            return true;
        } catch (Exception exception) {
            ExplorationRuntime failed = active.remove(record.structureId());
            if (failed != null) safeCleanup(failed);
            recordEnd(record.structureId(), ExplorationEndReason.ACTIVATION_FAILURE);
            // A failed ACTIVATE phase must not strand a persisted ACTIVE record with no runtime.
            if (record.state() == StructureEventState.UNDISCOVERED) {
                try {
                    repository.save(record);
                } catch (IOException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            plugin.getLogger().log(Level.SEVERE, "Exploration activation failed for " + record.structureId()
                    + "; persistent state rolled back when activation had not completed", exception);
            return false;
        }
    }

    public synchronized boolean complete(UUID structureId, long currentTick) {
        ExplorationRuntime runtime = active.get(structureId);
        StructureRecord record = repository.get(structureId).orElse(null);
        if (runtime == null || record == null || record.state() != StructureEventState.ACTIVE) return false;
        try {
            StructureRecord rewardCheckpoint = record;
            if ("desert_pyramid".equals(record.structureType()) && runtime.entryActor() != null
                    && !record.rewardClaimed()) {
                rewardCheckpoint = record.withMetadata("pyramid-reward-recipient", runtime.entryActor().toString())
                        .withMetadata("pyramid-reward-state", PyramidRewardTransaction.State.DELIVERY_PENDING.value());
                repository.save(rewardCheckpoint);
            }
            executePhase(rewardCheckpoint, runtime, ExplorationComponentPhase.CLEAR, currentTick);
            if ("desert_pyramid".equals(record.structureType())) {
                for (UUID recipient : java.util.Set.copyOf(runtime.participants())) {
                    if (runtime.sequence().flag("pyramid.reward.delivered." + recipient)) {
                        rewardCheckpoint = rewardCheckpoint.withMetadata("pyramid-reward-delivered-to", recipient.toString())
                                .withMetadata("pyramid-reward-state", "delivered");
                    }
                }
                if (runtime.entryActor() != null && runtime.sequence().flag("pyramid.reward.delivered." + runtime.entryActor())) {
                    rewardCheckpoint = rewardCheckpoint.withMetadata("pyramid-reward-delivered-to", runtime.entryActor().toString())
                        .withMetadata("pyramid-reward-state", "delivered");
                }
            }
            StructureRecord completed = rewardCheckpoint.transitionTo(StructureEventState.CLEARED, Instant.now());
            if (hasRewardPhase(record)) completed = completed.markRewardClaimed();
            repository.save(completed);
            active.remove(structureId);
            teleportExemptions.clear(structureId);
            safeCleanup(runtime);
            recordEnd(structureId, ExplorationEndReason.FINAL_CLEAR);
            return true;
        } catch (Exception exception) {
            recordEnd(structureId, ExplorationEndReason.COMPLETION_FAILURE);
            plugin.getLogger().log(Level.SEVERE, "Exploration completion failed for " + structureId + "; state stays ACTIVE", exception);
            return false;
        }
    }

    /** Resolves a choice created by choice_prompt without creating a second encounter engine. */
    public synchronized ChoiceResult choose(UUID structureId, Player player, String rawChoice, long currentTick) {
        ExplorationRuntime runtime = active.get(structureId);
        StructureRecord record = repository.get(structureId).orElse(null);
        if (runtime == null || record == null || record.state() != StructureEventState.ACTIVE) return ChoiceResult.NOT_FOUND;
        if (!runtime.choicePending()) return ChoiceResult.NOT_PENDING;
        if (player == null || !player.getUniqueId().equals(runtime.choiceOwner())) return ChoiceResult.NOT_OWNER;
        String choice = normalizeChoice(rawChoice);
        if (!runtime.allowedChoices().contains(choice)) return ChoiceResult.INVALID_CHOICE;
        ExplorationStructureDefinition definition = registry.get(record.structureType()).orElse(null);
        if (!"flee".equals(choice) && !runtime.lootTaken() && (definition == null || !record.worldId().equals(player.getWorld().getUID())
                || distanceSquared(record, player.getLocation()) > definition.combatAbandonRadius() * definition.combatAbandonRadius())) {
            return ChoiceResult.OUT_OF_RANGE;
        }
        if (!runtime.choose(player.getUniqueId(), choice)) return ChoiceResult.NOT_PENDING;
        if ("flee".equals(choice)) {
            abandon(structureId);
            return ChoiceResult.FLED;
        }
        try {
            runtime.setRaidTarget(player.getUniqueId());
            runtime.snapshotRaidOrigin(player.getLocation());
            runtime.clearCombatAbandonExit();
            if (record.structureType().equals("desert_pyramid")) {
                repository.save(repository.get(structureId).orElse(record)
                        .withMetadata("pyramid-guardian-encounter-state", "spawn_pending"));
                executePhase(record, runtime, ExplorationComponentPhase.PYRAMID_GUARDIAN_SPAWN, currentTick);
            } else {
                executePhase(record, runtime, phaseForChoice(choice), currentTick);
            }
            if (!runtime.objectiveMode() || runtime.objectiveEntities().isEmpty()) {
                throw new IllegalStateException("raid choice produced no objective entities");
            }
            runtime.markRaidStarted();
            String message = record.structureType().equals("desert_pyramid")
                    ? "사막 피라미드의 수호자가 깨어났습니다."
                    : "약탈자 전초기지 습격이 시작되었습니다.";
            player.sendMessage(net.kyori.adventure.text.Component.text(message));
            return ChoiceResult.ACCEPTED;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Exploration choice failed for " + structureId + ", choice=" + choice, exception);
            if (record.structureType().equals("desert_pyramid")) {
                runtime.releaseChoiceForRetry();
                runtime.sequence().clearFlag("pyramid.guardian.spawned");
                runtime.sequence().clearFlag("pyramid.guardian.started");
                // Re-arm the exterior crossing after a technical spawn failure. The
                // actor/direction remain persisted so the next valid entrant may replace
                // this pending attempt without a terminal state.
                runtime.sequence().clearFlag("pyramid.entry.prompted");
                runtime.sequence().clearFlag("pyramid.guardian.encounter.started");
                try {
                    repository.save(repository.get(structureId).orElse(record)
                            .withMetadata("pyramid-guardian-encounter-state", "not_started")
                            .withMetadata("pyramid-guardian-started", null)
                            .withMetadata("pyramid-guardian-spawned", null));
                } catch (IOException persistenceFailure) {
                    plugin.getLogger().log(Level.WARNING, "Unable to clear failed Pyramid encounter checkpoint: " + structureId, persistenceFailure);
                }
                return ChoiceResult.SPAWN_FAILED;
            }
            abandon(structureId);
            return ChoiceResult.SPAWN_FAILED;
        }
    }

    /** Arms an outpost runtime when a player actually removes an item from its container. */
    public synchronized boolean markLootTaken(Location container, Player player, long currentTick) {
        if (container == null || player == null || container.getWorld() == null) return false;
        boolean marked = false;
        for (StructureRecord record : repository.index().nearby(container.getWorld().getUID(),
                container.getX(), container.getZ(), 1.0D)) {
            if (!isLootContainerInStructure(record, container)) continue;
            if ("desert_pyramid".equals(record.structureType())
                    && !isValidPyramidTreasureContainer(record, container)) continue;
            if (record.state() == StructureEventState.UNDISCOVERED) {
                activate(record, player, currentTick);
            }
            ExplorationRuntime runtime = active.get(record.structureId());
            if (runtime == null || runtime.lootTaken()) continue;
            runtime.markLootTaken(player.getUniqueId(), currentTick);
            try {
                StructureRecord updated = repository.get(record.structureId()).orElse(record)
                        .withMetadata("loot-taken", "true")
                        .withMetadata("looter", player.getUniqueId().toString())
                        .withMetadata("loot-taken-at-tick", Long.toString(currentTick));
                if (record.structureType().equals("desert_pyramid")) {
                    updated = updated.withMetadata("pyramid-treasure-x", Integer.toString(container.getBlockX()))
                            .withMetadata("pyramid-treasure-y", Integer.toString(container.getBlockY()))
                            .withMetadata("pyramid-treasure-z", Integer.toString(container.getBlockZ()));
                }
                repository.save(updated);
                marked = true;
                if (record.structureType().equals("desert_pyramid")) {
                    try {
                        executeNamedPhase(repository.get(record.structureId()).orElse(updated), runtime,
                                ExplorationComponentPhase.PYRAMID_LOOT_TRIGGER.name(), currentTick);
                    } catch (Exception exception) {
                        // World/phase/display failures are retryable Pyramid diagnostics, never gameplay abandonment.
                        plugin.getLogger().log(Level.WARNING,
                                "Desert Pyramid loot sequence deferred (retryable): " + record.structureId(), exception);
                    }
                }
                plugin.getLogger().info("Exploration structure loot armed: structure=" + record.structureId()
                        + ", looter=" + player.getUniqueId());
            } catch (IOException exception) {
                // The in-memory trigger was only a reservation; roll it back when the
                // durable checkpoint cannot be written so the chest remains retryable.
                runtime.clearLootTaken();
                plugin.getLogger().log(Level.WARNING, "Unable to persist outpost loot state; trigger rolled back: " + record.structureId(), exception);
            }
        }
        return marked;
    }

    /**
     * Vanilla desert-temple treasure rooms can extend below the Paper structure
     * bounding box. Keep the horizontal structure ownership strict while
     * allowing the bounded underground loot chamber to trigger the event.
     */
    static boolean isLootContainerInStructure(StructureRecord record, Location container) {
        if (record == null || container == null || container.getWorld() == null
                || !record.worldId().equals(container.getWorld().getUID())) return false;
        return isLootContainerInStructure(record, container.getX(), container.getY(), container.getZ());
    }

    static boolean isValidPyramidTreasureContainer(StructureRecord record, Location container) {
        if (record == null || container == null || container.getWorld() == null
                || !"desert_pyramid".equals(record.structureType())
                || !record.worldId().equals(container.getWorld().getUID())) return false;
        return isValidPyramidTreasureSlot(record, container.getBlock().getType(),
                container.getBlockX(), container.getBlockY(), container.getBlockZ());
    }

    /** Pure geometry predicate shared by the Bukkit listener and deterministic tests. */
    static boolean isValidPyramidTreasureSlot(StructureRecord record, org.bukkit.Material type,
                                              int blockX, int blockY, int blockZ) {
        if (record == null || !"desert_pyramid".equals(record.structureType())
                || type != org.bukkit.Material.CHEST) return false;
        PyramidTreasureCenterPolicy.Center center = PyramidTreasureCenterPolicy.from(record.bounds());
        return Math.abs(blockX - center.x()) == 2
                && Math.abs(blockZ - center.z()) == 2
                && blockY >= record.bounds().minY()
                && blockY <= record.bounds().minY() + 3;
    }

    public static boolean isLootContainerInStructure(StructureRecord record, double x, double y, double z) {
        if (record == null) return false;
        if (record.structureType().equals("pillager_outpost")) {
            return record.bounds().contains(x, y, z);
        }
        if (!record.structureType().equals("desert_pyramid")) return false;
        boolean horizontal = x >= record.bounds().minX()
                && x <= record.bounds().maxX()
                && z >= record.bounds().minZ()
                && z <= record.bounds().maxZ();
        return horizontal
                && y >= record.bounds().minY() - 16
                && y <= record.bounds().maxY() + 2;
    }

    public synchronized boolean abandon(UUID structureId) {
        ExplorationRuntime runtime = active.get(structureId);
        StructureRecord record = repository.get(structureId).orElse(null);
        if (runtime == null || record == null || record.state() != StructureEventState.ACTIVE) return false;
        try {
            repository.save(record.transitionTo(StructureEventState.ABANDONED, Instant.now()));
            active.remove(structureId);
            teleportExemptions.clear(structureId);
            safeCleanup(runtime);
            recordEnd(structureId, ExplorationEndReason.ABANDON_GRACE);
            return true;
        } catch (IOException exception) {
            recordEnd(structureId, ExplorationEndReason.ABANDON_FAILURE);
            plugin.getLogger().log(Level.SEVERE, "Exploration abandon persistence failed for " + structureId, exception);
            return false;
        }
    }

    public synchronized void heartbeat(long currentTick) {
        for (ExplorationRuntime runtime : java.util.List.copyOf(active.values())) {
            StructureRecord record = repository.get(runtime.structureId()).orElse(null);
            if (record == null || record.state() != StructureEventState.ACTIVE) {
                active.remove(runtime.structureId());
                safeCleanup(runtime);
                recordEnd(runtime.structureId(), ExplorationEndReason.STATE_INVALID);
                continue;
            }
            ExplorationStructureDefinition definition = registry.get(record.structureType()).orElse(null);
            if (definition == null) continue;

            // Pyramid pillar activation is intentionally single-shot. A committed room is
            // restored only during deterministic activation/reload; failures are marked
            // RECOVERY_REQUIRED and never retried by heartbeat.
            if (runtime.choiceExpired(currentTick)) {
                plugin.getLogger().info("Exploration choice timed out: structure=" + record.structureType()
                        + ", id=" + record.structureId() + ", default=" + runtime.defaultChoice());
                if ("desert_pyramid".equals(record.structureType())) {
                    UUID owner = runtime.choiceOwner();
                    String fallback = runtime.defaultChoice();
                    if (owner != null && runtime.choose(owner, fallback)) {
                        try {
                            repository.save(repository.get(record.structureId()).orElse(record)
                                    .withMetadata("pyramid-guardian-encounter-state", "spawn_pending"));
                            executePhase(record, runtime, ExplorationComponentPhase.PYRAMID_GUARDIAN_SPAWN, currentTick);
                            runtime.markRaidStarted();
                            repository.save(repository.get(record.structureId()).orElse(record)
                                    .withMetadata("pyramid-guardian-encounter-state", "active")
                                    .withMetadata("pyramid-guardian-started", "true"));
                        } catch (Exception exception) {
                            runtime.releaseChoiceForRetry();
                            runtime.sequence().clearFlag("pyramid.entry.prompted");
                            runtime.sequence().clearFlag("pyramid.guardian.spawned");
                            runtime.sequence().clearFlag("pyramid.guardian.started");
                            try {
                                repository.save(repository.get(record.structureId()).orElse(record)
                                        .withMetadata("pyramid-guardian-encounter-state", "not_started")
                                        .withMetadata("pyramid-guardian-started", null)
                                        .withMetadata("pyramid-guardian-spawned", null));
                            } catch (IOException persistenceFailure) {
                                plugin.getLogger().log(Level.WARNING,
                                        "Unable to re-arm Pyramid timeout retry: " + record.structureId(), persistenceFailure);
                            }
                            plugin.getLogger().log(Level.WARNING,
                                    "Pyramid timeout guardian start deferred: " + record.structureId(), exception);
                        }
                    }
                } else {
                    abandon(record.structureId());
                }
                continue;
            }

            if (record.structureType().equals("pillager_outpost")
                    && runtime.lootTaken() && !runtime.lootExitPrompted() && !runtime.raidStarted()) {
                Player looter = runtime.looter() == null ? null : Bukkit.getPlayer(runtime.looter());
                if (looter != null && looter.isOnline() && looter.getWorld().getUID().equals(record.worldId())) {
                    if (teleportExemptions.isExempt(record.structureId(), looter.getUniqueId(), currentTick)) {
                        runtime.clearLootTriggerExit();
                        runtime.clearCombatAbandonExit();
                        continue;
                    }
                    double limit = definition.lootTriggerRadius() * definition.lootTriggerRadius();
                    if (distanceSquared(record, looter.getLocation()) <= limit) {
                        runtime.clearLootTriggerExit();
                    } else {
                        if (runtime.lootTriggerExitAtTick() == null) runtime.markLootTriggerExit(currentTick);
                        if (currentTick - runtime.lootTriggerExitAtTick() >= definition.lootTriggerGraceTicks()) {
                            runtime.snapshotRaidOrigin(looter.getLocation());
                            try {
                                executePhase(record, runtime, ExplorationComponentPhase.LOOT_EXIT, currentTick);
                                runtime.markLootExitPrompted();
                                repository.save(withRaidOriginMetadata(record
                                        .withMetadata("loot-exit-prompted", "true"), runtime.raidOrigin()));
                                runtime.clearLootTriggerExit();
                                plugin.getLogger().info("Exploration outpost loot exit prompt: structure=" + record.structureId()
                                        + ", origin=" + formatLocation(runtime.raidOrigin()));
                            } catch (Exception exception) {
                                plugin.getLogger().log(Level.WARNING, "Unable to start outpost loot exit prompt: " + record.structureId(), exception);
                                abandon(record.structureId());
                            }
                            continue;
                        }
                    }
                }
                // Loot-armed runtimes wait for the looter to cross the loot trigger; they are
                // not eligible for the combat abandon policy before a raid starts.
                continue;
            }

            try {
                if (progressSequenceWait(record, runtime, currentTick)) continue;
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Exploration sequence wait failed: " + record.structureId(), exception);
                if (!"desert_pyramid".equals(record.structureType())) abandon(record.structureId());
                continue;
            }

            // Pyramid underground completion is owned exclusively by PyramidPushPillarService.
            // Its durable completion_pending -> complete transaction is reconciled on activation;
            // heartbeat must not become a second writer.

            if (runtime.objectiveMode()) {
                if (currentTick <= runtime.activatedAtTick()) {
                    continue;
                }
                for (UUID entityId : runtime.objectiveEntities()) {
                    var entity = Bukkit.getEntity(entityId);
                    if (entity == null) {
                        plugin.getLogger().fine("Exploration objective is currently unresolved (likely unloaded): structure="
                                + record.structureId() + ", entity=" + entityId);
                    } else if (entity.isDead()) {
                        if (runtime.confirmObjectiveDeath(entityId)) {
                            plugin.getLogger().info("Exploration objective death confirmed: structure="
                                    + record.structureId() + ", entity=" + entityId);
                        }
                    } else if (!entity.isValid()) {
                        plugin.getLogger().fine("Exploration objective is invalid but not confirmed dead; retaining: structure="
                                + record.structureId() + ", entity=" + entityId);
                    } else {
                        keepRaidMobOnTarget(entity, runtime.raidTarget());
                    }
                }
                if (runtime.objectivesCleared()) {
                    if (record.structureType().equals("desert_pyramid")) {
                        // Guardian and underground are independent modules. Objective completion
                        // records only the guardian module; it must never force-start pillars.
                        try {
                            markPyramidModuleComplete(record, runtime, "pyramid-guardian-complete");
                        } catch (IOException exception) {
                            plugin.getLogger().log(Level.WARNING, "Unable to persist Pyramid guardian completion: "
                                    + record.structureId(), exception);
                            continue;
                        }
                        if (!pyramidModulesComplete(record.structureId())) continue;
                    }
                    if (runtime.hasNextRaidWave()) {
                        if (runtime.scheduleNextRaidWave(currentTick)) {
                            announceNextRaidWave(runtime);
                            continue;
                        }
                        if (!runtime.nextRaidWaveDue(currentTick)) continue;
                        try {
                            runtime.advanceRaidWave();
                            executePhase(record, runtime, ExplorationComponentPhase.NEXT_WAVE, currentTick);
                            if (runtime.objectiveEntities().isEmpty()) {
                                throw new IllegalStateException("next raid wave produced no objectives");
                            }
                            runtime.clearCombatAbandonExit();
                            plugin.getLogger().info("Exploration raid wave advanced: structure="
                                    + record.structureId() + ", wave=" + runtime.raidWaveNumber()
                                    + "/" + runtime.raidWaveCount());
                        } catch (Exception exception) {
                            plugin.getLogger().log(Level.SEVERE, "Exploration next wave failed for "
                                    + record.structureId(), exception);
                            abandon(record.structureId());
                        }
                    } else {
                        complete(record.structureId(), currentTick);
                    }
                    continue;
                }
            }

            boolean anyInside = false;
            if (runtime.raidStarted()) {
                Player raidTarget = runtime.raidTarget() == null ? null : Bukkit.getPlayer(runtime.raidTarget());
                // Once a raid has started, the target is the encounter owner. Do not fail the
                // encounter merely because the owner kites away from the structure while the
                // spawned objectives are still pursuing them.
                anyInside = raidTarget != null && raidTarget.isOnline() && !raidTarget.isDead()
                        && raidTarget.getWorld().getUID().equals(record.worldId());
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (anyInside) break;
                if (!player.getWorld().getUID().equals(record.worldId())) continue;
                if (distanceSquared(record, player.getLocation()) <= definition.combatAbandonRadius() * definition.combatAbandonRadius()) {
                    anyInside = true;
                    runtime.addParticipant(player.getUniqueId());
                    break;
                }
            }
            if (anyInside) {
                runtime.clearCombatAbandonExit();
            } else if (runtime.participants().stream()
                    .anyMatch(playerId -> teleportExemptions.isExempt(record.structureId(), playerId, currentTick))) {
                // A teleport is not a physical boundary crossing. Re-evaluate after the
                // short exemption instead of starting an abandon timer on the first heartbeat.
                runtime.clearCombatAbandonExit();
            } else {
                if (runtime.combatAbandonExitAtTick() == null) runtime.markCombatAbandonExit(currentTick);
                if (currentTick - runtime.combatAbandonExitAtTick() >= definition.combatAbandonGraceTicks()) {
                    abandon(record.structureId());
                }
            }
        }
    }

    private void markPyramidModuleComplete(StructureRecord record, ExplorationRuntime runtime, String metadataKey)
            throws IOException {
        String runtimeFlag = metadataKey.replace('-', '.');
        if (runtime.sequence().flag(runtimeFlag)) return;
        StructureRecord current = repository.get(record.structureId()).orElse(record);
        if (!Boolean.parseBoolean(current.activationMetadata().getOrDefault(metadataKey, "false"))) {
            StructureRecord updated = current.withMetadata(metadataKey, "true")
                    .withMetadata("pyramid-content-version", Integer.toString(CURRENT_PYRAMID_CONTENT_VERSION));
            if ("pyramid-guardian-complete".equals(metadataKey)) {
                updated = updated.withMetadata("pyramid-guardian-encounter-state", "complete")
                        .withMetadata("pyramid-guardian-started", "true");
            }
            repository.save(updated);
        }
        runtime.sequence().setFlag(runtimeFlag);
        plugin.getLogger().info("Desert Pyramid module complete: structure=" + record.structureId()
                + ", module=" + metadataKey);
    }

    private boolean pyramidModulesComplete(UUID structureId) {
        StructureRecord current = repository.get(structureId).orElse(null);
        if (current == null) return false;
        return Boolean.parseBoolean(current.activationMetadata().getOrDefault("pyramid-guardian-complete", "false"))
                && Boolean.parseBoolean(current.activationMetadata().getOrDefault("pyramid-underground-complete", "false"));
    }

    private double pyramidEntryPadding(ExplorationStructureDefinition definition) {
        return definition == null ? 4.0D : definition.entryBoundaryPadding();
    }

    /** A committed room may restore pillars once during deterministic activation/reload. */
    static boolean isCommittedPyramidRoomReadyForReload(StructureRecord record, boolean puzzleStarted) {
        return record != null
                && "desert_pyramid".equals(record.structureType())
                && Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-room-created", "false"))
                && !Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-underground-complete", "false"))
                && !"RECOVERY_REQUIRED".equals(record.activationMetadata().getOrDefault("pyramid-failure-state", ""))
                && !Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-reveal-in-progress", "false"))
                && !puzzleStarted;
    }


    static boolean crossesPyramidEntryBoundary(StructureRecord record, Location from, Location to, double padding) {
        if (record == null || from == null || to == null) return false;
        double p = Math.max(0.0D, padding);
        // The padded perimeter itself is a neutral threshold. Only an actual
        // outside-to-interior step starts the entry sequence, preventing a player
        // standing exactly on a border from retriggering it through jitter.
        boolean wasInside = from.getX() > record.bounds().minX() - p && from.getX() < record.bounds().maxX() + p
                && from.getZ() > record.bounds().minZ() - p && from.getZ() < record.bounds().maxZ() + p;
        boolean nowInside = to.getX() > record.bounds().minX() - p && to.getX() < record.bounds().maxX() + p
                && to.getZ() > record.bounds().minZ() - p && to.getZ() < record.bounds().maxZ() + p;
        return !wasInside && nowInside;
    }

    private void keepRaidMobOnTarget(org.bukkit.entity.Entity entity, UUID targetId) {
        if (!(entity instanceof org.bukkit.entity.LivingEntity living)) return;
        living.setGlowing(true);
        if (!(living instanceof org.bukkit.entity.Mob mob) || targetId == null) return;
        Player target = Bukkit.getPlayer(targetId);
        if (target != null && target.isOnline() && !target.isDead()
                && target.getWorld().equals(living.getWorld())) {
            mob.setTarget(target);
        }
    }

    /**
     * Evaluates only a runtime-owned bounded wait. Completion clears the gate before
     * executing its next phase, so heartbeat and player callbacks cannot progress it twice.
     */
    private boolean progressSequenceWait(StructureRecord record, ExplorationRuntime runtime, long currentTick) throws Exception {
        ExplorationSequenceState.PendingWait wait = runtime.sequence().pendingWait();
        if (wait == null) return false;
        boolean satisfied = switch (wait.condition()) {
            case "objectives-clear" -> runtime.objectivesCleared();
            case "objective-count" -> runtime.objectiveEntities().size() <= wait.threshold();
            case "counter" -> runtime.sequence().counterAtLeast(wait.key(), wait.threshold());
            case "flag" -> runtime.sequence().flag(wait.key());
            case "movement" -> runtime.sequence().movedSince(wait.movementEpoch());
            default -> throw new IllegalStateException("unsupported sequence wait " + wait.condition());
        };
        if (!satisfied) return true;

        ExplorationSequenceState.PendingWait completed = runtime.sequence().completeWait(wait.actionId());
        if (completed == null) return true;
        String nextPhase = completed.nextPhase().trim();
        if (nextPhase.isBlank() || !runtime.sequence().transitionTo(nextPhase, "wait:" + completed.condition()
                + ":" + completed.actionId())) return true;
        executeNamedPhase(record, runtime, nextPhase, currentTick);
        return true;
    }

    /** Marks an objective dead only from an explicit death event; unload is intentionally ignored. */
    public synchronized boolean confirmObjectiveDeath(UUID entityId) {
        if (entityId == null) return false;
        boolean confirmed = false;
        for (ExplorationRuntime runtime : active.values()) {
            if (runtime.confirmObjectiveDeath(entityId)) {
                confirmed = true;
                plugin.getLogger().info("Exploration objective death event accepted: structure="
                        + runtime.structureId() + ", entity=" + entityId);
                if (runtime.objectivesCleared()) {
                    StructureRecord record = repository.get(runtime.structureId()).orElse(null);
                    if (record != null && "desert_pyramid".equals(record.structureType())) {
                        try {
                            markPyramidModuleComplete(record, runtime, "pyramid-guardian-complete");
                        } catch (IOException exception) {
                            plugin.getLogger().log(Level.WARNING,
                                    "Pyramid guardian completion persistence deferred: " + runtime.structureId(), exception);
                        }
                    }
                }
            }
        }
        return confirmed;
    }

    private boolean scheduleSequencePhase(ExplorationEventContext context, String actionId,
                                          long delayTicks, String nextPhase) {
        ExplorationRuntime runtime = context.runtime();
        if (!runtime.sequence().beginAction(actionId)) return false;
        UUID structureId = runtime.structureId();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (ExplorationRuntimeManager.this) {
                ExplorationRuntime current = active.get(structureId);
                StructureRecord record = repository.get(structureId).orElse(null);
                if (current != runtime || record == null || record.state() != StructureEventState.ACTIVE) return;
                runtime.sequence().completeTask(actionId);
                boolean transitioned = runtime.sequence().transitionTo(nextPhase, "delayed:" + actionId);
                // A retry intentionally re-enters the same named phase; duplicate callbacks
                // are still suppressed by the in-flight/completed action reservation.
                if (!transitioned && !runtime.sequence().currentPhase().equalsIgnoreCase(nextPhase)) {
                    runtime.sequence().releaseAction(actionId);
                    return;
                }
                try {
                    executeNamedPhase(record, runtime, nextPhase, context.currentTick() + Math.max(0L, delayTicks));
                    runtime.sequence().completeAction(actionId);
                } catch (Exception exception) {
                    runtime.sequence().releaseAction(actionId);
                    plugin.getLogger().log(Level.WARNING, "Exploration delayed sequence failed: " + structureId, exception);
                    if (!"desert_pyramid".equals(record.structureType())) abandon(structureId);
                    else plugin.getLogger().warning("Pyramid delayed phase failed; progression remains fail-closed: " + structureId);
                }
            }
        }, Math.max(0L, delayTicks));
        runtime.sequence().trackTask(actionId, task);
        return true;
    }


    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) return "unknown";
        return location.getWorld().getName() + " "
                + Math.round(location.getX()) + "," + Math.round(location.getY()) + "," + Math.round(location.getZ());
    }

    public synchronized void onPhysicalMove(Player player, Location from, Location to, long currentTick) {
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) return;
        if (!from.getWorld().getUID().equals(to.getWorld().getUID())) return;
        for (ExplorationRuntime runtime : java.util.List.copyOf(active.values())) {
            StructureRecord record = repository.get(runtime.structureId()).orElse(null);
            if (record == null || !record.worldId().equals(to.getWorld().getUID())) continue;
            ExplorationStructureDefinition definition = registry.get(record.structureType()).orElse(null);
            if (definition == null) continue;
            if (runtime.participants().contains(player.getUniqueId())) {
                runtime.sequence().markPhysicalMove();
                try {
                    if (progressSequenceWait(record, runtime, currentTick)) continue;
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Exploration movement wait failed: " + record.structureId(), exception);
                    if (!"desert_pyramid".equals(record.structureType())) abandon(record.structureId());
                    continue;
                }
            }
            if (record.structureType().equals("desert_pyramid")
                    && crossesPyramidEntryBoundary(record, from, to, pyramidEntryPadding(definition))
                    && !runtime.raidStarted() && !runtime.choicePending()
                    && !Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-guardian-complete", "false"))
                    && !runtime.sequence().flag("pyramid.entry.prompted")
                    && !java.util.Set.of("spawn_pending", "active").contains(record.activationMetadata()
                    .getOrDefault("pyramid-guardian-encounter-state", "not_started"))) {
                runtime.addParticipant(player.getUniqueId());
                runtime.markPyramidEntry(player.getUniqueId(), to.getX() - from.getX(), to.getZ() - from.getZ());
                try {
                    StructureRecord latest = repository.get(record.structureId()).orElse(record);
                    repository.save(latest.withMetadata("pyramid-entry-actor", player.getUniqueId().toString())
                            .withMetadata("pyramid-entry-dx", Double.toString(to.getX() - from.getX()))
                            .withMetadata("pyramid-entry-dz", Double.toString(to.getZ() - from.getZ()))
                            .withMetadata("pyramid-guardian-encounter-state", "quiz_pending"));
                    executeNamedPhase(record, runtime, "pyramid_entry", currentTick);
                    executeNamedPhase(record, runtime, "pyramid_quiz", currentTick);
                    runtime.sequence().setFlag("pyramid.entry.prompted");
                    plugin.getLogger().info("Pyramid entry: structure=" + record.structureId() + ", player="
                            + player.getUniqueId() + ", from=" + formatLocation(from) + ", to=" + formatLocation(to));
                } catch (Exception exception) {
                    // Entry presentation/spawn failures are recoverable; do not terminally abandon a Pyramid.
                    plugin.getLogger().log(Level.WARNING, "Pyramid exterior entry sequence deferred: "
                            + record.structureId(), exception);
                }
                continue;
            }

            if (record.structureType().equals("pillager_outpost")
                    && runtime.lootTaken() && !runtime.raidStarted()) {
                if (runtime.lootExitPrompted()) continue;
                double limit = definition.lootTriggerRadius() * definition.lootTriggerRadius();
                boolean wasInside = distanceSquared(record, from) <= limit;
                boolean nowInside = distanceSquared(record, to) <= limit;
                if (nowInside) runtime.clearLootTriggerExit();
                else if (wasInside && !nowInside) runtime.markLootTriggerExit(currentTick);
                continue;
            }
            double limit = definition.combatAbandonRadius() * definition.combatAbandonRadius();
            boolean wasInside = distanceSquared(record, from) <= limit;
            boolean nowInside = distanceSquared(record, to) <= limit;
            if (nowInside) {
                runtime.addParticipant(player.getUniqueId());
                runtime.clearCombatAbandonExit();
                continue;
            }
            if (wasInside && !nowInside) {
                if (teleportExemptions.isExempt(record.structureId(), player.getUniqueId(), currentTick)) {
                    runtime.clearCombatAbandonExit();
                } else {
                    runtime.markCombatAbandonExit(currentTick);
                }
            }
        }
    }

    public synchronized void onTeleport(Player player, long currentTick) {
        for (ExplorationRuntime runtime : java.util.List.copyOf(active.values())) {
            if (!runtime.participants().contains(player.getUniqueId())) continue;
            teleportExemptions.exempt(runtime.structureId(), player.getUniqueId(), currentTick,
                    TELEPORT_EXEMPTION_TICKS);
            runtime.clearLootTriggerExit();
            runtime.clearCombatAbandonExit();
        }
    }

    public synchronized void shutdown() {
        for (ExplorationRuntime runtime : java.util.List.copyOf(active.values())) {
            safeCleanup(runtime);
            teleportExemptions.clear(runtime.structureId());
            recordEnd(runtime.structureId(), ExplorationEndReason.PLUGIN_DISABLE);
        }
        active.clear();
    }

    private void executePhase(StructureRecord record, ExplorationRuntime runtime,
                              ExplorationComponentPhase phase, long currentTick) throws Exception {
        executeNamedPhase(record, runtime, phase.name(), currentTick);
    }

    /** Explicit happy-path continuation invoked after a committed staged reveal. */
    public synchronized void continuePyramidPuzzle(ExplorationEventContext source) {
        if (source == null || !"desert_pyramid".equals(source.record().structureType())
                || source.runtime().tracker().isClosed()) return;
        StructureRecord record = repository.get(source.runtime().structureId()).orElse(source.record());
        Map<String, String> metadata = record.activationMetadata();
        if (Boolean.parseBoolean(metadata.getOrDefault("pyramid-underground-complete", "false"))
                || "RECOVERY_REQUIRED".equals(metadata.getOrDefault("pyramid-failure-state", ""))) return;
        ExplorationStructureDefinition definition = registry.get(record.structureType()).orElse(null);
        if (definition == null) return;
        StructureVariantDefinition variant = definition.variants().stream()
                .filter(candidate -> candidate.id().equals(record.variantId())).findFirst().orElse(null);
        if (variant == null) return;
        ExplorationEventContext context = new ExplorationEventContext(plugin, record, source.runtime(), ports,
                teleportExemptions, source.currentTick(), this::scheduleSequencePhase);
        int matched = 0;
        for (ExplorationComponentSpec spec : variant.components()) {
            if (!"pyramid_push_pillars".equalsIgnoreCase(spec.type())) continue;
            ExplorationComponent component = components.get(spec.type())
                    .orElseThrow(() -> new IllegalStateException("unknown exploration component " + spec.type()));
            try {
                component.execute(context, spec);
                matched++;
            } catch (Exception failure) {
                plugin.getLogger().log(Level.WARNING,
                        "Pyramid puzzle continuation deferred: structure=" + record.structureId(), failure);
            }
        }
        if (matched == 0) {
            plugin.getLogger().severe("Pyramid puzzle continuation matched zero pillar components: structure="
                    + record.structureId() + ", variant=" + record.variantId());
        }
    }

    /**
     * Dispatches a configured named phase without adding a scripting language.
     * A component either uses its existing default enum phase or an exact
     * literal phase identifier from its own definition.
     */
    private void executeNamedPhase(StructureRecord record, ExplorationRuntime runtime,
                                   String phase, long currentTick) throws Exception {
        ExplorationStructureDefinition definition = registry.get(record.structureType())
                .orElseThrow(() -> new IllegalStateException("missing structure definition " + record.structureType()));
        StructureVariantDefinition variant = definition.variants().stream()
                .filter(candidate -> candidate.id().equals(record.variantId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("missing variant " + record.variantId()));
        ExplorationEventContext context = new ExplorationEventContext(plugin, record, runtime, ports, teleportExemptions, currentTick,
                this::scheduleSequencePhase);
        int matched = 0;
        java.util.List<String> matchedTypes = new java.util.ArrayList<>();
        for (ExplorationComponentSpec spec : variant.components()) {
            ExplorationComponent component = components.get(spec.type())
                    .orElseThrow(() -> new IllegalStateException("unknown exploration component " + spec.type()));
            String configuredPhase = spec.string("phase", "").trim();
            boolean defaultPhaseMatch = configuredPhase.isEmpty()
                    && component.defaultPhase().name().equalsIgnoreCase(phase);
            boolean explicitPhaseMatch = !configuredPhase.isEmpty()
                    && configuredPhase.equalsIgnoreCase(phase);
            ExplorationComponentPhase configured = ExplorationComponentPhase.parse(configuredPhase, component.defaultPhase());
            boolean nextWaveRepeat = ExplorationComponentPhase.NEXT_WAVE.name().equalsIgnoreCase(phase)
                    && component.type().equals("raid_wave_spawn")
                    && spec.bool("repeat-on-next-wave", false)
                    && matchesSelectedRaidWave(runtime.selectedChoice(), configured);
            if (defaultPhaseMatch || explicitPhaseMatch || nextWaveRepeat) {
                matched++;
                matchedTypes.add(component.type());
                component.execute(context, spec);
            }
        }
        if (isRequiredPyramidPhase(record, phase) && matched == 0) {
            plugin.getLogger().severe("Pyramid required phase matched zero components: structure="
                    + record.structureId() + ", variant=" + record.variantId() + ", phase=" + phase
                    + ", matchedTypes=" + matchedTypes);
            throw new IllegalStateException("required Pyramid phase has no matching components: " + phase);
        }
    }

    private boolean isRequiredPyramidPhase(StructureRecord record, String phase) {
        if (!"desert_pyramid".equals(record.structureType())) return false;
        return java.util.Set.of("pyramid_entry", "pyramid_quiz", "pyramid_guardian_spawn",
                "pyramid_loot_trigger", "pyramid_room_reveal", "clear")
                .contains(phase.trim().toLowerCase(java.util.Locale.ROOT));
    }

    static boolean matchesSelectedRaidWave(String selectedChoice, ExplorationComponentPhase configured) {
        return switch (selectedChoice == null ? "" : selectedChoice.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "tier1" -> configured == ExplorationComponentPhase.CHOICE_TIER_1;
            case "tier2" -> configured == ExplorationComponentPhase.CHOICE_TIER_2;
            case "tier3" -> configured == ExplorationComponentPhase.CHOICE_TIER_3;
            default -> false;
        };
    }

    private void announceNextRaidWave(ExplorationRuntime runtime) {
        Player target = runtime.raidTarget() == null ? null : Bukkit.getPlayer(runtime.raidTarget());
        if (target == null || !target.isOnline() || target.isDead()) return;
        target.sendMessage(net.kyori.adventure.text.Component.text("더 많은 약탈자들이 몰려옵니다. 다음 습격을 준비하십시오."));
        target.playSound(target.getLocation(), Sound.ENTITY_PILLAGER_AMBIENT, 1.0F, 0.75F);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0D, 1.0D, 0.0D), 20,
                1.25D, 0.5D, 1.25D, 0.02D);
    }

    private ExplorationComponentPhase phaseForChoice(String choice) {
        return switch (normalizeChoice(choice)) {
            case "tier1" -> ExplorationComponentPhase.CHOICE_TIER_1;
            case "tier2" -> ExplorationComponentPhase.CHOICE_TIER_2;
            case "tier3" -> ExplorationComponentPhase.CHOICE_TIER_3;
            default -> throw new IllegalArgumentException("unsupported exploration choice: " + choice);
        };
    }

    private void recoverPyramidGuardianEncounter(StructureRecord record, ExplorationRuntime runtime, long currentTick) {
        if (!"desert_pyramid".equals(record.structureType())
                || Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-guardian-complete", "false"))) return;
        String state = record.activationMetadata().getOrDefault("pyramid-guardian-encounter-state", "not_started");
        if (!Set.of("spawn_pending", "active").contains(state)) return;
        UUID actor = runtime.entryActor();
        Player player = actor == null ? null : Bukkit.getPlayer(actor);
        if (player == null || !player.isOnline() || player.isDead()) {
            try {
                repository.save(record.withMetadata("pyramid-guardian-encounter-state", "not_started")
                        .withMetadata("pyramid-guardian-started", null)
                        .withMetadata("pyramid-guardian-spawned", null));
                runtime.sequence().clearFlag("pyramid.entry.prompted");
                plugin.getLogger().info("Pyramid guardian owner unavailable; encounter re-armed for next entry: " + record.structureId());
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to re-arm Pyramid guardian after restart: " + record.structureId(), exception);
            }
            return;
        }
        try {
            executeNamedPhase(record, runtime, "pyramid_guardian_spawn", currentTick);
            if (!runtime.objectiveEntities().isEmpty()) {
                runtime.markRaidStarted();
                repository.save(repository.get(record.structureId()).orElse(record)
                        .withMetadata("pyramid-guardian-encounter-state", "active")
                        .withMetadata("pyramid-guardian-started", "true"));
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Pyramid guardian restart recovery deferred: " + record.structureId(), exception);
        }
    }

    /** Restart recovery delegates only pending/legacy completion evidence to the live pillar transaction. */
    private StructureRecord reconcilePendingPyramidUndergroundCompletion(StructureRecord record) throws IOException {
        boolean complete = Boolean.parseBoolean(record.activationMetadata()
                .getOrDefault("pyramid-underground-complete", "false"));
        PyramidUndergroundCompletionState state = PyramidUndergroundCompletionState.parse(record.activationMetadata()
                .get("pyramid-underground-completion-state"));
        if (!complete && state != PyramidUndergroundCompletionState.COMPLETION_PENDING) {
            return record;
        }
        var reconciled = PyramidUndergroundCompletionCoordinator.complete(repository, record.structureId());
        if (reconciled != null && reconciled != record
                && Boolean.parseBoolean(reconciled.activationMetadata()
                .getOrDefault("pyramid-underground-complete", "false"))) {
            plugin.getLogger().info("Recovered pending Pyramid underground completion: structure=" + record.structureId());
        }
        return reconciled == null ? record : reconciled;
    }

    private StructureRecord migratePyramidRecord(StructureRecord record) throws IOException {
        int version;
        try { version = Integer.parseInt(record.activationMetadata().getOrDefault("pyramid-content-version", "0")); }
        catch (NumberFormatException ignored) { version = 0; }
        boolean obsoleteRadius = "3".equals(record.activationMetadata().get("pyramid-room-radius"));
        if (version >= CURRENT_PYRAMID_CONTENT_VERSION && !obsoleteRadius) return record;
        StructureRecord migrated = record.withMetadata("pyramid-content-version", Integer.toString(CURRENT_PYRAMID_CONTENT_VERSION));
        if ("3".equals(record.activationMetadata().get("pyramid-room-radius"))) {
            for (String key : List.of("pyramid-room-origin", "pyramid-room-radius", "pyramid-room-height",
                    "pyramid-room-prepared", "pyramid-room-created", "pyramid-room-created-at")) {
                migrated = migrated.withMetadata(key, null);
            }
            plugin.getLogger().warning("Migrated obsolete Pyramid geometry: structure="
                    + record.structureId() + ", radius=3 reset to retryable state");
        }
        if (!migrated.equals(record)) repository.save(migrated);
        return migrated;
    }

    private void restorePyramidState(StructureRecord record, ExplorationRuntime runtime, Player trigger) {
        if (!"desert_pyramid".equals(record.structureType())) return;
        UUID actor = parseUuid(record.activationMetadata().get("pyramid-entry-actor"));
        if (actor != null) {
            runtime.restorePyramidEntryActor(actor);
            runtime.addParticipant(actor);
        }
        String dx = record.activationMetadata().get("pyramid-entry-dx");
        String dz = record.activationMetadata().get("pyramid-entry-dz");
        if (dx != null && dz != null) {
            try { runtime.restorePyramidEntryDirection(Double.parseDouble(dx), Double.parseDouble(dz)); }
            catch (NumberFormatException ignored) { }
        }
        if (Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-guardian-complete", "false"))) {
            runtime.sequence().setFlag("pyramid.guardian.complete");
        }
        if (Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-underground-complete", "false"))) {
            runtime.sequence().setFlag("pyramid.underground.complete");
            runtime.sequence().setFlag("pyramid.puzzle.solved");
        }
    }

    private void restoreLootState(StructureRecord record, ExplorationRuntime runtime) {
        if (!Boolean.parseBoolean(record.activationMetadata().getOrDefault("loot-taken", "false"))) return;
        UUID looter = parseUuid(record.activationMetadata().get("looter"));
        if (looter != null) {
            runtime.addParticipant(looter);
            runtime.markLootTaken(looter, parseLong(record.activationMetadata().get("loot-taken-at-tick"), 0L));
        }
        Location origin = restoreLocation(record);
        if (origin != null) runtime.snapshotRaidOrigin(origin);
    }

    private StructureRecord withRaidOriginMetadata(StructureRecord record, Location origin) {
        if (origin == null || origin.getWorld() == null) return record;
        return record.withMetadata("raid-origin-x", Double.toString(origin.getX()))
                .withMetadata("raid-origin-y", Double.toString(origin.getY()))
                .withMetadata("raid-origin-z", Double.toString(origin.getZ()));
    }

    private Location restoreLocation(StructureRecord record) {
        String x = record.activationMetadata().get("raid-origin-x");
        String y = record.activationMetadata().get("raid-origin-y");
        String z = record.activationMetadata().get("raid-origin-z");
        if (x == null || y == null || z == null) return null;
        try {
            org.bukkit.World world = Bukkit.getWorld(record.worldId());
            return world == null ? null : new Location(world, Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private UUID parseUuid(String raw) {
        try { return raw == null ? null : UUID.fromString(raw); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private long parseLong(String raw, long fallback) {
        try { return raw == null ? fallback : Long.parseLong(raw); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private String normalizeChoice(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean hasRewardPhase(StructureRecord record) {
        ExplorationStructureDefinition definition = registry.get(record.structureType()).orElse(null);
        if (definition == null) return false;
        return definition.variants().stream().filter(v -> v.id().equals(record.variantId())).findFirst()
                .map(v -> v.components().stream().anyMatch(spec -> spec.type().equals("reward_drop")))
                .orElse(false);
    }

    private double distanceSquared(StructureRecord record, Location location) {
        return record.bounds().distanceSquaredTo(location.getX(), location.getY(), location.getZ());
    }

    private void recordEnd(UUID structureId, ExplorationEndReason reason) {
        if (structureId != null && reason != null) lastEndReasons.put(structureId, reason);
    }

    private void safeCleanup(ExplorationRuntime runtime) {
        runtime.sequence().cancelPendingTasks();
        try { runtime.tracker().cleanup(); }
        catch (RuntimeException exception) { plugin.getLogger().log(Level.WARNING, "Exploration cleanup had an error", exception); }
    }
}
