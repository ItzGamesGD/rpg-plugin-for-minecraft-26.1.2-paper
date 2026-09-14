package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/** Repairs only durability while preserving the existing ItemMeta and PDC. */
public final class EquipmentRepairService {
    private final ConfigService config;
    private final CoinService coins;
    private final EquipmentTierService tiers;
    private final EquipmentGrowthPolicy growthPolicy;
    private final EquipmentEnhancementService enhancement;

    public EquipmentRepairService(ConfigService config, CoinService coins, EquipmentTierService tiers,
                                  EquipmentGrowthPolicy growthPolicy,
                                  EquipmentEnhancementService enhancement) {
        this.config = config;
        this.coins = coins;
        this.tiers = tiers;
        this.growthPolicy = growthPolicy;
        this.enhancement = enhancement;
    }

    public boolean isRepairable(ItemStack item) {
        return growthPolicy.canRepair(item)
                && tiers.isSupported(item)
                && item != null && item.hasItemMeta()
                && item.getItemMeta() instanceof Damageable;
    }

    public int missingDurability(ItemStack item) {
        if (!isRepairable(item)) return 0;
        return Math.max(0, ((Damageable) item.getItemMeta()).getDamage());
    }

    public int maximumDurability(ItemStack item) {
        return item == null ? 0 : Math.max(0, item.getType().getMaxDurability());
    }

    public RepairQuote quote(ItemStack item) {
        if (!isRepairable(item)) return new RepairQuote(false, 0, maximumDurability(item), 0L, "Equipment cannot be repaired.");
        int missing = missingDurability(item);
        if (missing <= 0) return new RepairQuote(false, 0, maximumDurability(item), 0L, "Durability is already full.");
        long fullCost = costForDamage(item, missing);
        return new RepairQuote(true, missing, maximumDurability(item), fullCost, "Repair available.");
    }

    public long cost(ItemStack item) {
        return quote(item).cost();
    }

    public RepairResult repair(Player player, ItemStack item) {
        RepairQuote quote = quote(item);
        if (!quote.repairable()) return new RepairResult(false, 0L, 0, quote.message());
        long available = Math.max(0L, coins.getCoins(player));
        int amount = quote.missingDurability();
        long charge = quote.cost();
        if (available < charge && config.getBoolean("repair.allow-partial-repair", true)) {
            amount = maximumRepairAmount(item, available);
            charge = amount <= 0 ? 0L : costForDamage(item, amount, false);
        }
        if (amount <= 0 || charge <= 0 || available < charge) {
            return new RepairResult(false, quote.cost(), 0,
                    "Not enough coins. Required: " + quote.cost() + ", available: " + available);
        }

        ItemStack before = item.clone();
        if (!coins.takeCoins(player, charge)) {
            return new RepairResult(false, charge, 0, "Coin deduction failed.");
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof Damageable damageable)) throw new IllegalStateException("missing Damageable meta");
            damageable.setDamage(Math.max(0, damageable.getDamage() - amount));
            item.setItemMeta(meta);
            return new RepairResult(true, charge, amount, "Repair complete.");
        } catch (RuntimeException exception) {
            item.setItemMeta(before.getItemMeta());
            coins.addCoins(player, charge);
            return new RepairResult(false, charge, 0, "Repair failed; no coins were consumed.");
        }
    }

    private int maximumRepairAmount(ItemStack item, long available) {
        int missing = missingDurability(item);
        int low = 0;
        int high = missing;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (costForDamage(item, middle, false) <= available) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private long costForDamage(ItemStack item, int damage) {
        return costForDamage(item, damage, true);
    }

    private long costForDamage(ItemStack item, int damage, boolean applyMinimum) {
        long base = Math.max(0L, config.getLong("repair.base-cost", 0L));
        long perPoint = Math.max(0L, config.getLong("repair.cost-per-durability", 3L));
        base = safeAdd(base, safeMultiply(damage, perPoint));
        long minimum = Math.max(0L, Math.max(
                config.getLong("repair.minimum-cost", 50L),
                config.getLong("repair.durability.minimum-cost", 50L)));
        double maximum = Math.max(1.0D, maximumDurability(item));
        double ratio = clamp(damage / maximum);
        double exponent = positiveFinite(config.getDouble("repair.durability.exponent", 1.0D), 1.0D);
        double weight = finite(config.getDouble("repair.durability.weight", 0.0D), 0.0D);
        double durabilityMultiplier = Math.max(0.0D, 1.0D + weight * Math.pow(ratio, exponent));
        double total = base * durabilityMultiplier
                * enhancementMultiplier(item);
        if (!Double.isFinite(total) || total >= Long.MAX_VALUE) return Long.MAX_VALUE;
        long rounded = Math.max(1L, Math.round(total));
        return applyMinimum ? Math.max(minimum, rounded) : rounded;
    }

    private double enhancementMultiplier(ItemStack item) {
        int maximum = Math.max(1, enhancement.getMaximumLevel(item));
        double progress = clamp((double) enhancement.getLevel(item) / maximum);
        double perProgress = finite(config.getDouble("repair.enhancement.multiplier-per-progress", 0.0D), 0.0D);
        return Math.max(0.0D, 1.0D + progress * perProgress);
    }

    private long safeMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private long safeAdd(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }

    private double clamp(double value) { return Math.max(0.0D, Math.min(1.0D, value)); }
    private double finite(double value, double fallback) { return Double.isFinite(value) ? value : fallback; }
    private double positiveFinite(double value, double fallback) { return Double.isFinite(value) && value > 0.0D ? value : fallback; }

    public record RepairQuote(boolean repairable, int missingDurability, int maximumDurability,
                              long cost, String message) { }
    public record RepairResult(boolean success, long cost, int repairedDurability, String message) { }
}
