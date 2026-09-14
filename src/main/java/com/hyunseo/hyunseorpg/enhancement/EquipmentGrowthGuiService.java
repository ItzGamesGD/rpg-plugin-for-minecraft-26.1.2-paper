package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
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

/** Legacy growth menu retained for repair navigation; enhancement itself uses the vanilla anvil. */
public final class EquipmentGrowthGuiService {
    public static final int EQUIPMENT_SLOT = 10;
    public static final int STONE_SLOT = 16;
    public static final int EXECUTE_SLOT = 22;
    public static final int BACK_SLOT = 18;
    public static final int CLOSE_SLOT = 26;
    private static final Set<Integer> INPUT_SLOTS = Set.of(EQUIPMENT_SLOT, STONE_SLOT);

    private final JavaPlugin plugin;
    private final EquipmentEnhancementService enhancementService;
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private EquipmentRepairService repairService;
    private EquipmentGrowthPolicy growthPolicy;
    private EquipmentSupportGuiService supportService;
    private com.hyunseo.hyunseorpg.shop.ShopGuiService shopGuiService;

    public EquipmentGrowthGuiService(JavaPlugin plugin, EquipmentEnhancementService enhancementService) {
        this.plugin = plugin;
        this.enhancementService = enhancementService;
    }

    public void setRepairService(EquipmentRepairService repairService) { this.repairService = repairService; }
    public void setGrowthPolicy(EquipmentGrowthPolicy growthPolicy) { this.growthPolicy = growthPolicy; }
    public void setSupportService(EquipmentSupportGuiService supportService) { this.supportService = supportService; }
    public void setShopGuiService(com.hyunseo.hyunseorpg.shop.ShopGuiService shopGuiService) { this.shopGuiService = shopGuiService; }
    public EquipmentSupportGuiService getSupportService() { return supportService; }
    public com.hyunseo.hyunseorpg.shop.ShopGuiService getShopGuiService() { return shopGuiService; }
    public boolean isInputSlot(int slot) { return INPUT_SLOTS.contains(slot); }

    public void openMain(Player player) {
        EquipmentGrowthMenuHolder holder = new EquipmentGrowthMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("장비 성장"));
        holder.setInventory(inventory);
        fill(inventory);
        inventory.setItem(24, icon(Material.KNOWLEDGE_BOOK, "향후 확장", List.of("아직 구현되지 않은 기능 목록")));
        inventory.setItem(11, icon(Material.ANVIL, "강화", List.of("바닐라 모루에 장비와 강화석을 넣어 강화합니다.")));
        inventory.setItem(15, icon(Material.ENCHANTED_BOOK, "인챈트", List.of("인챈팅 테이블과 바닐라 모루를 사용합니다.")));
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

    /** Legacy menu compatibility; normal enhancement is entered through a vanilla anvil. */
    public void enhance(Player player, Inventory inventory) {
        if (!processing.add(player.getUniqueId())) return;
        try {
            ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
            ItemStack stone = inventory.getItem(STONE_SLOT);
            Optional<EnhancementLevelData> next = enhancementService.getNextLevel(equipment);
            if (next.isEmpty() || stone == null || !enhancementService.isRequiredStone(stone)
                    || stone.getAmount() < 1) {
                player.sendMessage(Component.text("강화 가능한 장비와 강화석을 확인하세요.", NamedTextColor.RED));
                return;
            }
            if (!consumeMaterial(inventory, STONE_SLOT, 1, "강화석")) return;
            enhancementService.applySuccessfulEnhancement(equipment, next.get());
            inventory.setItem(EQUIPMENT_SLOT, equipment);
            player.sendMessage(Component.text("강화 성공: +" + next.get().level(), NamedTextColor.GREEN));
        } finally {
            renderEnhancement(inventory);
            processing.remove(player.getUniqueId());
        }
    }

    public void refreshLater(Inventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inventory.getHolder() instanceof EnhancementInventoryHolder) renderEnhancement(inventory);
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
            if (top.getHolder() instanceof EnhancementInventoryHolder
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
                    "Enhancement: " + (growthPolicy.canEnhance(equipment) ? "available" : "unavailable"),
                    "Current: +" + enhancementService.getLevel(equipment) + "/" + enhancementService.getMaximumLevel(equipment)
            )));
        }
        inventory.setItem(4, icon(Material.BOOK, "강화 정보", next.map(rule -> List.of(
                "현재 강화: +" + enhancementService.getLevel(equipment),
                "다음 강화: +" + rule.level(),
                "필요 강화석: " + rule.stoneCost(),
                "필요 XP 레벨: " + enhancementService.getXpLevelCost(rule.level()),
                "성공 확률: 100%"
        )).orElse(List.of("강화 가능한 장비를 넣어주세요."))));
        inventory.setItem(EXECUTE_SLOT, icon(Material.LIME_DYE, "강화 실행", next.map(rule -> List.of(
                "다음 단계: +" + rule.level(),
                "필요 강화석: " + rule.stoneCost(),
                "필요 XP 레벨: " + enhancementService.getXpLevelCost(rule.level()),
                "성공 확률: 100%",
                "좌클릭으로 실행합니다."
        )).orElse(List.of("강화 가능한 장비가 없습니다."))));
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
                    "Enhancement: +" + enhancementService.getLevel(equipment) + "/" + enhancementService.getMaximumLevel(equipment),
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
