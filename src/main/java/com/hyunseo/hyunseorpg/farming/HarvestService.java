package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.equipment.HoeHarvestModifierService;
import com.hyunseo.hyunseorpg.equipment.ToolDurabilityService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.ThreadLocalRandom;

/** Separates direct player harvest from non-player crop destruction. */
public final class HarvestService {
    private final ConfigService config;
    private final RPGItemService itemService;
    private final InventoryDeliveryService delivery;
    private final FarmingProfileService profileService;
    private final CropRegistry registry;
    private final CropIndex index;
    private final CropRepresentationResolver resolver;
    private final CropBlockAdapter blockAdapter;
    private final Consumer<CropPosition> dirtyMarker;
    private final JavaPlugin plugin;
    private final Set<String> processing = new HashSet<>();
    private final Set<CropPosition> suppressVanillaDrops = new HashSet<>();
    private BukkitTask dropSuppressionCleanup;
    private double indirectCropDropChance = 0.10D;
    private HoeHarvestModifierService hoeModifiers;
    private ToolDurabilityService toolDurability;
    private CropQualityService qualityService;
    private FarmingHoePromotionService hoePromotionService;
    private AbundancePointService abundancePointService;
    private volatile boolean debugHarvest;

    public HarvestService(JavaPlugin plugin, ConfigService config, RPGItemService itemService,
                          InventoryDeliveryService delivery, FarmingProfileService profileService,
                          CropRegistry registry, CropIndex index,
                          CropRepresentationResolver resolver, CropBlockAdapter blockAdapter,
                          Consumer<CropPosition> dirtyMarker) {
        this.plugin = plugin;
        this.config = config;
        this.itemService = itemService;
        this.delivery = delivery;
        this.profileService = profileService;
        this.registry = registry;
        this.index = index;
        this.resolver = resolver;
        this.blockAdapter = blockAdapter;
        this.dirtyMarker = dirtyMarker;
    }

    /** Loads the ratio safely; values such as 10 must not become guaranteed drops. */
    public void validateConfiguration() {
        double configured = config.getFarmingHarvestDouble("indirect.crop-drop-chance", 0.10D);
        indirectCropDropChance = safeIndirectCropDropChance(configured);
        if (indirectCropDropChance != configured) {
            plugin.getLogger().warning("Invalid farming indirect.crop-drop-chance=" + configured
                    + "; using safe default 0.10 (ratio, not percent).");
        }
        plugin.getLogger().info("Farming indirect crop drop chance=" + indirectCropDropChance);
    }

    static double safeIndirectCropDropChance(double configured) {
        return Double.isFinite(configured) && configured >= 0.0D && configured <= 1.0D
                ? configured : 0.10D;
    }

    static long directHarvestPointAmount(long configured, HarvestCause cause,
                                         boolean mature, boolean validHarvest) {
        return directHarvestPointAmount(configured, 1.0D, cause, mature, validHarvest);
    }

    static long directHarvestPointAmount(long configured, double hoeMultiplier,
                                         HarvestCause cause, boolean mature, boolean validHarvest) {
        return directHarvestPointAmount(configured, hoeMultiplier, cause, mature, validHarvest,
                ThreadLocalRandom.current().nextDouble());
    }

    /**
     * Applies stochastic rounding so fractional promotion multipliers retain their expected value.
     * For example, 1 point at 1.25x yields 1 point normally and a second point 25% of the time.
     */
    static long directHarvestPointAmount(long configured, double hoeMultiplier,
                                         HarvestCause cause, boolean mature, boolean validHarvest,
                                         double randomUnit) {
        if (cause != HarvestCause.DIRECT_PLAYER || !mature || !validHarvest) return 0L;
        if (configured <= 0L || !Double.isFinite(hoeMultiplier) || hoeMultiplier < 0.0D) return 0L;
        double result = configured * hoeMultiplier;
        if (!Double.isFinite(result) || result >= Long.MAX_VALUE) return Long.MAX_VALUE;
        long base = Math.max(0L, (long) Math.floor(result));
        double fraction = result - base;
        if (fraction > 0.0D && Double.isFinite(randomUnit) && randomUnit >= 0.0D
                && randomUnit < fraction) {
            return base == Long.MAX_VALUE ? Long.MAX_VALUE : base + 1L;
        }
        return base;
    }

    public void setHoeHarvestServices(HoeHarvestModifierService hoeModifiers,
                                      ToolDurabilityService toolDurability) {
        this.hoeModifiers = hoeModifiers;
        this.toolDurability = toolDurability;
    }

    public void setQualityService(CropQualityService qualityService) {
        this.qualityService = qualityService;
    }

    public void setHoePromotionService(FarmingHoePromotionService hoePromotionService) {
        this.hoePromotionService = hoePromotionService;
    }

    public void setAbundancePointService(AbundancePointService abundancePointService) {
        this.abundancePointService = abundancePointService;
    }

    public void setDebugHarvest(boolean enabled) {
        debugHarvest = enabled;
    }

    public boolean debugHarvestEnabled() {
        return debugHarvest;
    }

    public boolean suppressesVanillaDrops(Block block) {
        return block != null && suppressVanillaDrops.contains(CropPosition.of(block));
    }

    public boolean suppressesVanillaDrops(Location location) {
        return location != null && location.getWorld() != null
                && suppressVanillaDrops.contains(CropPosition.of(location.getBlock()));
    }

    /** Returns true when the event was a registered crop event and was handled. */
    public DirectHarvestResult handleDirect(Player player, Block representation, ItemStack tool) {
        if (player == null || representation == null || !profileService.isReady(player)) return DirectHarvestResult.FAILED;
        CropInstance instance = resolver.find(CropPosition.of(representation)).orElse(null);
        if (instance == null) return DirectHarvestResult.NOT_A_CROP;
        CropDefinition definition = registry.get(instance.cropId()).orElse(null);
        if (definition == null) return DirectHarvestResult.FAILED;

        debug("direct player=" + player.getUniqueId() + " crop=" + instance.cropId()
                + " position=" + instance.position() + " stage=" + instance.stage());

        String key = transactionKey(instance.position(), HarvestCause.DIRECT_PLAYER);
        if (!processing.add(key)) return DirectHarvestResult.FAILED;
        try {
            boolean mature = instance.stage() >= definition.maxStage();
            HarvestContext context = context(player, instance, definition, tool,
                    HarvestCause.DIRECT_PLAYER, mature);
            if (!mature) {
                Optional<ItemStack> seed = createDrop(definition.seedItemId(), 1);
                if (seed.isEmpty()) return DirectHarvestResult.FAILED;
                if (!remove(instance, definition)) return DirectHarvestResult.FAILED;
                delivery.giveOrDiscard(player, seed.orElseThrow());
                return DirectHarvestResult.IMMATURE_UPROOTED;
            }

            HoeHarvestModifierService.HoeHarvestModifiers hoeHarvestModifiers = this.hoeModifiers == null
                    ? HoeHarvestModifierService.HoeHarvestModifiers.neutral()
                    : this.hoeModifiers.resolve(tool, profileService.getFarmingProfile(player).stage());
            FarmingHoePromotionService.HoePassives hoePassives = hoePromotionService == null
                    ? FarmingHoePromotionService.HoePassives.neutral()
                    : hoePromotionService.passives(tool);
            CropQualityService.QualityRoll quality = qualityService == null
                    ? new CropQualityService.QualityRoll(CropQuality.NORMAL, 0.0D,
                    Optional.of(definition.cropItemId()))
                    : qualityService.roll(definition.id(), hoePassives);
            String cropItemId = quality.itemId().orElse(definition.cropItemId());
            int cropAmount = scaledDropAmount(context.baseDropAmount(), hoePassives.cropDropMultiplier());
            int seedAmount = scaledDropAmount(context.seedDropAmount(), hoePassives.seedDropMultiplier());
            Optional<ItemStack> crop = createDrop(cropItemId, cropAmount);
            Optional<ItemStack> seed = createDrop(definition.seedItemId(), seedAmount);
            if (cropAmount > 0 && crop.isEmpty() || seedAmount > 0 && seed.isEmpty()) {
                return DirectHarvestResult.FAILED;
            }
            if (!remove(instance, definition)) return DirectHarvestResult.FAILED;
            crop.ifPresent(item -> delivery.giveOrDiscard(player, item));
            seed.ifPresent(item -> delivery.giveOrDiscard(player, item));
            profileService.addValidHarvest(player, definition.id(), 1L);
            awardDirectHarvestPoints(player, definition.id(), hoePassives.abundancePointMultiplier());
            if (toolDurability != null && hoeModifiers != null && hoeModifiers.isHoe(tool)) {
                toolDurability.consumeCustomCropHoeUse(player, tool);
            }
            return DirectHarvestResult.MATURE_HARVESTED;
        } finally {
            processing.remove(key);
        }
    }

    private int scaledDropAmount(int baseAmount, double multiplier) {
        if (baseAmount <= 0 || !Double.isFinite(multiplier) || multiplier <= 0.0D) return 0;
        long value = directHarvestPointAmount(baseAmount, multiplier,
                HarvestCause.DIRECT_PLAYER, true, true);
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /** Handles a crop affected by a non-player block change. */
    public IndirectHarvestResult handleIndirect(Block affected, HarvestCause cause) {
        if (affected == null || cause == HarvestCause.DIRECT_PLAYER) return IndirectHarvestResult.NOT_A_CROP;
        CropInstance instance = resolveAffected(affected);
        if (instance == null) return IndirectHarvestResult.NOT_A_CROP;
        debug("indirect cause=" + cause + " crop=" + instance.cropId()
                + " position=" + instance.position());
        return handleIndirect(instance.position(), cause, affected.getLocation());
    }

    private void debug(String message) {
        if (debugHarvest) plugin.getLogger().info("[FarmingHarvestDebug] " + message);
    }

    public Optional<CropPosition> findIndirectCanonical(Block affected) {
        CropInstance instance = affected == null ? null : resolveAffected(affected);
        return instance == null ? Optional.empty() : Optional.of(instance.position());
    }

    public IndirectHarvestResult handleIndirect(CropPosition position, HarvestCause cause) {
        if (position == null || cause == HarvestCause.DIRECT_PLAYER) return IndirectHarvestResult.NOT_A_CROP;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(position.worldId());
        if (world == null) return IndirectHarvestResult.FAILED;
        return handleIndirect(position, cause, world.getBlockAt(position.x(), position.y(), position.z()).getLocation());
    }

    private IndirectHarvestResult handleIndirect(CropPosition position, HarvestCause cause, Location dropLocation) {
        CropInstance instance = index.findByBlock(position).orElse(null);
        if (instance == null) return IndirectHarvestResult.NOT_A_CROP;
        CropDefinition definition = registry.get(instance.cropId()).orElse(null);
        if (definition == null) return IndirectHarvestResult.FAILED;

        String key = transactionKey(instance.position(), cause);
        if (!processing.add(key)) return IndirectHarvestResult.FAILED;
        try {
            if (!remove(instance, definition)) return IndirectHarvestResult.FAILED;
            if (instance.stage() < definition.maxStage()) return IndirectHarvestResult.REMOVED;
            if (indirectCropDropChance <= 0.0D || Math.random() >= indirectCropDropChance) {
                return IndirectHarvestResult.REMOVED;
            }
            createDrop(definition.cropItemId(), config.getFarmingHarvestInt("indirect.crop-amount", 1))
                    .ifPresent(item -> dropNaturally(dropLocation, item));
            return IndirectHarvestResult.REMOVED;
        } finally {
            processing.remove(key);
        }
    }

    private HarvestContext context(Player player, CropInstance instance, CropDefinition definition,
                                   ItemStack tool, HarvestCause cause, boolean mature) {
        String transactionId = player.getUniqueId() + ":" + instance.position().worldId()
                + ":" + instance.position().x() + ":" + instance.position().y() + ":"
                + instance.position().z() + ":" + cause.name();
        FarmingStage farmingStage = profileService.getFarmingProfile(player).stage();
        return new HarvestContext(player, definition.id(), instance.position(), mature, cause, tool,
                isHoe(tool), farmingStage, transactionId,
                Math.max(0, config.getFarmingHarvestInt("direct.crop-amount", 1)),
                Math.max(0, config.getFarmingHarvestInt("direct.seed-amount", 1)));
    }

    private CropInstance resolveAffected(Block affected) {
        CropInstance direct = resolver.find(CropPosition.of(affected)).orElse(null);
        if (direct != null) return direct;
        if (affected.getType() == org.bukkit.Material.FARMLAND) {
            return resolver.find(CropPosition.of(affected.getRelative(0, 1, 0))).orElse(null);
        }
        return null;
    }

    private boolean remove(CropInstance instance, CropDefinition definition) {
        Location location = new Location(org.bukkit.Bukkit.getWorld(instance.position().worldId()),
                instance.position().x(), instance.position().y(), instance.position().z());
        if (location.getWorld() == null) return false;
        markRepresentationForDropSuppression(instance.position(), definition);
        if (!blockAdapter.clearRepresentation(location.getBlock(), definition)) return false;
        if (index.remove(instance.position()).isEmpty()) return false;
        dirtyMarker.accept(instance.position());
        return true;
    }

    private void markRepresentationForDropSuppression(CropPosition position, CropDefinition definition) {
        suppressVanillaDrops.addAll(vanillaDropSuppressionPositions(position, definition.twoBlock()));
        if (dropSuppressionCleanup == null) {
            dropSuppressionCleanup = Bukkit.getScheduler().runTask(plugin, () -> {
                suppressVanillaDrops.clear();
                dropSuppressionCleanup = null;
            });
        }
    }

    static Set<CropPosition> vanillaDropSuppressionPositions(CropPosition position, boolean twoBlock) {
        Set<CropPosition> positions = new HashSet<>();
        positions.add(position);
        positions.add(new CropPosition(position.worldId(), position.x(), position.y() - 1, position.z()));
        if (twoBlock) {
            positions.add(new CropPosition(position.worldId(), position.x(), position.y() + 1, position.z()));
        }
        return positions;
    }

    private Optional<ItemStack> createDrop(String itemId, int amount) {
        return amount <= 0 ? Optional.empty() : itemService.create(itemId, amount);
    }

    private void dropNaturally(Location location, ItemStack item) {
        if (location != null && location.getWorld() != null) location.getWorld().dropItemNaturally(location, item);
    }

    private boolean isHoe(ItemStack tool) {
        return tool != null && tool.getType().name().toLowerCase(Locale.ROOT).endsWith("_hoe");
    }

    private String transactionKey(CropPosition position, HarvestCause cause) {
        return position.worldId() + ":" + position.x() + ":" + position.y() + ":" + position.z()
                + ":" + cause.name();
    }

    private void awardDirectHarvestPoints(Player player, String cropId, double hoeMultiplier) {
        if (abundancePointService == null) return;
        long amount = directHarvestPointAmount(
                config.getFarmingHarvestLong("direct.abundance-points", 0L), hoeMultiplier,
                HarvestCause.DIRECT_PLAYER, true, true);
        if (amount == 0L) return;
        if (!abundancePointService.addPoints(player.getUniqueId(), amount,
                AbundancePointService.Source.HARVEST)) {
            plugin.getLogger().warning("Failed to save direct harvest abundance points for "
                    + player.getUniqueId() + " crop=" + cropId);
        }
    }

}
