package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.enchant.EnchantService;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.farming.FarmingPromotionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Shared GUI for enhancement, promotion and enchantment equipment input. */
public final class EquipmentGrowthGuiService {
    public static final int EQUIPMENT_SLOT = 10;
    public static final int STONE_SLOT = 16;
    public static final int EXECUTE_SLOT = 22;
    public static final int BACK_SLOT = 18;
    public static final int CLOSE_SLOT = 26;
    private static final Set<Integer> INPUT_SLOTS = Set.of(EQUIPMENT_SLOT, STONE_SLOT);

    private final JavaPlugin plugin;
    private final EquipmentEnhancementService enhancementService;
    private final EquipmentPromotionService promotionService;
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private EnchantService enchantService;
    private EquipmentRepairService repairService;
    private EquipmentGrowthPolicy growthPolicy;
    private EquipmentSupportGuiService supportService;
    private com.hyunseo.hyunseorpg.shop.ShopGuiService shopGuiService;
    private CoinService coinService;
    private FarmingPromotionService farmingPromotionService;

    public EquipmentGrowthGuiService(JavaPlugin plugin, EquipmentEnhancementService enhancementService,
                                     EquipmentPromotionService promotionService) {
        this.plugin = plugin;
        this.enhancementService = enhancementService;
        this.promotionService = promotionService;
    }

    public void setEnchantService(EnchantService enchantService) { this.enchantService = enchantService; }
    public void setRepairService(EquipmentRepairService repairService) { this.repairService = repairService; }
    public void setGrowthPolicy(EquipmentGrowthPolicy growthPolicy) { this.growthPolicy = growthPolicy; }
    public void setSupportService(EquipmentSupportGuiService supportService) { this.supportService = supportService; }
    public void setCoinService(CoinService coinService) { this.coinService = coinService; }
    public void setShopGuiService(com.hyunseo.hyunseorpg.shop.ShopGuiService shopGuiService) { this.shopGuiService = shopGuiService; }
    public void setFarmingPromotionService(FarmingPromotionService service) { this.farmingPromotionService = service; }
    public EquipmentSupportGuiService getSupportService() { return supportService; }
    public com.hyunseo.hyunseorpg.shop.ShopGuiService getShopGuiService() { return shopGuiService; }
    public boolean isInputSlot(int slot) { return INPUT_SLOTS.contains(slot); }

    public void openMain(Player player) {
        EquipmentGrowthMenuHolder holder = new EquipmentGrowthMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("장비 성장"));
        holder.setInventory(inventory);
        fill(inventory);
        inventory.setItem(20, icon(Material.NETHER_STAR, "승급 옵션 초기화", List.of("선택한 옵션만 다시 결정합니다.")));
        inventory.setItem(24, icon(Material.KNOWLEDGE_BOOK, "향후 확장", List.of("아직 구현되지 않은 기능 목록")));
        inventory.setItem(11, icon(Material.ANVIL, "강화", List.of("강화석으로 장비의 기본 수치를 높입니다.")));
        inventory.setItem(13, icon(Material.SMITHING_TABLE, "승급", List.of("승급석으로 등급과 성장 상한을 높입니다.")));
        inventory.setItem(15, icon(Material.ENCHANTED_BOOK, "인챈트", List.of("해방된 슬롯에 인챈트 북을 장착합니다.")));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
        inventory.setItem(17, icon(Material.IRON_INGOT, "수리", List.of("코인만 사용하여 내구도를 회복합니다.")));
        player.openInventory(inventory);
    }

    public void openEnhancement(Player player) {
        EnhancementInventoryHolder holder = new EnhancementInventoryHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("장비 강화"));
        holder.setInventory(inventory);
        renderEnhancement(inventory);
        player.openInventory(inventory);
    }

    public void openPromotion(Player player) {
        PromotionInventoryHolder holder = new PromotionInventoryHolder();
        holder.setPlayerId(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("장비 승급"));
        holder.setInventory(inventory);
        renderPromotion(inventory);
        player.openInventory(inventory);
    }

    public void openEnchant(Player player) {
        EnchantInventoryHolder holder = new EnchantInventoryHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("장비 인챈트"));
        holder.setInventory(inventory);
        renderEnchant(inventory);
        player.openInventory(inventory);
    }

    public void openRepair(Player player) {
        RepairInventoryHolder holder = new RepairInventoryHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("장비 수리"));
        holder.setInventory(inventory);
        renderRepair(inventory);
        player.openInventory(inventory);
    }

    public void repair(Player player, Inventory inventory) {
        if (repairService == null || !processing.add(player.getUniqueId())) return;
        try {
            ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
            if (inventory.getHolder() instanceof RepairInventoryHolder holder
                    && holder.getPreviewItem() != null
                    && (equipment == null || !equipment.isSimilar(holder.getPreviewItem()))) {
                player.sendMessage(Component.text("The repair item changed; repair was cancelled.", NamedTextColor.RED));
                return;
            }
            EquipmentRepairService.RepairResult result = repairService.repair(player, equipment);
            player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            inventory.setItem(EQUIPMENT_SLOT, equipment);
        } finally {
            renderRepair(inventory);
            processing.remove(player.getUniqueId());
        }
    }

    public void promote(Player player, Inventory inventory) {
        if (!processing.add(player.getUniqueId())) return;
        try {
            ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
            ItemStack stone = inventory.getItem(STONE_SLOT);
            if (farmingPromotionService != null && farmingPromotionService.isFarmingHoe(equipment)) {
                FarmingPromotionService.FarmingPromotionResult result = farmingPromotionService.promote(player, equipment, stone);
                player.sendMessage(Component.text(result.message(), result.success()
                        ? NamedTextColor.GREEN : NamedTextColor.RED));
                inventory.setItem(STONE_SLOT, stone == null || stone.getAmount() <= 0 ? null : stone);
                inventory.setItem(EQUIPMENT_SLOT, equipment);
                return;
            }
            if (growthPolicy != null && !growthPolicy.canPromote(equipment)) {
                player.sendMessage(Component.text(growthPolicy.growthRestriction(equipment), NamedTextColor.RED));
                return;
            }
            Optional<EquipmentPromotionService.PromotionPreview> preview = promotionService.preview(equipment);
            if (preview.isEmpty()) {
                player.sendMessage(Component.text("승급할 수 없는 장비입니다.", NamedTextColor.RED));
                return;
            }
            EquipmentPromotionService.PromotionPreview rule = preview.get();
            if (stone == null || !promotionService.isPromotionStone(stone) || stone.getAmount() < rule.stoneCost()) {
                player.sendMessage(Component.text("승급석이 부족하거나 올바르지 않습니다.", NamedTextColor.RED));
                return;
            }
            if (!consumeMaterial(inventory, STONE_SLOT, rule.stoneCost(), "승급석")) return;
            double successChance = promotionService.getSuccessChance(equipment, rule);
            if (successChance < 1.0D && ThreadLocalRandom.current().nextDouble() > successChance) {
                player.sendMessage(Component.text("승급 실패", NamedTextColor.RED));
                return;
            }
            promotionService.apply(equipment, rule);
            inventory.setItem(EQUIPMENT_SLOT, equipment);
            player.sendMessage(Component.text("승급 완료: " + rule.displayName(), NamedTextColor.LIGHT_PURPLE));
        } finally {
            renderPromotion(inventory);
            processing.remove(player.getUniqueId());
        }
    }

    public void enhance(Player player, Inventory inventory) {
        if (!processing.add(player.getUniqueId())) return;
        try {
            ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
            ItemStack stone = inventory.getItem(STONE_SLOT);
            if (growthPolicy != null && !growthPolicy.canEnhance(equipment)) {
                player.sendMessage(Component.text(growthPolicy.growthRestriction(equipment), NamedTextColor.RED));
                return;
            }
            Optional<EnhancementLevelData> next = enhancementService.getNextLevel(equipment);
            if (next.isEmpty()) {
                player.sendMessage(Component.text("강화 가능한 장비가 아니거나 최대 단계입니다.", NamedTextColor.RED));
                return;
            }
            EnhancementLevelData rule = next.get();
            long coinCost = enhancementService.getCoinCost(equipment, rule);
            if (coinService != null && coinService.getCoins(player) < coinCost) {
                player.sendMessage(Component.text("강화 코인이 부족합니다. 필요 코인: " + coinCost, NamedTextColor.RED));
                return;
            }
            if (stone == null || !enhancementService.isRequiredStone(stone) || stone.getAmount() < rule.stoneCost()) {
                player.sendMessage(Component.text("강화석이 부족하거나 올바르지 않습니다.", NamedTextColor.RED));
                return;
            }
            if (!consumeMaterial(inventory, STONE_SLOT, rule.stoneCost(), "강화석")) return;
            if (coinService != null && !coinService.takeCoins(player, coinCost)) {
                inventory.setItem(STONE_SLOT, stone);
                player.sendMessage(Component.text("강화 코인 차감에 실패했습니다.", NamedTextColor.RED));
                return;
            }
            double chance = enhancementService.getSuccessChance(equipment, rule);
            if (ThreadLocalRandom.current().nextDouble() <= chance) {
                enhancementService.applySuccessfulEnhancement(equipment, rule);
                player.sendMessage(Component.text("강화 성공: +" + rule.level(), NamedTextColor.GREEN));
            } else {
                enhancementService.recordFailure(equipment);
                player.sendMessage(Component.text("강화 실패. 단계는 유지되며 다음 성공 확률이 증가합니다.", NamedTextColor.RED));
            }
            inventory.setItem(EQUIPMENT_SLOT, equipment);
        } finally {
            renderEnhancement(inventory);
            processing.remove(player.getUniqueId());
        }
    }

    public void enchant(Player player, Inventory inventory) {
        if (enchantService == null || !processing.add(player.getUniqueId())) return;
        try {
            ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
            ItemStack book = inventory.getItem(STONE_SLOT);
            if (equipment == null || book == null || !enchantService.equip(equipment, book)) {
                player.sendMessage(Component.text("장비 등급, 슬롯, 무기 종류 또는 인챈트 북을 확인해주세요.", NamedTextColor.RED));
                return;
            }
            book.setAmount(book.getAmount() - 1);
            if (book.getAmount() <= 0) inventory.setItem(STONE_SLOT, null);
            inventory.setItem(EQUIPMENT_SLOT, equipment);
            player.sendMessage(Component.text("인챈트를 장착했습니다.", NamedTextColor.GREEN));
        } finally {
            renderEnchant(inventory);
            processing.remove(player.getUniqueId());
        }
    }

    public void refreshLater(Inventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inventory.getHolder() instanceof EnhancementInventoryHolder) renderEnhancement(inventory);
            if (inventory.getHolder() instanceof PromotionInventoryHolder) renderPromotion(inventory);
            if (inventory.getHolder() instanceof EnchantInventoryHolder) renderEnchant(inventory);
            if (inventory.getHolder() instanceof RepairInventoryHolder) renderRepair(inventory);
        });
    }

    public void returnInputs(Player player, Inventory inventory) {
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            ItemStack copy = item.clone();
            inventory.setItem(slot, null);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(copy);
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    public void returnOpenInputs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof EnhancementInventoryHolder || top.getHolder() instanceof PromotionInventoryHolder
                    || top.getHolder() instanceof EnchantInventoryHolder
                    || top.getHolder() instanceof RepairInventoryHolder) {
                returnInputs(player, top);
                player.closeInventory();
            }
        }
    }

    private void renderEnhancement(Inventory inventory) {
        ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
        ItemStack stone = inventory.getItem(STONE_SLOT);
        fill(inventory);
        inventory.setItem(EQUIPMENT_SLOT, equipment);
        inventory.setItem(STONE_SLOT, stone);
        Optional<EnhancementLevelData> next = enhancementService.getNextLevel(equipment);
        if (equipment != null && !equipment.getType().isAir() && growthPolicy != null) {
            inventory.setItem(5, icon(Material.PAPER, "Equipment policy", List.of(
                    "Equipment grade: " + growthPolicy.grade(equipment).value(),
                    "Enhancement: " + (growthPolicy.canEnhance(equipment) ? "available" : "unavailable"),
                    "Current: +" + enhancementService.getLevel(equipment) + "/" + enhancementService.getMaximumLevel(equipment)
            )));
        }
        inventory.setItem(4, icon(Material.BOOK, "강화 정보", next.map(rule -> List.of(
                "현재 강화: +" + enhancementService.getLevel(equipment),
                "다음 강화: +" + rule.level(),
                "필요 강화석: " + rule.stoneCost(),
                "성공 확률: " + Math.round(enhancementService.getSuccessChance(equipment, rule) * 100.0D) + "%",
                "실패 누적: " + enhancementService.getFailCount(equipment)
        )).orElse(List.of("강화 가능한 장비를 넣어주세요."))));
        inventory.setItem(EXECUTE_SLOT, icon(Material.LIME_DYE, "강화 실행", next.map(rule -> List.of(
                "다음 단계: +" + rule.level(),
                "필요 강화석: " + rule.stoneCost(),
                "현재 성공 확률: " + percent(enhancementService.getSuccessChance(equipment, rule)),
                "실패 1회당 보정: +" + percent(enhancementService.getFailureBonus()) + "p",
                "좌클릭으로 실행합니다."
        )).orElse(List.of("강화 가능한 장비가 없습니다."))));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "뒤로", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
    }

    private void renderPromotion(Inventory inventory) {
        ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
        ItemStack stone = inventory.getItem(STONE_SLOT);
        fill(inventory);
        inventory.setItem(EQUIPMENT_SLOT, equipment);
        inventory.setItem(STONE_SLOT, stone);
        Optional<EquipmentPromotionService.PromotionPreview> next = promotionService.preview(equipment);
        Player farmingPlayer = inventory.getHolder() instanceof PromotionInventoryHolder holder
                ? Bukkit.getPlayer(holder.playerId()) : null;
        Optional<FarmingPromotionService.FarmingPromotionPreview> farmingNext = farmingPromotionService == null
                ? Optional.empty() : farmingPromotionService.preview(farmingPlayer, equipment);
        if (equipment != null && !equipment.getType().isAir() && growthPolicy != null) {
            inventory.setItem(5, icon(Material.PAPER, "Equipment policy", List.of(
                    "Equipment grade: " + growthPolicy.grade(equipment).value(),
                    "Promotion: " + (growthPolicy.canPromote(equipment) ? "available" : "unavailable"),
                    "Promotion stage: " + promotionService.getStage(equipment)
            )));
        }
        if (farmingNext.isPresent()) {
            FarmingPromotionService.FarmingPromotionPreview rule = farmingNext.get();
            inventory.setItem(4, icon(Material.BOOK, "농사 승급 정보", List.of(
                    "다음 단계: " + rule.displayName(),
                    "괭이 재질: " + rule.minimumTier() + " 이상",
                    "필요 강화: +" + rule.requiredEnhancement(),
                    "필요 유효 수확: " + rule.requiredValidHarvests(),
                    "제출 작물: " + rule.requiredCropItemId() + " x" + rule.requiredCropAmount(),
                    "필요 마석: " + rule.requiredMagicStoneAmount(),
                    "성공률: 100%"
            )));
            inventory.setItem(EXECUTE_SLOT, icon(Material.LIME_DYE, "농사 승급 실행", List.of("코인은 사용하지 않습니다.")));
        } else {
            inventory.setItem(4, icon(Material.BOOK, "승급 정보", next.map(rule -> List.of(
                "다음 단계: " + rule.displayName(),
                "승급 요구 강화: +" + rule.requiredEnhancement(),
                "일반 옵션: " + rule.normalCount(),
                "특수 옵션: " + rule.specialCount(),
                "인챈트 슬롯 해방: " + rule.enchantSlots(),
                "필요 승급석: " + rule.stoneCost(),
                "성공 확률: " + percent(rule.successChance())
            )).orElse(List.of("승급 가능한 장비를 넣어주세요."))));
            inventory.setItem(EXECUTE_SLOT, icon(Material.LIME_DYE, "승급 실행", List.of("좌클릭으로 실행합니다.")));
        }
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "뒤로", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
    }

    private void renderEnchant(Inventory inventory) {
        ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
        ItemStack book = inventory.getItem(STONE_SLOT);
        fill(inventory);
        inventory.setItem(EQUIPMENT_SLOT, equipment);
        inventory.setItem(STONE_SLOT, book);
        inventory.setItem(4, icon(Material.BOOK, "인챈트 슬롯", equipment == null || equipment.getType().isAir()
                ? List.of("장비를 넣으면 사용 가능한 슬롯을 확인할 수 있습니다.")
                : List.of("사용 가능 슬롯: " + enchantService.getEnchantSlotLimit(equipment),
                "현재 장착: " + enchantService.formatEquipped(equipment))));
        inventory.setItem(EXECUTE_SLOT, icon(Material.LIME_DYE, "인챈트 장착", List.of("좌클릭으로 장착합니다.")));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "뒤로", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
    }

    private void renderRepair(Inventory inventory) {
        ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
        fill(inventory);
        inventory.setItem(EQUIPMENT_SLOT, equipment);
        // Repair has no material slot. Keep the filler out of the return path.
        inventory.setItem(STONE_SLOT, null);
        EquipmentRepairService.RepairQuote quote = repairService == null
                ? new EquipmentRepairService.RepairQuote(false, 0, 0, 0L, "Repair unavailable.")
                : repairService.quote(equipment);
        int missing = quote.missingDurability();
        long cost = quote.cost();
        if (equipment != null && !equipment.getType().isAir()) {
            inventory.setItem(5, icon(Material.PAPER, "Equipment policy", List.of(
                    "Equipment grade: " + (growthPolicy == null ? 0 : growthPolicy.grade(equipment).value()),
                    "Enhancement: +" + enhancementService.getLevel(equipment) + "/" + enhancementService.getMaximumLevel(equipment),
                    "Promotion stage: " + promotionService.getStage(equipment),
                    "Missing durability: " + missing + "/" + quote.maximumDurability(),
                    "Expected cost: " + cost,
                    "Repair: " + (quote.repairable() ? "available" : quote.message())
            )));
        }
        if (inventory.getHolder() instanceof RepairInventoryHolder holder) holder.setPreviewItem(equipment);
        inventory.setItem(4, icon(Material.IRON_INGOT, "수리 정보", List.of(
                "손실 내구도: " + missing,
                "필요 코인: " + cost
        )));
        inventory.setItem(EXECUTE_SLOT, icon(Material.LIME_DYE, "수리 실행", List.of("좌클릭으로 수리합니다.")));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "뒤로", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack icon(Material material, String name, List<String> lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lines.stream().map(line -> Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private boolean consumeMaterial(Inventory inventory, int slot, int amount, String materialName) {
        if (amount <= 0) {
            inventory.getViewers().forEach(viewer -> viewer.sendMessage(Component.text(
                    materialName + " 비용 설정을 읽지 못했습니다. 관리자에게 설정을 확인해달라고 요청해주세요.", NamedTextColor.RED)));
            return false;
        }
        ItemStack current = inventory.getItem(slot);
        if (current == null || current.getAmount() < amount) {
            return false;
        }
        int remaining = current.getAmount() - amount;
        if (remaining <= 0) {
            inventory.setItem(slot, null);
        } else {
            ItemStack updated = current.clone();
            updated.setAmount(remaining);
            inventory.setItem(slot, updated);
        }
        return true;
    }

    private String percent(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", value * 100.0D);
    }
}
