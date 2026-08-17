package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.enchant.EnchantService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentPromotionService;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

/** Shared durability-preservation rules for normal tool damage and farming use. */
public final class ToolDurabilityService {
    private final ConfigService config;
    private final EquipmentTierService tiers;
    private final EquipmentPromotionService promotion;
    private final EnchantService enchants;
    private final HoeHarvestModifierService hoeModifiers;

    public ToolDurabilityService(ConfigService config, EquipmentTierService tiers,
                                 EquipmentPromotionService promotion, EnchantService enchants,
                                 HoeHarvestModifierService hoeModifiers) {
        this.config = config;
        this.tiers = tiers;
        this.promotion = promotion;
        this.enchants = enchants;
        this.hoeModifiers = hoeModifiers;
    }

    public boolean isHoe(ItemStack item) {
        return item != null && !item.getType().isAir()
                && tiers.getCategory(item) == EquipmentTierService.Category.TOOL
                && item.getType().name().endsWith("_HOE");
    }

    /** Preserves the existing PlayerItemDamageEvent behavior for normal tools. */
    public boolean shouldPreserveNormal(ItemStack tool) {
        return tool != null && tool.getItemMeta() != null && tool.getItemMeta().isUnbreakable()
                || promotionChance(tool) > 0.0D && roll(promotionChance(tool));
    }

    /** Applies one custom-crop harvest use to the player's main-hand hoe. */
    public boolean consumeCustomCropHoeUse(Player player, ItemStack source) {
        if (player == null || !isHoe(source)) return false;
        ItemStack target = player.getInventory().getItemInMainHand();
        if (target == null || !target.isSimilar(source)) return false;
        ItemMeta meta = target.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return true;
        if (meta.isUnbreakable() || target.getType().getMaxDurability() <= 0) return true;
        if (shouldPreserveCustomCrop(target)) return true;
        int current = Math.max(0, damageable.getDamage());
        int maximum = target.getType().getMaxDurability();
        if (current + 1 >= maximum) {
            // Keep the existing plugin behavior: a custom use never writes an invalid
            // damage value. The regular Bukkit damage lifecycle handles destruction.
            return false;
        }
        damageable.setDamage(current + 1);
        target.setItemMeta(damageable);
        return true;
    }

    boolean shouldPreserveCustomCrop(ItemStack tool) {
        if (tool == null) return false;
        ItemMeta meta = tool.getItemMeta();
        if (meta != null && meta.isUnbreakable()) return true;
        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && ThreadLocalRandom.current().nextInt(unbreaking + 1) > 0) return true;
        if (enchants != null && enchants.hasActiveEquipped(tool, "unbreaking")
                && roll(clamp(config.getEnchantsDouble("enchants.unbreaking.settings.chance", 0.20D)))) {
            return true;
        }
        return roll(hoeModifiers.resolve(tool, null).durabilitySaveChance());
    }

    private double promotionChance(ItemStack tool) {
        return tool == null || promotion == null ? 0.0D
                : clamp(promotion.getOptionValue(tool, "durability-save-chance"));
    }

    private boolean roll(double chance) {
        return chance > 0.0D && ThreadLocalRandom.current().nextDouble() < clamp(chance);
    }

    private double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }
}
