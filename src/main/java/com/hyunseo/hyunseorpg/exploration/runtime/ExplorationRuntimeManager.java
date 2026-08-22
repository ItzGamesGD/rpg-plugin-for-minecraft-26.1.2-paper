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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** E3-E5 runtime coordinator. Persistent state transition always precedes runtime activation. */
public final class ExplorationRuntimeManager {
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

    /** Builds a non-mutating diagnostic view for one persistent structure. */
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
            existing.clearLootTriggerExit();
            existing.clearCombatAbandonExit();
            lastEndReasons.remove(record.structureId());
            return true;
        }
        StructureRecord persistent = record;
        try {
            if (record.state() == StructureEventState.UNDISCOVERED) {
                persistent = record.transitionTo(StructureEventState.ACTIVE, Instant.now())
                        .withMetadata("activated-by", trigger.getUniqueId().toString())
                        .withMetadata("activated-at", Long.toString(System.currentTimeMillis()));
                repository.save(persistent);
            }
            ExplorationRuntime runtime = new ExplorationRuntime(persistent.structureId(), persistent.variantId(), currentTick);
            runtime.addParticipant(trigger.getUniqueId());
            restoreLootState(persistent, runtime);
            active.put(persistent.structureId(), runtime);
            lastEndReasons.remove(persistent.structureId());
            plugin.getLogger().info("Exploration activation: structure=" + persistent.structureType()
                    + ", variant=" + persistent.variantId() + ", id=" + persistent.structureId());
            executePhase(persistent, runtime, ExplorationComponentPhase.ACTIVATE, currentTick);
            if (runtime.lootTaken()
                    && Boolean.parseBoolean(persistent.activationMetadata().getOrDefault("loot-exit-prompted", "false"))) {
                executePhase(persistent, runtime, ExplorationComponentPhase.LOOT_EXIT, currentTick);
                runtime.markLootExitPrompted();
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
            executePhase(record, runtime, ExplorationComponentPhase.CLEAR, currentTick);
            StructureRecord completed = record.transitionTo(StructureEventState.CLEARED, Instant.now());
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
            executePhase(record, runtime, phaseForChoice(choice), currentTick);
            if (!runtime.objectiveMode() || runtime.objectiveEntities().isEmpty()) {
                throw new IllegalStateException("raid choice produced no objective entities");
            }
            runtime.markRaidStarted();
            player.sendMessage(net.kyori.adventure.text.Component.text("약탈자 전초기지 습격이 시작되었습니다."));
            return ChoiceResult.ACCEPTED;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Exploration choice failed for " + structureId + ", choice=" + choice, exception);
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
            if (!record.structureType().equals("pillager_outpost")
                    || !record.bounds().contains(container.getX(), container.getY(), container.getZ())) continue;
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
                repository.save(updated);
                marked = true;
                plugin.getLogger().info("Exploration outpost loot armed: structure=" + record.structureId()
                        + ", looter=" + player.getUniqueId());
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to persist outpost loot state: " + record.structureId(), exception);
            }
        }
        return marked;
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

            if (runtime.choiceExpired(currentTick)) {
                plugin.getLogger().info("Exploration choice timed out: structure=" + record.structureType()
                        + ", id=" + record.structureId() + ", default=" + runtime.defaultChoice());
                abandon(record.structureId());
                continue;
            }

            if (runtime.lootTaken() && !runtime.lootExitPrompted() && !runtime.raidStarted()) {
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
                    if (runtime.hasNextRaidWave()) {
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

    /** Marks an objective dead only from an explicit death event; unload is intentionally ignored. */
    public synchronized boolean confirmObjectiveDeath(UUID entityId) {
        if (entityId == null) return false;
        boolean confirmed = false;
        for (ExplorationRuntime runtime : active.values()) {
            if (runtime.confirmObjectiveDeath(entityId)) {
                confirmed = true;
                plugin.getLogger().info("Exploration objective death event accepted: structure="
                        + runtime.structureId() + ", entity=" + entityId);
            }
        }
        return confirmed;
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
            if (runtime.lootTaken() && !runtime.raidStarted()) {
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
        ExplorationStructureDefinition definition = registry.get(record.structureType())
                .orElseThrow(() -> new IllegalStateException("missing structure definition " + record.structureType()));
        StructureVariantDefinition variant = definition.variants().stream()
                .filter(candidate -> candidate.id().equals(record.variantId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("missing variant " + record.variantId()));
        ExplorationEventContext context = new ExplorationEventContext(plugin, record, runtime, ports, teleportExemptions, currentTick);
        for (ExplorationComponentSpec spec : variant.components()) {
            ExplorationComponent component = components.get(spec.type())
                    .orElseThrow(() -> new IllegalStateException("unknown exploration component " + spec.type()));
            ExplorationComponentPhase configured = ExplorationComponentPhase.parse(spec.string("phase", ""), component.defaultPhase());
            boolean nextWaveRepeat = phase == ExplorationComponentPhase.NEXT_WAVE
                    && component.type().equals("raid_wave_spawn")
                    && spec.bool("repeat-on-next-wave", false);
            if (configured == phase || nextWaveRepeat) component.execute(context, spec);
        }
    }

    private ExplorationComponentPhase phaseForChoice(String choice) {
        return switch (normalizeChoice(choice)) {
            case "tier1" -> ExplorationComponentPhase.CHOICE_TIER_1;
            case "tier2" -> ExplorationComponentPhase.CHOICE_TIER_2;
            case "tier3" -> ExplorationComponentPhase.CHOICE_TIER_3;
            default -> throw new IllegalArgumentException("unsupported exploration choice: " + choice);
        };
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
        try { runtime.tracker().cleanup(); }
        catch (RuntimeException exception) { plugin.getLogger().log(Level.WARNING, "Exploration cleanup had an error", exception); }
    }
}
