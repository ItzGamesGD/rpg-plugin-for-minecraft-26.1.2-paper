package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

/** Farming-only hoe promotion flow reused by the ordinary promotion GUI. */
public final class FarmingPromotionService {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final RPGItemService items;
    private final FarmingProfileService profiles;
    private final EquipmentTierService tiers;
    private final EquipmentEnhancementService enhancement;
    private FarmingHoePromotionService hoePromotion;

    public FarmingPromotionService(ConfigService config, RPGItemService items,
                                   FarmingProfileService profiles, EquipmentTierService tiers,
                                   EquipmentEnhancementService enhancement) {
        this(null, config, items, profiles, tiers, enhancement);
    }

    public FarmingPromotionService(JavaPlugin plugin, ConfigService config, RPGItemService items,
                                   FarmingProfileService profiles, EquipmentTierService tiers,
                                   EquipmentEnhancementService enhancement) {
        this.plugin = plugin;
        this.config = config;
        this.items = items;
        this.profiles = profiles;
        this.tiers = tiers;
        this.enhancement = enhancement;
    }

    public void setHoePromotionService(FarmingHoePromotionService hoePromotion) {
        this.hoePromotion = hoePromotion;
    }

    public boolean isFarmingHoe(ItemStack item) {
        return item != null && !item.getType().isAir()
                && tiers.getCategory(item) == EquipmentTierService.Category.TOOL
                && item.getType().name().endsWith("_HOE");
    }

    public Optional<FarmingPromotionPreview> preview(Player player, ItemStack hoe) {
        if (!config.getFarmingProgressionBoolean("enabled", true)
                || player == null || !profiles.isReady(player) || !isFarmingHoe(hoe)
                || hoePromotion == null) return Optional.empty();
        FarmingProfile profile = profiles.getFarmingProfile(player);
        int currentHoeTier = hoePromotion == null ? 0 : hoePromotion.read(hoe).tier();
        FarmingStage next = nextForHoeTier(currentHoeTier);
        if (next == null) return Optional.empty();
        String path = "promotion." + next.name().toLowerCase(Locale.ROOT);
        String unlock = normalized(config.getFarmingProgressionString(path + ".unlock-crop", ""));
        return Optional.of(new FarmingPromotionPreview(
                next,
                config.getFarmingProgressionString(path + ".display-name", next.displayName()),
                config.getFarmingProgressionString(path + ".minimum-tier", "iron").toLowerCase(Locale.ROOT),
                Math.max(0, config.getFarmingProgressionInt(path + ".required-enhancement", 0)),
                Math.max(0L, config.getFarmingProgressionLong(path + ".required-valid-harvests", 0L)),
                normalized(config.getFarmingProgressionString(path + ".required-crop-item-id", "")),
                Math.max(0, config.getFarmingProgressionInt(path + ".required-crop-amount", 0)),
                Math.max(0, config.getFarmingProgressionInt(path + ".required-magic-stone-amount", 0)),
                unlock,
                profile.stage(),
                profile.totalValidHarvests(),
                enhancement.getLevel(hoe),
                tiers.getTier(hoe)
        ));
    }

    public Optional<PromotionRule> nextRule(FarmingStage current) {
        FarmingStage next = next(current);
        if (next == null) return Optional.empty();
        String path = "promotion." + next.name().toLowerCase(Locale.ROOT);
        return Optional.of(new PromotionRule(
                next,
                config.getFarmingProgressionString(path + ".minimum-tier", "iron"),
                Math.max(0, config.getFarmingProgressionInt(path + ".required-enhancement", 0)),
                Math.max(0L, config.getFarmingProgressionLong(path + ".required-valid-harvests", 0L)),
                normalized(config.getFarmingProgressionString(path + ".required-crop-item-id", "")),
                Math.max(0, config.getFarmingProgressionInt(path + ".required-crop-amount", 0)),
                Math.max(0, config.getFarmingProgressionInt(path + ".required-magic-stone-amount", 0)),
                normalized(config.getFarmingProgressionString(path + ".unlock-crop", ""))
        ));
    }

    public FarmingPromotionResult promote(Player player, ItemStack hoe) {
        return promote(player, hoe, null);
    }

    public FarmingPromotionResult promote(Player player, ItemStack hoe, ItemStack offeredMagicStone) {
        InventorySnapshot safetySnapshot = player == null ? null : InventorySnapshot.capture(player.getInventory());
        int offeredStoneAmount = offeredMagicStone == null ? 0 : offeredMagicStone.getAmount();
        try {
            return promoteInternal(player, hoe, offeredMagicStone);
        } catch (RuntimeException exception) {
            if (safetySnapshot != null) safetySnapshot.restore(player.getInventory());
            if (offeredMagicStone != null) offeredMagicStone.setAmount(offeredStoneAmount);
            if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Farming promotion exception: player="
                                + (player == null ? "null" : player.getUniqueId())
                                + ", hoe=" + describeItem(hoe), exception);
            }
            return FarmingPromotionResult.failure("\uB18D\uC0AC \uC2B9\uAE09 \uCC98\uB9AC \uC911 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7AC\uB8CC\uB294 \uCC28\uAC10\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4.");
        }
    }

    private FarmingPromotionResult promoteInternal(Player player, ItemStack hoe, ItemStack offeredMagicStone) {
        FarmingPromotionPreview preview = preview(player, hoe).orElse(null);
        if (preview == null) return FarmingPromotionResult.failure("\uB18D\uC0AC \uC2B9\uAE09 \uB300\uC0C1 \uAD2D\uC774\uAC00 \uC544\uB2D9\uB2C8\uB2E4.");
        String tierFailure = tierFailure(preview);
        if (tierFailure != null) return FarmingPromotionResult.failure(tierFailure);
        if (preview.currentEnhancement() < preview.requiredEnhancement()) {
            return FarmingPromotionResult.failure("\uAD2D\uC774 \uAC15\uD654\uAC00 \uBD80\uC871\uD569\uB2C8\uB2E4. \uD544\uC694: +" + preview.requiredEnhancement());
        }
        if (preview.currentHarvests() < preview.requiredValidHarvests()) {
            return FarmingPromotionResult.failure("\uC720\uD6A8 \uC218\uD655\uB7C9\uC774 \uBD80\uC871\uD569\uB2C8\uB2E4. \uD544\uC694: " + preview.requiredValidHarvests());
        }
        boolean offeredStoneValid = offeredMagicStone != null
                && items.isItem(offeredMagicStone, magicStoneId())
                && offeredMagicStone.getAmount() >= preview.requiredMagicStoneAmount();
        if (!has(player, preview.requiredCropItemId(), preview.requiredCropAmount())
                || (!offeredStoneValid && !has(player, magicStoneId(), preview.requiredMagicStoneAmount()))) {
            return FarmingPromotionResult.failure("\uC791\uBB3C \uB610\uB294 \uB9C8\uC11D\uC774 \uBD80\uC871\uD569\uB2C8\uB2E4.");
        }

        InventorySnapshot snapshot = InventorySnapshot.capture(player.getInventory());
        int originalStoneAmount = offeredMagicStone == null ? 0 : offeredMagicStone.getAmount();
        ItemStack originalHoe = hoe == null ? null : hoe.clone();
        if (!remove(player, preview.requiredCropItemId(), preview.requiredCropAmount())) {
            snapshot.restore(player.getInventory());
            return FarmingPromotionResult.failure("\uC2B9\uAE09 \uC7AC\uB8CC \uCC28\uAC10\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
        }
        if (offeredStoneValid) {
            offeredMagicStone.setAmount(originalStoneAmount - preview.requiredMagicStoneAmount());
        } else if (!remove(player, magicStoneId(), preview.requiredMagicStoneAmount())) {
            snapshot.restore(player.getInventory());
            return FarmingPromotionResult.failure("\uC2B9\uAE09 \uC7AC\uB8CC \uCC28\uAC10\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
        }
        int currentHoeTier = hoePromotion == null ? 0 : hoePromotion.read(hoe).tier();
        boolean hoePromotionApplied = hoePromotion == null
                || hoePromotion.setTierStar(hoe, currentHoeTier + 1, 0);
        if (!hoePromotionApplied) {
            snapshot.restore(player.getInventory());
            if (offeredStoneValid) offeredMagicStone.setAmount(originalStoneAmount);
            restoreItem(hoe, originalHoe);
            return FarmingPromotionResult.failure("\uB18D\uC0AC \uB370\uC774\uD130 \uC800\uC7A5\uC5D0 \uC2E4\uD328\uD574 \uC2B9\uAE09\uC744 \uCDE8\uC18C\uD588\uC2B5\uB2C8\uB2E4.");
        }
        // Player farming stage/unlocks are account progression and remain independent
        // from this item-local hoe promotion.
        return FarmingPromotionResult.success(preview.nextStage(), "");
    }

    private String describeItem(ItemStack item) {
        if (item == null) return "null";
        return item.getType().name() + "/amount=" + item.getAmount()
                + "/enhancement=" + enhancement.getLevel(item);
    }

    private String tierFailure(FarmingPromotionPreview preview) {
        return tierRank(preview.currentTier()) < tierRank(preview.minimumTier())
                ? "\uAD2D\uC774 \uC7AC\uC9C8\uC774 \uBD80\uC871\uD569\uB2C8\uB2E4. \uD544\uC694: " + tierDisplay(preview.minimumTier()) : null;
    }

    private String tierDisplay(String tier) {
        return switch (normalized(tier)) {
            case "wooden" -> "\uB098\uBB34";
            case "stone" -> "\uB3CC";
            case "gold", "golden" -> "\uAE08";
            case "iron" -> "\uCCA0";
            case "diamond" -> "\uB2E4\uC774\uC544\uBAAC\uB4DC";
            case "netherite" -> "\uB124\uB354\uB77C\uC774\uD2B8";
            default -> tier == null ? "" : tier;
        };
    }

    private int tierRank(String tier) {
        return switch (normalized(tier)) {
            case "wooden" -> 0;
            case "stone", "gold" -> 1;
            case "iron" -> 2;
            case "diamond" -> 3;
            case "netherite" -> 4;
            default -> -1;
        };
    }

    private FarmingStage next(FarmingStage current) {
        if (current == null || current == FarmingStage.EXPERT) return null;
        return FarmingStage.values()[current.ordinal() + 1];
    }

    private FarmingStage nextForHoeTier(int currentTier) {
        if (currentTier < 0 || currentTier >= FarmingStage.EXPERT.ordinal()) return null;
        return FarmingStage.values()[currentTier + 1];
    }

    private String magicStoneId() {
        return config.getEquipmentGrowthString("promotion.magic-stone-item-id", "magic_stone");
    }

    private boolean has(Player player, String id, int amount) {
        return amount <= 0 || (!id.isBlank() && count(player, id) >= amount);
    }

    private int count(Player player, String id) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (items.isItem(item, id)) total = safeAdd(total, item.getAmount());
        }
        return total;
    }

    private boolean remove(Player player, String id, int amount) {
        if (amount <= 0) return true;
        if (!has(player, id, amount)) return false;
        int remaining = amount;
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getStorageContents().length && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!items.isItem(item, id)) continue;
            int removed = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - removed);
            if (item.getAmount() <= 0) inventory.setItem(slot, null);
            remaining -= removed;
        }
        return remaining == 0;
    }

    private int safeAdd(int left, int right) {
        return right > Integer.MAX_VALUE - left ? Integer.MAX_VALUE : left + right;
    }

    private void restoreItem(ItemStack target, ItemStack source) {
        if (target == null || source == null) return;
        target.setType(source.getType());
        target.setItemMeta(source.getItemMeta());
        target.setAmount(source.getAmount());
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record FarmingPromotionPreview(FarmingStage nextStage, String displayName,
                                          String minimumTier, int requiredEnhancement,
                                          long requiredValidHarvests, String requiredCropItemId,
                                          int requiredCropAmount, int requiredMagicStoneAmount,
                                          String unlockCrop, FarmingStage currentStage,
                                          long currentHarvests, int currentEnhancement,
                                          String currentTier) { }

    public record PromotionRule(FarmingStage nextStage, String minimumTier, int requiredEnhancement,
                                long requiredValidHarvests, String requiredCropItemId,
                                int requiredCropAmount, int requiredMagicStoneAmount,
                                String unlockCrop) { }

    public record FarmingPromotionResult(boolean success, String message,
                                         FarmingStage stage, String unlockedCrop) {
        static FarmingPromotionResult success(FarmingStage stage, String crop) {
            return new FarmingPromotionResult(true, "\uAD2D\uC774 \uB18D\uC0AC \uB2E8\uACC4 \uC2B9\uAE09 \uC644\uB8CC: " + stage.displayName(), stage, crop);
        }

        static FarmingPromotionResult failure(String message) {
            return new FarmingPromotionResult(false, message, null, "");
        }
    }

    private record InventorySnapshot(ItemStack[] storage, ItemStack[] armor, ItemStack offhand) {
        static InventorySnapshot capture(PlayerInventory inventory) {
            ItemStack[] storage = cloneItems(inventory.getStorageContents());
            ItemStack[] armor = cloneItems(inventory.getArmorContents());
            ItemStack offhand = inventory.getItemInOffHand();
            return new InventorySnapshot(storage, armor, offhand == null ? null : offhand.clone());
        }

        void restore(PlayerInventory inventory) {
            inventory.setStorageContents(cloneItems(storage));
            inventory.setArmorContents(cloneItems(armor));
            inventory.setItemInOffHand(offhand == null ? null : offhand.clone());
        }

        private static ItemStack[] cloneItems(ItemStack[] source) {
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
            return copy;
        }
    }
}
