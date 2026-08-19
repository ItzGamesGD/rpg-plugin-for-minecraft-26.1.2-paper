package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentRegistry;
import com.hyunseo.hyunseorpg.exploration.component.impl.DisplayTargetComponent;
import com.hyunseo.hyunseorpg.exploration.component.impl.ForcedRelocationComponent;
import com.hyunseo.hyunseorpg.exploration.component.impl.InteractionTargetComponent;
import com.hyunseo.hyunseorpg.exploration.component.impl.PuzzleComponent;
import com.hyunseo.hyunseorpg.exploration.component.impl.RewardDropComponent;
import com.hyunseo.hyunseorpg.exploration.component.impl.ScriptedSpawnComponent;
import com.hyunseo.hyunseorpg.exploration.component.impl.TemporarySealComponent;
import com.hyunseo.hyunseorpg.exploration.detection.DeterministicStructureSelector;
import com.hyunseo.hyunseorpg.exploration.detection.ReflectivePaperStructureCandidateProvider;
import com.hyunseo.hyunseorpg.exploration.detection.StructureCandidateProvider;
import com.hyunseo.hyunseorpg.exploration.detection.StructureDetectionService;
import com.hyunseo.hyunseorpg.exploration.integration.BukkitExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.listener.ChunkLoadExplorationListener;
import com.hyunseo.hyunseorpg.exploration.listener.ExplorationPlayerMovementListener;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureIndex;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.persistence.YamlStructureStorage;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationRegistry;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationHeartbeatTask;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import com.hyunseo.hyunseorpg.exploration.runtime.TeleportExemptionService;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Drop-in exploration package bootstrap. HyunseoRPGPlugin only needs to own/start/stop this object.
 * No farming/alchemy dependency is required.
 */
public final class ExplorationModule {
    private final JavaPlugin plugin;
    private final ExplorationRegistry registry;
    private final StructureRepository repository;
    private final StructureDetectionService detection;
    private final ExplorationRuntimeManager runtimes;
    private final AtomicLong tickCounter = new AtomicLong();
    private final List<Listener> listeners;
    private BukkitTask heartbeatTask;

    public ExplorationModule(JavaPlugin plugin) {
        this(plugin, null, null);
    }

    public ExplorationModule(JavaPlugin plugin, ExplorationPorts ports, StructureCandidateProvider candidateProvider) {
        this.plugin = plugin;
        this.registry = new ExplorationRegistry(plugin);
        this.repository = new StructureRepository(new YamlStructureStorage(plugin), new StructureIndex());
        ExplorationPorts effectivePorts = ports == null ? BukkitExplorationPorts.safeDefaults(plugin) : ports;
        StructureCandidateProvider effectiveProvider = candidateProvider == null
                ? new ReflectivePaperStructureCandidateProvider(plugin) : candidateProvider;
        this.detection = new StructureDetectionService(plugin, registry, effectiveProvider, repository,
                new DeterministicStructureSelector());
        ExplorationComponentRegistry componentRegistry = defaultComponents();
        this.runtimes = new ExplorationRuntimeManager(plugin, registry, repository, componentRegistry,
                effectivePorts, new TeleportExemptionService());
        this.listeners = List.of(
                new ChunkLoadExplorationListener(detection),
                new ExplorationPlayerMovementListener(runtimes, tickCounter));
    }

    public boolean start() {
        if (!registry.load()) return false;
        if (!registry.isEnabled()) {
            plugin.getLogger().info("Exploration module is installed but disabled in exploration/structures.yml.");
            return true;
        }
        try {
            for (var world : Bukkit.getWorlds()) repository.ensureWorldLoaded(world.getUID());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load exploration persistence", exception);
            return false;
        }
        listeners.forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(plugin,
                new ExplorationHeartbeatTask(registry, repository, runtimes, tickCounter),
                registry.heartbeatTicks(), registry.heartbeatTicks());
        return true;
    }

    public void stop() {
        if (heartbeatTask != null) heartbeatTask.cancel();
        heartbeatTask = null;
        listeners.forEach(HandlerList::unregisterAll);
        runtimes.shutdown();
        try { repository.flushAll(); }
        catch (IOException exception) { plugin.getLogger().log(Level.SEVERE, "Unable to flush exploration persistence", exception); }
    }

    /** Reloads definitions only. Existing StructureRecord selection/variant/state are never rerolled. */
    public boolean reload() { return registry.load(); }

    public boolean complete(UUID structureId) { return runtimes.complete(structureId, tickCounter.get()); }
    public int scanChunk(org.bukkit.World world, int chunkX, int chunkZ) { return detection.scanChunk(world, chunkX, chunkZ); }
    public ExplorationRegistry registry() { return registry; }
    public StructureRepository repository() { return repository; }
    public ExplorationRuntimeManager runtimes() { return runtimes; }

    private ExplorationComponentRegistry defaultComponents() {
        return new ExplorationComponentRegistry()
                .register(new ScriptedSpawnComponent())
                .register(new DisplayTargetComponent())
                .register(new InteractionTargetComponent())
                .register(new TemporarySealComponent())
                .register(new ForcedRelocationComponent())
                .register(new RewardDropComponent())
                .register(new PuzzleComponent());
    }
}
