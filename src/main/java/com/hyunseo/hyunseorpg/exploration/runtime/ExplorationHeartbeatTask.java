package com.hyunseo.hyunseorpg.exploration.runtime;

import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicLong;

/** Lazy proximity activation; no per-structure repeating task is created. */
public final class ExplorationHeartbeatTask implements Runnable {
    private final ExplorationRegistry registry;
    private final StructureRepository repository;
    private final ExplorationRuntimeManager runtimes;
    private final AtomicLong tickCounter;
    private final ExplorationTriggerPolicy triggerPolicy = new ExplorationTriggerPolicy();

    public ExplorationHeartbeatTask(ExplorationRegistry registry, StructureRepository repository,
                                    ExplorationRuntimeManager runtimes, AtomicLong tickCounter) {
        this.registry = registry;
        this.repository = repository;
        this.runtimes = runtimes;
        this.tickCounter = tickCounter;
    }

    @Override
    public void run() {
        long tick = tickCounter.addAndGet(registry.heartbeatTicks());
        double search = registry.all().stream().mapToDouble(def -> def.triggerRadius()).max().orElse(48.0D) + 32.0D;
        for (Player player : Bukkit.getOnlinePlayers()) {
            var location = player.getLocation();
            for (var record : repository.index().nearby(player.getWorld().getUID(), location.getX(), location.getZ(), search)) {
                if (record.state() != StructureEventState.UNDISCOVERED && record.state() != StructureEventState.ACTIVE) continue;
                var definition = registry.get(record.structureType()).orElse(null);
                if (definition == null) continue;
                if (triggerPolicy.isEligible(record, definition, player.getWorld().getUID(),
                        location.getX(), location.getY(), location.getZ())) {
                    runtimes.activate(record, player, tick);
                }
            }
        }
        runtimes.heartbeat(tick);
    }
}
