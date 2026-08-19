package com.hyunseo.hyunseorpg.exploration.detection;

import com.hyunseo.hyunseorpg.exploration.model.StructureCandidate;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationRegistry;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.StructureVariantDefinition;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/** E1: observes structures, permanently fixes RPG selection/variant, persists before activation. */
public final class StructureDetectionService {
    private final JavaPlugin plugin;
    private final ExplorationRegistry registry;
    private final StructureCandidateProvider provider;
    private final StructureRepository repository;
    private final DeterministicStructureSelector selector;

    public StructureDetectionService(JavaPlugin plugin, ExplorationRegistry registry,
                                     StructureCandidateProvider provider, StructureRepository repository,
                                     DeterministicStructureSelector selector) {
        this.plugin = plugin;
        this.registry = registry;
        this.provider = provider;
        this.repository = repository;
        this.selector = selector;
    }

    public int scanChunk(World world, int chunkX, int chunkZ) {
        if (!registry.isEnabled()) return 0;
        try {
            repository.ensureWorldLoaded(world.getUID());
            List<StructureCandidate> candidates = provider.scanChunk(world, chunkX, chunkZ, registry.minecraftKeys());
            int created = 0;
            for (StructureCandidate candidate : candidates) {
                ExplorationStructureDefinition definition = registry.byMinecraftKey(candidate.minecraftKey()).orElse(null);
                if (definition == null || !definition.enabled()) continue;
                UUID id = selector.stableId(candidate);
                if (repository.get(id).isPresent()) continue;
                boolean selected = selector.selected(id, definition.selectionChance());
                String variantId = "";
                StructureEventState state = StructureEventState.VANILLA;
                if (selected) {
                    StructureVariantDefinition variant = selector.chooseVariant(id, definition);
                    variantId = variant.id();
                    state = StructureEventState.UNDISCOVERED;
                }
                StructureRecord record = new StructureRecord(id, candidate.worldId(), definition.id(),
                        candidate.minecraftKey(), candidate.anchor(), candidate.bounds(), selected, variantId, state,
                        java.util.Map.of(), false, Instant.now(), null, StructureRecord.CURRENT_DATA_VERSION);
                if (repository.createIfAbsent(record)) created++;
            }
            return created;
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Exploration chunk scan failed safely at " + chunkX + "," + chunkZ, exception);
            return 0;
        }
    }
}
