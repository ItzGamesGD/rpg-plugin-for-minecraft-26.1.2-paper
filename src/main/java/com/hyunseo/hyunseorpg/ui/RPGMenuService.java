package com.hyunseo.hyunseorpg.ui;

import com.hyunseo.hyunseorpg.boss.BossSessionManager;
import com.hyunseo.hyunseorpg.boss.BossType;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentGrowthGuiService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeRegistry;
import com.hyunseo.hyunseorpg.crafting.CraftingGuiService;
import com.hyunseo.hyunseorpg.crafting.CraftingTransactionService;
import com.hyunseo.hyunseorpg.crafting.SoulboundItemService;
import com.hyunseo.hyunseorpg.farming.FarmingHubGuiService;
import com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiControllerService;
import com.hyunseo.hyunseorpg.quest.AutoQuestData;
import com.hyunseo.hyunseorpg.quest.AutoQuestService;
import com.hyunseo.hyunseorpg.quest.QuestData;
import com.hyunseo.hyunseorpg.quest.QuestService;
import com.hyunseo.hyunseorpg.shop.ShopData;
import com.hyunseo.hyunseorpg.shop.ShopGuiService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Single navigation surface for crafting, shops, growth and bosses. */
public final class RPGMenuService {
    public static final String MAIN_MENU_CRAFTING_LABEL = "제작";
    private final JavaPlugin plugin;
    private final ShopGuiService shops;
    private final EquipmentGrowthGuiService growth;
    private final StatGuiService stats;
    private final BossSessionManager bosses;
    private final CraftingGuiService craftingGuiService;
    private final CoinService coins;
    private final RPGItemService items;
    private final CraftingRecipeRegistry recipes;
    private final CraftingTransactionService craftingTransactions;
    private final SoulboundItemService soulbound;
    private final QuestService quests;
    private final AutoQuestService autoQuests;
    private FarmingHubGuiService farmingHubGuiService;
    private AlchemyGuiControllerService alchemyGuiController;
    private final Map<String, Long> abandonConfirmations = new ConcurrentHashMap<>();

    public RPGMenuService(JavaPlugin plugin, ShopGuiService shops, EquipmentGrowthGuiService growth,
                          StatGuiService stats, BossSessionManager bosses, CraftingGuiService craftingGuiService,
                          CoinService coins, RPGItemService items, CraftingRecipeRegistry recipes,
                          CraftingTransactionService craftingTransactions, SoulboundItemService soulbound,
                          QuestService quests, AutoQuestService autoQuests) {
        this.plugin = plugin;
        this.shops = shops;
        this.growth = growth;
        this.stats = stats;
        this.bosses = bosses;
        this.craftingGuiService = craftingGuiService;
        this.coins = coins;
        this.items = items;
        this.recipes = recipes;
        this.craftingTransactions = craftingTransactions;
        this.soulbound = soulbound;
        this.quests = quests;
        this.autoQuests = autoQuests;
    }

    public void setFarmingHubGuiService(FarmingHubGuiService farmingHubGuiService) {
        this.farmingHubGuiService = farmingHubGuiService;
    }

    public void setAlchemyGuiController(AlchemyGuiControllerService alchemyGuiController) {
        this.alchemyGuiController = alchemyGuiController;
    }

    public void openMain(Player player) {
        RPGMenuHolder holder = new RPGMenuHolder(RPGMenuHolder.View.MAIN);
        Inventory inventory = create(holder, "HyunseoRPG 메뉴");
        inventory.setItem(13, icon(Material.CRAFTING_TABLE, MAIN_MENU_CRAFTING_LABEL, List.of("재료와 장비 제작")));
        inventory.setItem(15, icon(Material.EMERALD, "상점", List.of("등록된 상점 목록")));
        inventory.setItem(17, icon(Material.WHEAT, "농사", List.of("가공, 요리, 배달 의뢰, 농사 정보")));
        inventory.setItem(19, icon(Material.BREWING_STAND, "양조", List.of(
                alchemyGuiController != null && alchemyGuiController.enabled()
                        ? "정수 제작, 물약 제작, 촉매 적용" : "현재 비활성화")));
        inventory.setItem(29, icon(Material.NETHER_STAR, "보스", List.of("월드 보스 세션과 보상")));
        inventory.setItem(31, icon(Material.BOOK, "퀘스트", List.of("자동 의뢰와 진행도 확인", "클릭: 의뢰 목록 열기")));
        inventory.setItem(33, icon(Material.ANVIL, "장비 성장", List.of("강화, 승급, 인챈트, 수리")));
        inventory.setItem(35, icon(Material.BOOK, "스탯", List.of("스탯 투자 메뉴")));
        inventory.setItem(27, icon(Material.ENCHANTED_BOOK, "인챈트 강화", List.of("인챈트 강화와 추출 메뉴")));
        inventory.setItem(4, icon(Material.PAPER, "현재 상태", List.of("보유 코인: " + coins.getCoins(player))));
        inventory.setItem(49, icon(Material.BARRIER, "닫기", List.of()));
        player.openInventory(inventory);
    }

    public void openEnchantSupport(Player player) {
        RPGMenuHolder holder = new RPGMenuHolder(RPGMenuHolder.View.ENCHANT_SUPPORT);
        Inventory inventory = create(holder, "인챈트 강화");
        inventory.setItem(11, icon(Material.EXPERIENCE_BOTTLE, "인챈트 강화", List.of(
                "아직 구현되지 않은 기능입니다.",
                "클릭해도 재화와 아이템은 소비되지 않습니다.")));
        inventory.setItem(15, icon(Material.ENCHANTED_BOOK, "인챈트 추출", List.of(
                "장비에서 선택한 커스텀 인챈트를 추출합니다.",
                "추출권은 상점 > 보조 아이템에서 구매합니다.")));
        inventory.setItem(49, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inventory);
    }

    public void openCraft(Player player) {
        craftingGuiService.openMain(player);
    }

    public void openCraftItems(Player player, String filter) {
        String category = filter == null ? "" : filter.trim().toLowerCase(java.util.Locale.ROOT);
        if (category.equals("material")) category = "materials";
        if (category.equals("equipment")) category = "equipment";
        craftingGuiService.openCategory(player, category, 0);
    }

    public void openShopList(Player player) {
        RPGMenuHolder holder = new RPGMenuHolder(RPGMenuHolder.View.SHOP_LIST);
        Inventory inventory = create(holder, "상점");
        int slot = 11;
        for (ShopData shop : shops.getShops()) {
            if (slot >= 44) break;
            inventory.setItem(slot, icon(Material.EMERALD, shop.title(), List.of("ID: " + shop.shopId())));
            holder.actions().put(slot, shop.shopId());
            slot++;
        }
        inventory.setItem(49, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inventory);
    }

    public void clickShop(Player player, String shopId) {
        if (!shops.openShop(player, shopId)) player.sendMessage(Component.text("상점을 찾을 수 없습니다.", NamedTextColor.RED));
    }

    public void openBoss(Player player) {
        RPGMenuHolder holder = new RPGMenuHolder(RPGMenuHolder.View.BOSS);
        Inventory inventory = create(holder, "보스 세션");
        int slot = 20;
        for (BossType type : BossType.values()) {
            inventory.setItem(slot, icon(Material.NETHER_STAR, type.configId(), List.of(
                    "상태: " + bosses.status(type), "처치 참여자에게만 보상이 지급됩니다.")));
            holder.actions().put(slot, type.configId());
            slot += 2;
            List<String> bossLore = new java.util.ArrayList<>();
            bossLore.add("상태: " + bosses.status(type));
            bossLore.addAll(bosses.menuLore(type));
            inventory.setItem(slot - 2, icon(Material.NETHER_STAR, type.configId(), bossLore));
        }
        inventory.setItem(49, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inventory);
    }

    public void clickBoss(Player player, String bossId) {
        BossType.fromInput(bossId).ifPresentOrElse(type -> player.sendMessage(Component.text(
                type.configId() + " 보스: " + bosses.status(type), NamedTextColor.AQUA)),
                () -> player.sendMessage(Component.text("보스 정보를 찾을 수 없습니다.", NamedTextColor.RED)));
    }

    public void openGrowth(Player player) {
        if (player != null && player.isOnline()) {
            growth.openMain(player);
            return;
        }
        RPGMenuHolder holder = new RPGMenuHolder(RPGMenuHolder.View.GROWTH);
        Inventory inventory = create(holder, "장비 성장");
        inventory.setItem(10, icon(Material.ANVIL, "강화", List.of("기본 능력치 성장")));
        inventory.setItem(12, icon(Material.SMITHING_TABLE, "승급", List.of("성장 단계와 상한 해방")));
        inventory.setItem(14, icon(Material.ENCHANTED_BOOK, "인챈트", List.of("마석과 인챈트 북 사용")));
        inventory.setItem(16, icon(Material.IRON_INGOT, "수리", List.of("코인으로 내구도 회복")));
        inventory.setItem(49, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inventory);
    }

    public void clickMain(Player player, int slot) {
        switch (slot) {
            case 13 -> craftingGuiService.open(player);
            case 15 -> openShopList(player);
            case 17 -> {
                if (farmingHubGuiService != null) farmingHubGuiService.open(player);
                else player.sendMessage(Component.text("농사 메뉴를 사용할 수 없습니다.", NamedTextColor.RED));
            }
            case 19 -> {
                if (alchemyGuiController == null || !alchemyGuiController.openInventory(player)) {
                    player.sendMessage(Component.text("양조 메뉴는 현재 비활성화되어 있습니다.", NamedTextColor.RED));
                }
            }
            case 29 -> openBoss(player);
            case 31 -> openQuests(player);
            case 33 -> openGrowth(player);
            case 35 -> stats.openStatGui(player);
            case 27 -> openEnchantSupport(player);
            case 4 -> player.sendMessage(Component.text("보유 코인: " + coins.getCoins(player), NamedTextColor.GOLD));
            case 49 -> player.closeInventory();
            default -> { }
        }
    }

    public void openQuests(Player player) {
        autoQuests.refresh(player);
        RPGMenuHolder holder = new RPGMenuHolder(RPGMenuHolder.View.QUESTS);
        Inventory inventory = create(holder, "퀘스트");
        int slot = 10;
        for (AutoQuestData quest : autoQuests.getQuests(player)) {
            if (slot >= 44) break;
            int progress = autoQuests.displayedProgress(player, quest);
            boolean complete = autoQuests.isComplete(player, quest);
            String action = complete ? "클릭: 보상 받기" : "클릭: 의뢰 포기";
            inventory.setItem(slot, icon(complete ? Material.GOLD_INGOT : Material.PAPER,
                    "[" + quest.slot() + "] " + quest.displayName(),
                    List.of("목표: " + progress + "/" + quest.amount(), action)));
            ItemStack displayed = inventory.getItem(slot);
            if (displayed != null) {
                List<String> details = new ArrayList<>(autoQuests.progressLines(player, quest));
                details.add("남은 시간: " + formatSeconds(Math.max(0L, (quest.expiresAt() - System.currentTimeMillis() + 999L) / 1000L)));
                details.add("보상: " + quest.rewardCoins() + " 코인 / " + quest.rewardExp() + " RPG 경험치");
                details.add("Shift + 우클릭: 포기");
                addLore(displayed, details);
            }
            holder.actions().put(slot, "AUTO:" + (complete ? "COMPLETE:" : "ABANDON:") + quest.slot());
            slot++;
        }
        for (QuestData quest : quests.getQuests()) {
            if (slot >= 44) break;
            String state = quests.getQuestState(player, quest.questId());
            inventory.setItem(slot, icon(Material.BOOK, quest.displayName(),
                    List.of("상태: " + state, "클릭: " + ("ACTIVE".equals(state) ? "의뢰 포기" : "의뢰 수락/보상 받기"))));
            holder.actions().put(slot, "STATIC:" + quest.questId());
            slot++;
        }
        if (slot == 10) inventory.setItem(22, icon(Material.BARRIER, "진행 중인 퀘스트 없음",
                List.of("잠시 후 /quest로 새 의뢰를 받을 수 있습니다.")));
        inventory.setItem(49, icon(Material.ARROW, "뒤로", List.of()));
        int active = autoQuests.getQuests(player).size();
        long cooldown = autoQuests.remainingCooldownSeconds(player);
        inventory.setItem(31, icon(Material.WRITABLE_BOOK, "새 퀘스트 받기", List.of(
                "활성 퀘스트: " + active + "/" + autoQuests.maxActive(),
                cooldown > 0L ? "대기 시간: " + formatSeconds(cooldown) : "좌클릭: 현재 가능한 퀘스트 수락")));
        holder.actions().put(31, "AUTO:ACCEPT");
        inventory.setItem(33, icon(Material.CLOCK, "퀘스트 대기 시간", List.of(
                cooldown > 0L ? formatSeconds(cooldown) : "즉시 수락 가능")));
        player.openInventory(inventory);
    }

    public void clickQuest(Player player, String action, boolean abandonGesture) {
        if ("AUTO:ACCEPT".equals(action)) {
            autoQuests.accept(player);
            Bukkit.getScheduler().runTask(plugin, () -> openQuests(player));
            return;
        }
        if (action != null && action.startsWith("AUTO:ABANDON:")) {
            if (!abandonGesture) {
                player.sendMessage(Component.text("Shift + 우클릭을 두 번 사용하면 퀘스트를 포기합니다.", NamedTextColor.YELLOW));
                return;
            }
            String key = player.getUniqueId() + ":" + action.substring("AUTO:ABANDON:".length());
            long now = System.currentTimeMillis();
            long previous = abandonConfirmations.getOrDefault(key, 0L);
            if (now - previous > 5_000L) {
                abandonConfirmations.put(key, now);
                player.sendMessage(Component.text("5초 안에 Shift + 우클릭을 한 번 더 하면 퀘스트를 포기합니다.", NamedTextColor.YELLOW));
                return;
            }
            abandonConfirmations.remove(key);
        }
        clickQuest(player, action);
    }

    public void clickQuest(Player player, String action) {
        if (action == null || action.isBlank()) return;
        String[] parts = action.split(":", 3);
        if (parts.length >= 3 && parts[0].equals("AUTO")) {
            try {
                int slot = Integer.parseInt(parts[2]);
                if (parts[1].equals("COMPLETE")) autoQuests.complete(player, slot);
                else autoQuests.abandon(player, slot);
            } catch (NumberFormatException ignored) { }
            Bukkit.getScheduler().runTask(plugin, () -> openQuests(player));
            return;
        }
        if (parts.length == 2 && parts[0].equals("STATIC")) {
            String questId = parts[1];
            String state = quests.getQuestState(player, questId);
            if ("ACTIVE".equals(state)) quests.cancelQuest(player, questId);
            else if ("READY_TO_CLAIM".equals(state)) quests.claimReward(player, questId);
            else quests.startQuest(player, questId);
            Bukkit.getScheduler().runTask(plugin, () -> openQuests(player));
        }
    }

    private String formatSeconds(long seconds) {
        long safe = Math.max(0L, seconds);
        return safe / 60L + "분 " + safe % 60L + "초";
    }

    public void clickCraftCategory(Player player, int slot) {
        if (slot == 11) openCraftItems(player, "MATERIAL");
        if (slot == 15) openCraftItems(player, "EQUIPMENT");
    }

    public void clickGrowth(Player player, int slot) {
        switch (slot) {
            case 10 -> player.sendMessage(Component.text("강화는 바닐라 모루에 장비와 강화석을 넣어 진행합니다.", NamedTextColor.YELLOW));
            case 12 -> growth.openPromotion(player);
            case 14 -> growth.openEnchant(player);
            case 16 -> growth.openRepair(player);
            default -> { }
        }
    }

    public void clickEnchantSupport(Player player, int slot) {
        switch (slot) {
            case 11 -> player.sendMessage(Component.text("인챈트 강화는 아직 구현되지 않았습니다. 재화와 아이템은 소비되지 않았습니다.", NamedTextColor.YELLOW));
            case 15 -> {
                if (growth.getSupportService() != null) growth.getSupportService().openExtraction(player);
                else player.sendMessage(Component.text("인챈트 추출 메뉴를 사용할 수 없습니다.", NamedTextColor.RED));
            }
            default -> { }
        }
    }

    public void back(Player player) { openMain(player); }

    private Inventory create(RPGMenuHolder holder, String title) {
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text(title));
        holder.setInventory(inventory);
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
        return inventory;
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void addLore(ItemStack item, List<String> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.addAll(lines.stream().map(line -> Component.text(line, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)).toList());
        meta.lore(lore);
        item.setItemMeta(meta);
    }
}
