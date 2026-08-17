package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Stage 1 native crop engine: planting, scheduled growth, and chunk persistence. */
public final class CropGrowthService implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final RPGItemService itemService;
    private final CropRegistry registry;
    private final CropIndex index;
    private final CropRepresentationResolver representationResolver;
    private final FarmingProfileService farmingProfileService;
    private final CropStorage storage;
    private final CropBlockAdapter blockAdapter;
    private final HarvestService harvestService;
    private final Set<CropIndex.ChunkKey> dirtyChunks = new HashSet<>();
    private BukkitTask growthTask;
    private BukkitTask saveTask;
    private boolean started;
    private volatile Boolean manualDebugEvents;

    public CropGrowthService(JavaPlugin plugin, ConfigService config, RPGItemService itemService,
                             CropRegistry registry, CropIndex index, CropStorage storage,
                             CropBlockAdapter blockAdapter, FarmingProfileService farmingProfileService,
                             InventoryDeliveryService delivery) {
        this.plugin = plugin;
        this.config = config;
        this.itemService = itemService;
        this.registry = registry;
        this.index = index;
        this.representationResolver = new CropRepresentationResolver(index, registry::get);
        this.storage = storage;
        this.blockAdapter = blockAdapter;
        this.farmingProfileService = farmingProfileService;
        this.harvestService = new HarvestService(plugin, config, itemService, delivery, farmingProfileService,
                registry, index, representationResolver, blockAdapter, this::markDirty);
    }

    public boolean reload() {
        boolean loaded = registry.load();
        if (!loaded) {
            return false;
        }
        harvestService.validateConfiguration();
        if (started && growthTask == null) {
            restoreLoadedChunks();
            startTasks();
        }
        return true;
    }

    public void start() {
        if (started) return;
        started = true;
        if (!registry.load()) return;
        harvestService.validateConfiguration();
        restoreLoadedChunks();
        startTasks();
    }

    private void startTasks() {
        long tickSeconds = Math.max(1L, config.getFarmingGrowthLong("scheduler.tick-seconds", 1L));
        long saveSeconds = Math.max(1L, config.getFarmingGrowthLong("scheduler.save-seconds", 15L));
        growthTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickGrowth,
                tickSeconds * 20L, tickSeconds * 20L);
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveDirtyChunks,
                saveSeconds * 20L, saveSeconds * 20L);
    }

    public void shutdown() {
        stopTasks();
        saveAllLoadedChunks();
        try {
            storage.close();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to close farming crop storage", exception);
        }
        started = false;
    }

    public CropHarvestValidator harvestValidator() {
        return new CropHarvestValidator(representationResolver);
    }

    public CropIndex index() {
        return index;
    }

    public CropRegistry registry() {
        return registry;
    }

    public HarvestService harvestService() {
        return harvestService;
    }

    public int loadedCropCount() {
        return index.cropCount();
    }

    public int loadedChunkCount() {
        return index.loadedChunkCount();
    }

    public boolean debugEventsEnabled() {
        return manualDebugEvents != null
                ? manualDebugEvents
                : config.getFarmingGrowthBoolean("debug.events", false);
    }

    /** Runtime-only diagnostic switch; it never writes farming configuration. */
    public void setDebugEvents(boolean enabled) {
        manualDebugEvents = enabled;
    }

    /**
     * Repairs one loaded-world chunk through the normal storage and representation
     * boundaries. No world-wide scan or asynchronous Bukkit access is allowed.
     */
    public RepairResult repairChunk(World world, int chunkX, int chunkZ) {
        if (!Bukkit.isPrimaryThread()) {
            return RepairResult.failure("repairchunk must run on the Bukkit main thread");
        }
        if (world == null) return RepairResult.failure("world not found");
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        CropIndex.ChunkKey key = new CropIndex.ChunkKey(world.getUID(), chunkX, chunkZ);
        int storedBefore;
        try {
            storedBefore = storage.load(world.getUID(), chunkX, chunkZ).crops().size();
        } catch (IOException exception) {
            return RepairResult.failure("storage load failed: " + exception.getMessage());
        }
        if (index.isChunkLoaded(world.getUID(), chunkX, chunkZ)) {
            unloadChunk(chunk);
            if (index.isChunkLoaded(world.getUID(), chunkX, chunkZ)) {
                return RepairResult.failure("current in-memory chunk could not be flushed");
            }
        }
        loadChunk(chunk);
        try {
            storage.load(world.getUID(), chunkX, chunkZ);
        } catch (IOException exception) {
            return RepairResult.failure("storage reload failed; existing data was preserved: "
                    + exception.getMessage());
        }
        List<CropInstance> repaired = index.getChunk(world.getUID(), chunkX, chunkZ);
        saveChunk(key, repaired);
        return new RepairResult(true, storedBefore, repaired.size(),
                Math.max(0, storedBefore - repaired.size()), "chunk repaired");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlant(PlayerInteractEvent event) {
        if (!started || !registry.isEnabled() || event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block soil = event.getClickedBlock();
        if (soil == null || soil.getType() == Material.AIR) return;
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        String itemId = itemService.getItemId(held).orElse("");
        CropDefinition definition = registry.findBySeed(itemId).orElse(null);
        if (definition == null) return;
        if (!farmingProfileService.isReady(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("농사 데이터가 아직 준비되지 않았습니다. 잠시 후 다시 시도하세요.");
            return;
        }
        if (!farmingProfileService.isCropUnlocked(event.getPlayer(), definition.id())) {
            event.setCancelled(true);
            String required = farmingProfileService.requiredStageForCrop(definition.id())
                    .map(FarmingStage::displayName)
                    .orElse("알 수 없는 단계");
            event.getPlayer().sendMessage("아직 해금하지 않은 작물입니다. 필요 농사 단계: " + required);
            return;
        }
        Block target = soil.getRelative(0, 1, 0);
        CropPosition position = CropPosition.of(target);
        if (!blockAdapter.canPlant(soil, target, definition)
                || index.findByBlock(position).isPresent()) return;

        long now = System.currentTimeMillis();
        if (!blockAdapter.applyStage(target, definition, 0)) return;
        CropInstance instance = new CropInstance(definition.id(), position, 0, now,
                now + definition.secondsPerStage() * 1000L, definition.dataVersion());
        if (!index.register(instance)) {
            blockAdapter.clearRepresentation(target, definition);
            return;
        }
        consumeOne(held, event.getPlayer());
        markDirty(position);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        if (isRegisteredCrop(event.getBlock())) {
            harvestService.handleDirect(event.getPlayer(), event.getBlock(),
                    event.getPlayer().getInventory().getItemInMainHand());
            event.setCancelled(true);
            event.setDropItems(false);
            return;
        }
        if (event.getBlock().getType() == Material.FARMLAND) {
            IndirectHarvestResult result = harvestService.handleIndirect(event.getBlock(), HarvestCause.SOIL_LOSS);
            CropPosition removed = result == IndirectHarvestResult.REMOVED
                    ? CropPosition.of(event.getBlock().getRelative(0, 1, 0)) : null;
            debugEvent("BlockBreakEvent", event.getBlock(), null,
                    Material.FARMLAND, Material.AIR, removed);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (isRegisteredCrop(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        if (event.getBlocks().stream().anyMatch(state -> isRegisteredCrop(state.getBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getType() == Material.FARMLAND) {
            Material after = event.getNewState().getType();
            CropPosition removed = null;
            if (!isSupportedSoil(event.getBlock(), after)) {
                IndirectHarvestResult result = harvestService.handleIndirect(event.getBlock(), HarvestCause.SOIL_LOSS);
                if (result == IndirectHarvestResult.REMOVED) {
                    removed = CropPosition.of(event.getBlock().getRelative(0, 1, 0));
                }
            }
            debugEvent("BlockFadeEvent", event.getBlock(), null,
                    Material.FARMLAND, after, removed);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (isRegisteredCrop(event.getBlock())) {
            CropPosition position = CropPosition.of(event.getBlock());
            CropInstance instance = representationResolver.find(position).orElse(null);
            CropDefinition definition = instance == null ? null : registry.get(instance.cropId()).orElse(null);
            Block canonical = instance == null ? event.getBlock() : event.getBlock().getWorld().getBlockAt(
                    instance.position().x(), instance.position().y(), instance.position().z());
            boolean validSoil = definition != null
                    && definition.soil().contains(canonical.getRelative(0, -1, 0).getType());
            boolean validRepresentation = definition != null
                    && blockAdapter.isRepresentation(canonical, definition);
            boolean completeRepresentation = definition != null
                    && blockAdapter.isCompleteRepresentation(canonical, definition);
            if (instance == null || definition == null || !validSoil || !validRepresentation) {
                CropPosition removed = clearCrop(event.getBlock());
                debugEvent("BlockPhysicsEvent", event.getBlock(), event.getSourceBlock(),
                        event.getBlock().getType(), null, removed);
            } else if (!completeRepresentation && !blockAdapter.applyStage(canonical, definition, instance.stage())) {
                CropPosition removed = clearCrop(event.getBlock());
                debugEvent("BlockPhysicsEvent", event.getBlock(), event.getSourceBlock(),
                        event.getBlock().getType(), null, removed);
            } else {
                if (!completeRepresentation) markDirty(instance.position());
                debugEvent("BlockPhysicsEvent", event.getBlock(), event.getSourceBlock(),
                        event.getBlock().getType(), event.getBlock().getType(), null);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getBlock().getType() != Material.FARMLAND || event.getTo() == Material.FARMLAND) return;
        IndirectHarvestResult result = harvestService.handleIndirect(event.getBlock(), HarvestCause.SOIL_LOSS);
        CropPosition removed = result == IndirectHarvestResult.REMOVED
                ? CropPosition.of(event.getBlock().getRelative(0, 1, 0)) : null;
        debugEvent("EntityChangeBlockEvent", event.getBlock(), null,
                Material.FARMLAND, event.getTo(), removed);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!handlePistonCropImpact(event.getBlocks(), event.getDirection(), false)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!handlePistonCropImpact(event.getBlocks(), event.getDirection(), true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Optional<CropPosition> position = harvestService.findIndirectCanonical(event.getToBlock());
        if (position.isEmpty()) return;
        IndirectHarvestResult result = harvestService.handleIndirect(position.orElseThrow(), HarvestCause.WATER);
        if (result != IndirectHarvestResult.REMOVED
                || index.findByBlock(position.orElseThrow()).isPresent()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCustomCropDrop(BlockDropItemEvent event) {
        if (!harvestService.suppressesVanillaDrops(event.getBlock())) return;
        event.getItems().removeIf(drop -> isVanillaCropDrop(drop.getItemStack()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCustomCropItemSpawn(ItemSpawnEvent event) {
        if (!harvestService.suppressesVanillaDrops(event.getLocation())) return;
        if (isVanillaCropDrop(event.getEntity().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(block -> harvestService.handleIndirect(block, HarvestCause.EXPLOSION));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(block -> harvestService.handleIndirect(block, HarvestCause.EXPLOSION));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isRegisteredCrop(event.getBlockPlaced())) clearCrop(event.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (started) loadChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (started) unloadChunk(event.getChunk());
    }

    private void tickGrowth() {
        if (!started || !registry.isEnabled()) return;
        long now = System.currentTimeMillis();
        for (CropInstance instance : index.allLoaded()) {
            if (!instance.isDue(now)) continue;
            CropDefinition definition = registry.get(instance.cropId()).orElse(null);
            World world = Bukkit.getWorld(instance.position().worldId());
            if (definition == null || world == null || !world.isChunkLoaded(
                    instance.position().chunkX(), instance.position().chunkZ())) {
                removeInvalid(instance);
                continue;
            }
            Block block = world.getBlockAt(instance.position().x(), instance.position().y(), instance.position().z());
            if (!blockAdapter.isRepresentation(block, definition)) {
                removeInvalid(instance);
                continue;
            }
            if (!blockAdapter.isCompleteRepresentation(block, definition)) {
                if (!blockAdapter.applyStage(block, definition, instance.stage())) {
                    removeInvalid(instance);
                    continue;
                }
                markDirty(instance.position());
            }
            synchronizeGrowth(instance, definition, block, now, false);
        }
    }

    private void restoreLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) loadChunk(chunk);
        }
    }

    private void loadChunk(Chunk chunk) {
        UUID worldId = chunk.getWorld().getUID();
        if (index.isChunkLoaded(worldId, chunk.getX(), chunk.getZ())) return;
        CropStorage.CropChunkSnapshot snapshot;
        try {
            snapshot = storage.load(worldId, chunk.getX(), chunk.getZ());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Farming chunk load failed; module keeps running without that chunk: "
                            + worldId + "/" + chunk.getX() + "/" + chunk.getZ(), exception);
            return;
        }
        for (CropInstance instance : snapshot.crops()) {
            CropDefinition definition = registry.get(instance.cropId()).orElse(null);
            Block block = chunk.getBlock(instance.position().x() & 15,
                    instance.position().y(), instance.position().z() & 15);
            if (definition == null
                    || !blockAdapter.isRepresentation(block, definition)) {
                if (definition != null) blockAdapter.clearRepresentation(block, definition);
                markDirty(instance.position());
                continue;
            }
            if (!synchronizeGrowth(instance, definition, block, System.currentTimeMillis(), true)) continue;
            index.register(instance);
        }
    }

    private void unloadChunk(Chunk chunk) {
        CropIndex.ChunkKey key = CropIndex.chunkKey(chunk);
        List<CropInstance> crops = index.getChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        for (CropInstance instance : crops) {
            CropDefinition definition = registry.get(instance.cropId()).orElse(null);
            if (definition == null) {
                removeInvalid(instance);
                continue;
            }
            Block block = chunk.getBlock(instance.position().x() & 15,
                    instance.position().y(), instance.position().z() & 15);
            if (!blockAdapter.isRepresentation(block, definition)) {
                removeInvalid(instance);
                continue;
            }
            synchronizeGrowth(instance, definition, block, System.currentTimeMillis(), true);
        }
        crops = index.getChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        try {
            storage.save(new CropStorage.CropChunkSnapshot(
                    chunk.getWorld().getUID(), chunk.getX(), chunk.getZ(), crops));
            index.removeChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
            dirtyChunks.remove(key);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Farming chunk save failed; keeping index loaded", exception);
            dirtyChunks.add(key);
        }
    }

    private void saveDirtyChunks() {
        for (CropIndex.ChunkKey key : Set.copyOf(dirtyChunks)) {
            World world = Bukkit.getWorld(key.worldId());
            if (world == null || !world.isChunkLoaded(key.x(), key.z())) continue;
            saveChunk(key, index.getChunk(key.worldId(), key.x(), key.z()));
        }
    }

    private void saveAllLoadedChunks() {
        for (CropIndex.ChunkKey key : index.loadedChunks()) {
            saveChunk(key, index.getChunk(key.worldId(), key.x(), key.z()));
        }
    }

    private void saveChunk(CropIndex.ChunkKey key, List<CropInstance> crops) {
        try {
            storage.save(new CropStorage.CropChunkSnapshot(key.worldId(), key.x(), key.z(), crops));
            dirtyChunks.remove(key);
        } catch (IOException exception) {
            dirtyChunks.add(key);
            plugin.getLogger().log(Level.WARNING, "Farming chunk save failed: " + key, exception);
        }
    }

    private void removeInvalid(CropInstance instance) {
        if (index.remove(instance.position()).isPresent()) {
            World world = Bukkit.getWorld(instance.position().worldId());
            CropDefinition definition = registry.get(instance.cropId()).orElse(null);
            if (world != null && definition != null) {
                Block canonical = world.getBlockAt(instance.position().x(),
                        instance.position().y(), instance.position().z());
                blockAdapter.clearRepresentation(canonical, definition);
            }
            markDirty(instance.position());
        }
    }

    private boolean synchronizeGrowth(CropInstance instance, CropDefinition definition,
                                      Block block, long now, boolean alwaysApply) {
        CropGrowthCalculator.GrowthResult result = CropGrowthCalculator.catchUp(
                instance.stage(), definition.maxStage(), now,
                instance.nextGrowthAt(), definition.secondsPerStage());
        if (!alwaysApply && !result.changed()) return true;
        if (!blockAdapter.applyStage(block, definition, result.stage())) {
            removeInvalid(instance);
            return false;
        }
        if (result.changed()) {
            instance.applyGrowth(result.stage(), result.nextGrowthAt());
            markDirty(instance.position());
        }
        return true;
    }

    private boolean isRegisteredCrop(Block block) {
        return block != null && representationResolver.find(CropPosition.of(block)).isPresent();
    }

    private boolean touchesRegisteredCrop(Block block) {
        return isRegisteredCrop(block) || (block != null && block.getType() == Material.FARMLAND
                && isRegisteredCrop(block.getRelative(0, 1, 0)));
    }

    private CropPosition clearSupportedCrop(Block support) {
        if (support == null || support.getType() != Material.FARMLAND) return null;
        return clearCrop(support.getRelative(0, 1, 0));
    }

    private CropPosition clearSupportedCropIfSoilChanges(Block support, Material newSoil) {
        if (support == null || support.getType() != Material.FARMLAND) return null;
        Block cropBlock = support.getRelative(0, 1, 0);
        CropInstance instance = representationResolver.find(CropPosition.of(cropBlock)).orElse(null);
        if (instance == null) return null;
        CropDefinition definition = registry.get(instance.cropId()).orElse(null);
        if (definition != null && definition.soil().contains(newSoil)) return null;
        return clearCrop(cropBlock);
    }

    private boolean isSupportedSoil(Block support, Material newSoil) {
        if (support == null || newSoil == null || support.getType() != Material.FARMLAND) return false;
        CropInstance instance = representationResolver.find(
                CropPosition.of(support.getRelative(0, 1, 0))).orElse(null);
        if (instance == null) return false;
        CropDefinition definition = registry.get(instance.cropId()).orElse(null);
        return definition != null && definition.soil().contains(newSoil);
    }

    private void clearIndirectlyAffectedCrop(Block block) {
        if (isRegisteredCrop(block)) clearCrop(block);
        else if (block != null && block.getType() == Material.FARMLAND) clearSupportedCrop(block);
    }

    private CropPosition clearCrop(Block block) {
        if (block == null) return null;
        CropInstance instance = representationResolver.find(CropPosition.of(block)).orElse(null);
        if (instance == null) return null;
        CropPosition position = instance.position();
        index.remove(position);
        if (instance == null) return null;
        CropDefinition definition = registry.get(instance.cropId()).orElse(null);
        Block canonical = block.getWorld().getBlockAt(position.x(), position.y(), position.z());
        if (definition != null) blockAdapter.clearRepresentation(canonical, definition);
        markDirty(position);
        return position;
    }

    private boolean pistonTouchesRegisteredCrop(List<Block> blocks, BlockFace direction, boolean retracting) {
        BlockFace destinationFace = retracting ? direction.getOppositeFace() : direction;
        for (Block source : blocks) {
            if (touchesRegisteredCrop(source)
                    || touchesRegisteredCrop(source.getRelative(destinationFace))) return true;
        }
        return false;
    }

    private boolean handlePistonCropImpact(List<Block> blocks, BlockFace direction, boolean retracting) {
        BlockFace destinationFace = retracting ? direction.getOppositeFace() : direction;
        Set<CropPosition> affected = new java.util.HashSet<>();
        for (Block source : blocks) {
            harvestService.findIndirectCanonical(source).ifPresent(affected::add);
            harvestService.findIndirectCanonical(source.getRelative(destinationFace)).ifPresent(affected::add);
        }
        for (CropPosition position : affected) {
            if (harvestService.handleIndirect(position, HarvestCause.PISTON) != IndirectHarvestResult.REMOVED) {
                return false;
            }
        }
        return affected.isEmpty() || affected.stream().noneMatch(position -> index.findByBlock(position).isPresent());
    }

    private void debugEvent(String eventName, Block block, Block source,
                            Material before, Material after, CropPosition removed) {
        if (!debugEventsEnabled()) return;
        plugin.getLogger().info("[FarmingDebug] event=" + eventName
                + " block=" + formatBlock(block)
                + " source=" + formatBlock(source)
                + " before=" + before
                + " after=" + after
                + " removed-canonical=" + (removed == null ? "none" : removed));
    }

    private String formatBlock(Block block) {
        if (block == null) return "none";
        return block.getWorld().getUID() + ":" + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private boolean isVanillaCropDrop(ItemStack item) {
        if (item == null) return false;
        if (itemService.getItemId(item).isPresent()) return false;
        return switch (item.getType()) {
            case WHEAT, WHEAT_SEEDS, CARROT, POTATO, BEETROOT, BEETROOT_SEEDS,
                    COCOA_BEANS, NETHER_WART, MELON_SLICE, PUMPKIN_SEEDS -> true;
            default -> false;
        };
    }

    private void markDirty(CropPosition position) {
        dirtyChunks.add(new CropIndex.ChunkKey(position.worldId(), position.chunkX(), position.chunkZ()));
    }

    private void consumeOne(ItemStack held, Player player) {
        int amount = held.getAmount() - 1;
        if (amount <= 0) player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        else held.setAmount(amount);
    }

    private void stopTasks() {
        if (growthTask != null) growthTask.cancel();
        if (saveTask != null) saveTask.cancel();
        growthTask = null;
        saveTask = null;
    }

    public record RepairResult(boolean success, int storedEntries, int activeEntries,
                               int removedEntries, String message) {
        static RepairResult failure(String message) {
            return new RepairResult(false, 0, 0, 0, message == null ? "repair failed" : message);
        }
    }
}
