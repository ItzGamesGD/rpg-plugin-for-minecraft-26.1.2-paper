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
    private final JavaPlugin plugin;
    private final ExplorationRegistry registry;
    private final StructureRepository repository;
    private final ExplorationComponentRegistry components;
    private final ExplorationPorts ports;
    private final TeleportExemptionService teleportExemptions;
    private final Map<UUID, ExplorationRuntime> active = new LinkedHashMap<>();

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

    public synchronized boolean activate(StructureRecord record, Player trigger, long currentTick) {
        if (record.state().terminal()) return false;
        ExplorationRuntime existing = active.get(record.structureId());
        if (existing != null) {
            existing.addParticipant(trigger.getUniqueId());
            existing.clearPhysicalExit();
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
            ExplorationRuntime runtime = new ExplorationRuntime(persistent.structureId(), persistent.variantId());
            runtime.addParticipant(trigger.getUniqueId());
            active.put(persistent.structureId(), runtime);
            executePhase(persistent, runtime, ExplorationComponentPhase.ACTIVATE, currentTick);
            return true;
        } catch (Exception exception) {
            ExplorationRuntime failed = active.remove(record.structureId());
            if (failed != null) safeCleanup(failed);
            plugin.getLogger().log(Level.SEVERE, "Exploration activation failed for " + record.structureId(), exception);
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
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Exploration completion failed for " + structureId + "; state stays ACTIVE", exception);
            return false;
        }
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
            return true;
        } catch (IOException exception) {
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
                continue;
            }
            ExplorationStructureDefinition definition = registry.get(record.structureType()).orElse(null);
            if (definition == null) continue;

            if (runtime.objectiveMode()) {
                for (UUID entityId : runtime.objectiveEntities()) {
                    var entity = Bukkit.getEntity(entityId);
                    if (entity == null || !entity.isValid() || entity.isDead()) runtime.removeObjective(entityId);
                }
                if (runtime.objectiveEntities().isEmpty()) {
                    complete(record.structureId(), currentTick);
                    continue;
                }
            }

            boolean anyInside = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().getUID().equals(record.worldId())) continue;
                if (distanceSquared(record, player.getLocation()) <= definition.abandonRadius() * definition.abandonRadius()) {
                    anyInside = true;
                    runtime.addParticipant(player.getUniqueId());
                    break;
                }
            }
            if (anyInside) {
                runtime.clearPhysicalExit();
            } else if (runtime.physicalExitAtTick() != null
                    && currentTick - runtime.physicalExitAtTick() >= definition.abandonGraceTicks()) {
                abandon(record.structureId());
            }
        }
    }

    public synchronized void onPhysicalMove(Player player, Location from, Location to, long currentTick) {
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) return;
        if (!from.getWorld().getUID().equals(to.getWorld().getUID())) return;
        for (ExplorationRuntime runtime : java.util.List.copyOf(active.values())) {
            StructureRecord record = repository.get(runtime.structureId()).orElse(null);
            if (record == null || !record.worldId().equals(to.getWorld().getUID())) continue;
            ExplorationStructureDefinition definition = registry.get(record.structureType()).orElse(null);
            if (definition == null) continue;
            double limit = definition.abandonRadius() * definition.abandonRadius();
            boolean wasInside = distanceSquared(record, from) <= limit;
            boolean nowInside = distanceSquared(record, to) <= limit;
            if (nowInside) {
                runtime.addParticipant(player.getUniqueId());
                runtime.clearPhysicalExit();
                continue;
            }
            if (wasInside && !nowInside) {
                if (teleportExemptions.isExempt(record.structureId(), player.getUniqueId(), currentTick)) {
                    runtime.clearPhysicalExit();
                } else {
                    runtime.markPhysicalExit(currentTick);
                }
            }
        }
    }

    public synchronized void onTeleport(Player player, long currentTick) {
        for (ExplorationRuntime runtime : java.util.List.copyOf(active.values())) {
            if (!runtime.participants().contains(player.getUniqueId())) continue;
            teleportExemptions.exempt(runtime.structureId(), player.getUniqueId(), currentTick, 40L);
            runtime.clearPhysicalExit();
        }
    }

    public synchronized void shutdown() {
        for (ExplorationRuntime runtime : java.util.List.copyOf(active.values())) safeCleanup(runtime);
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
            if (configured == phase) component.execute(context, spec);
        }
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

    private void safeCleanup(ExplorationRuntime runtime) {
        try { runtime.tracker().cleanup(); }
        catch (RuntimeException exception) { plugin.getLogger().log(Level.WARNING, "Exploration cleanup had an error", exception); }
    }
}
