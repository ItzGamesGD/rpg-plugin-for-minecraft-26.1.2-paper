package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores item-local hoe promotion metadata and resolves its fixed passive table.
 *
 * This is deliberately separate from FarmingProfileService. Player farming
 * stage/unlocks are progression; this tier/star is an item-local promotion.
 */
public final class FarmingHoePromotionService {
    private static final int DATA_VERSION = 2;

    private final ConfigService config;
    private final EquipmentTierService tiers;
    private final NamespacedKey tierKey;
    private final NamespacedKey starKey;
    private final NamespacedKey instanceKey;
    private final NamespacedKey dataVersionKey;
    private Map<String, HoePassives> passiveTable = Map.of("0:0", HoePassives.neutral());

    public FarmingHoePromotionService(JavaPlugin plugin, ConfigService config,
                                      EquipmentTierService tiers) {
        this.config = config;
        this.tiers = tiers;
        this.tierKey = new NamespacedKey(plugin, "farming_tier");
        this.starKey = new NamespacedKey(plugin, "farming_star");
        this.instanceKey = new NamespacedKey(plugin, "farming_hoe_instance_id");
        this.dataVersionKey = new NamespacedKey(plugin, "farming_hoe_data_version");
    }

    /** Loads item-local tier/star metadata without importing performance values. */
    public void load() {
        Map<String, HoePassives> loaded = new LinkedHashMap<>();
        for (String rawTier : config.getFarmingHoePromotionKeys("tiers")) {
            for (String rawStar : config.getFarmingHoePromotionKeys("tiers." + rawTier)) {
                int tier = parseNonNegative(rawTier);
                int star = parseNonNegative(rawStar);
                if (tier < 0 || star < 0) continue;
                String path = "tiers." + rawTier + "." + rawStar;
                loaded.put(key(tier, star), readPassives(path));
            }
        }
        if (loaded.isEmpty()) loaded.put("0:0", HoePassives.neutral());
        passiveTable = Map.copyOf(loaded);
    }

    private HoePassives readPassives(String path) {
        double shift = clamp(config.getFarmingHoePromotionDouble(
                path + ".quality-density-shift", 0.0D), 0.0D,
                Math.max(0.0D, config.getFarmingHoePromotionDouble(
                        "limits.maximum-quality-density-shift", 10.0D)));
        double multiplier = clamp(config.getFarmingHoePromotionDouble(
                path + ".abundance-point-multiplier", 1.0D), 1.0D,
                Math.max(1.0D, config.getFarmingHoePromotionDouble(
                        "limits.maximum-abundance-point-multiplier", 2.0D)));
        double rareSeedChance = clamp(config.getFarmingHoePromotionDouble(
                path + ".rare-seed-chance", 0.0D), 0.0D,
                Math.min(1.0D, Math.max(0.0D, config.getFarmingHoePromotionDouble(
                        "limits.maximum-rare-seed-chance", 1.0D))));
        boolean rareSeedEnabled = config.getFarmingHoePromotionBoolean(
                path + ".rare-seed-enabled", false);
        double cropDropMultiplier = clamp(config.getFarmingHoePromotionDouble(
                path + ".crop-drop-multiplier", 1.0D), 1.0D, 10.0D);
        double seedDropMultiplier = clamp(config.getFarmingHoePromotionDouble(
                path + ".seed-drop-multiplier", 1.0D), 1.0D, 10.0D);
        return new HoePassives(shift, multiplier, rareSeedChance, rareSeedEnabled,
                cropDropMultiplier, seedDropMultiplier);
    }

    public boolean isHoe(ItemStack item) {
        return item != null && !item.getType().isAir()
                && tiers.getCategory(item) == EquipmentTierService.Category.TOOL
                && item.getType().name().endsWith("_HOE");
    }

    /** Adds only missing metadata; it never changes enhancement or farming data. */
    public boolean ensureData(ItemStack item) {
        if (!isHoe(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        boolean changed = false;
        if (!pdc.has(tierKey, PersistentDataType.INTEGER)) {
            pdc.set(tierKey, PersistentDataType.INTEGER, 0);
            changed = true;
        }
        if (!pdc.has(starKey, PersistentDataType.INTEGER)) {
            pdc.set(starKey, PersistentDataType.INTEGER, 0);
            changed = true;
        }
        if (!pdc.has(instanceKey, PersistentDataType.STRING)) {
            pdc.set(instanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            changed = true;
        }
        if (pdc.getOrDefault(dataVersionKey, PersistentDataType.INTEGER, 0) < DATA_VERSION) {
            pdc.set(dataVersionKey, PersistentDataType.INTEGER, DATA_VERSION);
            changed = true;
        }
        if (changed) item.setItemMeta(meta);
        return changed;
    }

    public HoeTierStar read(ItemStack item) {
        if (!isHoe(item)) return new HoeTierStar(0, 0, "");
        ensureData(item);
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        int tier = Math.max(0, pdc.getOrDefault(tierKey, PersistentDataType.INTEGER, 0));
        int star = Math.max(0, pdc.getOrDefault(starKey, PersistentDataType.INTEGER, 0));
        String instanceId = pdc.getOrDefault(instanceKey, PersistentDataType.STRING, "");
        return new HoeTierStar(tier, star, instanceId);
    }

    /** Metadata setter for future item-local promotion content only. */
    public boolean setTierStar(ItemStack item, int tier, int star) {
        if (!isHoe(item) || tier < 0 || star < 0) return false;
        ensureData(item);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, tier);
        pdc.set(starKey, PersistentDataType.INTEGER, star);
        item.setItemMeta(meta);
        return true;
    }

    public HoePassives passives(ItemStack item) {
        HoeTierStar value = read(item);
        return passives(value.tier(), value.star());
    }

    public HoePassives passives(int tier, int star) {
        HoePassives exact = passiveTable.get(key(Math.max(0, tier), Math.max(0, star)));
        if (exact != null) return exact;
        HoePassives tierDefault = passiveTable.get(key(Math.max(0, tier), 0));
        return tierDefault == null ? HoePassives.neutral() : tierDefault;
    }

    private int parseNonNegative(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? -1 : parsed;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String key(int tier, int star) { return tier + ":" + star; }

    private double clamp(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
    }

    public String instanceId(ItemStack item) {
        return read(item).instanceId();
    }

    public boolean isSameInstance(ItemStack item, String instanceId) {
        return instanceId != null && !instanceId.isBlank() && instanceId.equals(instanceId(item));
    }

    public record HoeTierStar(int tier, int star, String instanceId) { }

    public record HoePassives(double qualityDensityShift, double abundancePointMultiplier,
                              double rareSeedChance, boolean rareSeedEnabled,
                              double cropDropMultiplier, double seedDropMultiplier) {
        public static HoePassives neutral() {
            return new HoePassives(0.0D, 1.0D, 0.0D, false, 1.0D, 1.0D);
        }
    }
}
