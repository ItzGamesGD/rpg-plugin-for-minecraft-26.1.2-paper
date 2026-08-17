package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.enchant.EnchantService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.shop.ShopGuiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

/**
 * Equipment support flows. The source item is never placed in these views;
 * every action re-finds the source by its equipment instance UUID.
 */
public final class EquipmentSupportGuiService implements Listener {
    private static final int PAGE_SIZE = 45;
    private static final int PREVIOUS = 45;
    private static final int NEXT = 53;
    private static final int EXECUTE = 49;
    private static final int BACK = 48;
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final RPGItemService items;
    private final EquipmentTierService tiers;
    private final EquipmentGrowthPolicy growthPolicy;
    private final EquipmentInstanceService instances;
    private final EquipmentPromotionService promotions;
    private final EnchantService enchants;
    private final CoinService coins;
    private final ShopGuiService shops;
    private final FutureEquipmentFeatureRegistry future;
    private final Map<UUID, UUID> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, SourceRef> cache = new ConcurrentHashMap<>();
    private final Map<RerollClickKey, Long> rerollClicks = new ConcurrentHashMap<>();

    public EquipmentSupportGuiService(JavaPlugin plugin, ConfigService config, RPGItemService items,
                                      EquipmentTierService tiers, EquipmentGrowthPolicy growthPolicy,
                                      EquipmentInstanceService instances, EquipmentPromotionService promotions,
                                      EnchantService enchants, CoinService coins, ShopGuiService shops,
                                      FutureEquipmentFeatureRegistry future) {
        this.plugin = plugin;
        this.config = config;
        this.items = items;
        this.tiers = tiers;
        this.growthPolicy = growthPolicy;
        this.instances = instances;
        this.promotions = promotions;
        this.enchants = enchants;
        this.coins = coins;
        this.shops = shops;
        this.future = future;
    }

    public void clearSessions() {
        sessions.clear();
        cache.clear();
        rerollClicks.clear();
    }

    public void openExtraction(Player player) { openExtraction(player, 0); }
    public void openReroll(Player player) { openReroll(player, 0); }

    public void openExtraction(Player player, int page) {
        List<SourceRef> list = sources(player, true, false);
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.EXTRACTION_LIST, page);
        Inventory inv = inventory(holder, "장비 인챈트 추출");
        renderSources(inv, holder, list, "인챈트 추출 대상", "클릭하여 인챈트를 선택");
        player.openInventory(inv);
    }

    public void openReroll(Player player, int page) {
        List<SourceRef> list = sources(player, false, true);
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.REROLL_LIST, page);
        Inventory inv = inventory(holder, "승급 옵션 초기화");
        renderSources(inv, holder, list, "승급 옵션 장비", "클릭하여 옵션을 선택");
        player.openInventory(inv);
    }

    public void openFuture(Player player) {
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.FUTURE, 0);
        Inventory inv = inventory(holder, "향후 장비 확장");
        int slot = 10;
        for (FutureEquipmentFeatureRegistry.Feature feature : future.getAll()) {
            if (slot >= PAGE_SIZE) break;
            holder.featureIds().put(slot, feature.id());
            inv.setItem(slot++, icon(Material.KNOWLEDGE_BOOK, feature.displayName(), List.of(
                    feature.description(), feature.enabled() && feature.implemented() ? "사용 가능" : "아직 구현되지 않은 기능", 
                    "클릭하여 상세 정보 확인")));
        }
        footer(inv, false);
        player.openInventory(inv);
    }

    private void openFutureDetail(Player player, String featureId) {
        FutureEquipmentFeatureRegistry.Feature feature = future.getAll().stream()
                .filter(candidate -> candidate.id().equals(featureId)).findFirst().orElse(null);
        if (feature == null) {
            player.sendMessage(Component.text("향후 기능 정보를 찾을 수 없습니다.", NamedTextColor.RED));
            return;
        }
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.FUTURE_DETAIL, 0);
        holder.setSelectedFeatureId(feature.id());
        Inventory inv = inventory(holder, feature.displayName());
        inv.setItem(13, icon(Material.KNOWLEDGE_BOOK, feature.displayName(), List.of(
                feature.description(), feature.enabled() && feature.implemented() ? "사용 가능" : "아직 구현되지 않은 기능")));
        inv.setItem(EXECUTE, icon(Material.GRAY_WOOL, "실행", List.of(
                "아직 구현되지 않은 기능입니다.", "재화와 아이템은 소비되지 않습니다.")));
        inv.setItem(BACK, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inv);
    }

    private EquipmentSupportMenuHolder holder(EquipmentSupportMenuHolder.View view, int page) {
        return new EquipmentSupportMenuHolder(view, page);
    }

    private Inventory inventory(EquipmentSupportMenuHolder holder, String title) {
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(title));
        holder.setInventory(inv);
        return inv;
    }

    private void renderSources(Inventory inv, EquipmentSupportMenuHolder holder, List<SourceRef> list,
                               String title, String action) {
        int pageCount = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(holder.page(), pageCount - 1);
        holder.setPage(page);
        for (int i = page * PAGE_SIZE; i < list.size() && i < (page + 1) * PAGE_SIZE; i++) {
            SourceRef source = list.get(i);
            int slot = i - page * PAGE_SIZE;
            holder.sources().put(slot, source.id());
            inv.setItem(slot, display(source.item(), title, action));
            cache.put(source.id(), source);
        }
        footer(inv, page > 0 || page < pageCount - 1);
        if (page > 0) inv.setItem(PREVIOUS, icon(Material.ARROW, "이전 페이지", List.of()));
        if (page < pageCount - 1) inv.setItem(NEXT, icon(Material.ARROW, "다음 페이지", List.of()));
    }

    private void openEnchantChoices(Player player, EquipmentSupportMenuHolder old, UUID sourceId) {
        SourceRef source = findSource(player, sourceId);
        if (source == null) { player.sendMessage(Component.text("원본 장비를 찾을 수 없습니다.", NamedTextColor.RED)); return; }
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.EXTRACTION_ENCHANTS, 0);
        bindSource(player, holder, source);
        Inventory inv = inventory(holder, "추출할 인챈트 선택");
        int slot = 10;
        for (EnchantService.AppliedEnchant applied : enchants.getAppliedEnchants(source.item())) {
            if (slot >= 35) break;
            holder.choices().put(slot, applied.id());
            inv.setItem(slot++, icon(Material.ENCHANTED_BOOK, applied.id(), List.of(
                    "현재 레벨: " + applied.level(), "클릭하여 추출 정보 확인")));
        }
        inv.setItem(BACK, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inv);
    }

    private void openExtractionConfirm(Player player, EquipmentSupportMenuHolder old, String enchantId) {
        SourceRef source = findSource(player, old.sourceId());
        if (source == null) { player.sendMessage(Component.text("원본 장비를 찾을 수 없습니다.", NamedTextColor.RED)); return; }
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.EXTRACTION_CONFIRM, 0);
        bindSource(player, holder, source);
        holder.setSelectedId(enchantId);
        int level = enchants.getEnchantLevel(source.item(), enchantId);
        holder.setSourceEnchantLevel(level);
        Inventory inv = inventory(holder, "인챈트 추출 확인");
        inv.setItem(13, display(source.item(), "추출 장비", "선택 인챈트: " + enchantId + " " + level));
        inv.setItem(EXECUTE, icon(Material.LIME_WOOL, "추출 실행", List.of(
                "필요 추출권: " + ticketAmount("extraction"),
                "필요 코인: " + currencyCost("extraction"), "클릭하여 실행")));
        inv.setItem(BACK, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inv);
    }

    private void openOptionChoices(Player player, EquipmentSupportMenuHolder old, UUID sourceId) {
        SourceRef source = findSource(player, sourceId);
        if (source == null) { player.sendMessage(Component.text("원본 장비를 찾을 수 없습니다.", NamedTextColor.RED)); return; }
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.REROLL_OPTIONS, 0);
        bindSource(player, holder, source);
        Inventory inv = inventory(holder, "초기화할 승급 옵션 선택");
        int slot = 10;
        for (String id : promotions.getOptionIds(source.item())) {
            if (slot >= 35) break;
            double value = promotions.getOptionValue(source.item(), id);
            holder.choices().put(slot, id);
            inv.setItem(slot++, icon(Material.NETHER_STAR, promotions.getOptionDisplayName(id), List.of(
                    "현재: " + value, "범위: " + promotions.getOptionBounds(id).minimum() + " - " + promotions.getOptionBounds(id).maximum(),
                    "클릭하여 초기화 정보 확인")));
        }
        inv.setItem(BACK, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inv);
    }

    private void openRerollConfirm(Player player, EquipmentSupportMenuHolder old, String optionId) {
        SourceRef source = findSource(player, old.sourceId());
        if (source == null) { player.sendMessage(Component.text("원본 장비를 찾을 수 없습니다.", NamedTextColor.RED)); return; }
        double current = promotions.getOptionValue(source.item(), optionId);
        EquipmentPromotionService.OptionBounds bounds = promotions.getOptionBounds(optionId);
        EquipmentSupportMenuHolder holder = holder(EquipmentSupportMenuHolder.View.REROLL_CONFIRM, 0);
        bindSource(player, holder, source);
        holder.setSelectedId(optionId);
        holder.setSnapshotValue(current);
        holder.optionSnapshot().putAll(promotions.getOptions(source.item()));
        Inventory inv = inventory(holder, "승급 옵션 초기화 확인");
        inv.setItem(13, display(source.item(), "옵션 초기화 장비", "선택 옵션: " + promotions.getOptionDisplayName(optionId)));
        inv.setItem(22, icon(Material.LIME_WOOL, "초기화 실행", List.of(
                "현재 값: " + current, "가능 범위: " + bounds.minimum() + " - " + bounds.maximum(),
                "현재 값보다 낮아지지 않습니다.", "필요 초기화권: " + ticketAmount("promotion-option-reroll"),
                "필요 코인: " + currencyCost("promotion-option-reroll"))));
        inv.setItem(BACK, icon(Material.ARROW, "뒤로", List.of()));
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof EquipmentSupportMenuHolder holder)) return;
        event.setCancelled(true);
        if (unsafe(event.getClick()) || event.isShiftClick() || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (slot == PREVIOUS || slot == NEXT) {
            if (holder.view() == EquipmentSupportMenuHolder.View.EXTRACTION_LIST) openExtraction(player, holder.page() + (slot == NEXT ? 1 : -1));
            else if (holder.view() == EquipmentSupportMenuHolder.View.REROLL_LIST) openReroll(player, holder.page() + (slot == NEXT ? 1 : -1));
            return;
        }
        if (slot == BACK) { openFromView(player, holder); return; }
        switch (holder.view()) {
            case EXTRACTION_LIST -> { UUID id = holder.sources().get(slot); if (id != null) openEnchantChoices(player, holder, id); }
            case EXTRACTION_ENCHANTS -> { String id = holder.choices().get(slot); if (id != null) openExtractionConfirm(player, holder, id); }
            case EXTRACTION_CONFIRM -> { if (slot == EXECUTE) extract(player, holder); }
            case REROLL_LIST -> { UUID id = holder.sources().get(slot); if (id != null) openOptionChoices(player, holder, id); }
            case REROLL_OPTIONS -> { String id = holder.choices().get(slot); if (id != null) openRerollConfirm(player, holder, id); }
            case REROLL_CONFIRM -> { if (slot == 22) reroll(player, holder, event.getClick()); }
            case FUTURE -> { String id = holder.featureIds().get(slot); if (id != null) openFutureDetail(player, id); }
            case FUTURE_DETAIL -> {
                if (slot == EXECUTE) player.sendMessage(Component.text("아직 구현되지 않은 기능입니다. 재화와 아이템은 소비되지 않았습니다.", NamedTextColor.YELLOW));
            }
        }
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EquipmentSupportMenuHolder
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) event.setCancelled(true);
    }

    @EventHandler public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && event.getInventory().getHolder() instanceof EquipmentSupportMenuHolder) {
            sessions.remove(player.getUniqueId());
        }
    }

    private void extract(Player player, EquipmentSupportMenuHolder holder) {
        if (!processing(player, holder)) return;
        try {
            SourceRef source = findSource(player, holder.sourceId());
            int level = source == null ? 0 : enchants.getEnchantLevel(source.item(), holder.selectedId());
            ItemStack book = source == null ? null : enchants.createBook(holder.selectedId(), level).orElse(null);
            if (!matchesExtractionSnapshot(player, holder, source) || level <= 0 || book == null
                    || !canFit(player, book) || !allowed(source.item(), "extraction")
                    || !hasTickets(player, "extraction")) {
                player.sendMessage(Component.text("추출 조건을 충족하지 못했습니다.", NamedTextColor.RED));
                return;
            }

            ItemStack original = source.item().clone();
            ItemStack candidate = original.clone();
            if (!enchants.removeEnchant(candidate, holder.selectedId())
                    || enchants.hasEquipped(candidate, holder.selectedId())) {
                player.sendMessage(Component.text("선택 인챈트를 안전하게 제거할 수 없어 취소되었습니다.", NamedTextColor.RED));
                return;
            }
            long cost = currencyCost("extraction");
            if (!coins.takeCoins(player, cost)) {
                player.sendMessage(Component.text("코인이 부족합니다.", NamedTextColor.RED));
                return;
            }
            TicketPlan ticket = removeTickets(player, "extraction");
            if (ticket == null) {
                coins.addCoins(player, cost);
                player.sendMessage(Component.text("추출권이 부족합니다.", NamedTextColor.RED));
                return;
            }
            try {
                player.getInventory().setItem(source.slot(), candidate);
                SourceRef applied = findSource(player, holder.sourceId());
                if (!matchesExtractionResult(applied, holder)) throw new IllegalStateException("equipment verification failed");
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(book);
                if (!overflow.isEmpty()) throw new IllegalStateException("output inventory is full");
            } catch (RuntimeException exception) {
                player.getInventory().setItem(source.slot(), original);
                ticket.restore(player);
                coins.addCoins(player, cost);
                player.sendMessage(Component.text("추출 처리에 실패해 원상 복구했습니다.", NamedTextColor.RED));
                plugin.getLogger().warning("Enchant extraction rolled back: " + exception.getMessage());
                return;
            }
            player.sendMessage(Component.text("인챈트를 추출했습니다: " + holder.selectedId(), NamedTextColor.GREEN));
            openExtraction(player);
        } finally {
            sessions.remove(player.getUniqueId());
        }
    }

    private void reroll(Player player, EquipmentSupportMenuHolder holder, ClickType click) {
        if (!processing(player, holder)) return;
        try {
            SourceRef source = findSource(player, holder.sourceId());
            if (!matchesSnapshot(player, holder, source)
                    || !allowed(source.item(), "promotion-option-reroll")
                    || !promotions.getOptionIds(source.item()).contains(holder.selectedId())
                    || Math.abs(promotions.getOptionValue(source.item(), holder.selectedId()) - holder.snapshotValue()) > 0.000001D) {
                player.sendMessage(Component.text("장비 또는 옵션이 변경되어 취소되었습니다.", NamedTextColor.RED));
                return;
            }
            Map<String, Double> before = new LinkedHashMap<>(promotions.getOptions(source.item()));
            EquipmentPromotionService.OptionBounds bounds = promotions.getOptionBounds(holder.selectedId());
            if (config.getEquipmentSupportBoolean("promotion-option-reroll.block-at-maximum", true)
                    && holder.snapshotValue() >= bounds.maximum() - 0.000001D) {
                player.sendMessage(Component.text("이미 최대 옵션 수치입니다.", NamedTextColor.YELLOW));
                return;
            }
            var value = promotions.rerollOptionValue(holder.selectedId(), holder.snapshotValue());
            if (value.isEmpty()) {
                player.sendMessage(Component.text("초기화할 수 없는 옵션입니다.", NamedTextColor.RED));
                return;
            }

            // Build and validate the complete result on a clone before consuming anything.
            ItemStack candidate = source.item().clone();
            if (!promotions.replaceOption(candidate, holder.selectedId(), value.getAsDouble())) {
                player.sendMessage(Component.text("옵션 결과를 저장할 수 없어 취소되었습니다.", NamedTextColor.RED));
                return;
            }
            Map<String, Double> after = new LinkedHashMap<>(promotions.getOptions(candidate));
            double storedValue = after.getOrDefault(holder.selectedId(), Double.NaN);
            if (!Double.isFinite(storedValue) || Math.abs(storedValue - value.getAsDouble()) > 0.000001D
                    || !instances.is(candidate, holder.sourceId())) {
                player.sendMessage(Component.text("옵션 결과 검증에 실패해 취소되었습니다.", NamedTextColor.RED));
                return;
            }
            if (!hasTickets(player, "promotion-option-reroll")) {
                player.sendMessage(Component.text("초기화권이 부족합니다.", NamedTextColor.RED));
                return;
            }
            long cost = currencyCost("promotion-option-reroll");
            if (!coins.takeCoins(player, cost)) {
                player.sendMessage(Component.text("코인이 부족합니다.", NamedTextColor.RED));
                return;
            }
            TicketPlan ticket = removeTickets(player, "promotion-option-reroll");
            if (ticket == null) {
                coins.addCoins(player, cost);
                player.sendMessage(Component.text("초기화권이 부족합니다.", NamedTextColor.RED));
                return;
            }
            ItemStack original = source.item().clone();
            try {
                player.getInventory().setItem(source.slot(), candidate);
                SourceRef applied = findSource(player, holder.sourceId());
                if (applied == null || !matchesSnapshotAfterWrite(player, holder, applied, storedValue)) {
                    player.getInventory().setItem(source.slot(), original);
                    ticket.restore(player);
                    coins.addCoins(player, cost);
                    player.sendMessage(Component.text("장비 반영에 실패해 취소되었습니다.", NamedTextColor.RED));
                    return;
                }
            } catch (RuntimeException exception) {
                player.getInventory().setItem(source.slot(), original);
                ticket.restore(player);
                coins.addCoins(player, cost);
                plugin.getLogger().warning("Promotion option reroll rolled back: " + exception.getMessage());
                player.sendMessage(Component.text("리롤 처리 중 오류가 발생해 취소되었습니다.", NamedTextColor.RED));
                return;
            }

            player.sendMessage(Component.text("[승급 옵션 리롤]", NamedTextColor.GREEN));
            player.sendMessage(Component.text("장비: " + equipmentLabel(candidate), NamedTextColor.GRAY));
            sendOptionChanges(player, before, after);
            holder.optionSnapshot().clear();
            holder.optionSnapshot().putAll(after);
            holder.setSnapshotValue(storedValue);
            renderRerollConfirm(player, holder, appliedSource(player, holder.sourceId()));
        } finally {
            sessions.remove(player.getUniqueId(), holder.sessionId());
            rerollClicks.remove(new RerollClickKey(player.getUniqueId(), holder.sourceId(), click,
                    plugin.getServer().getCurrentTick()));
        }
    }

    private void bindSource(Player player, EquipmentSupportMenuHolder holder, SourceRef source) {
        holder.setSourceId(source.id());
        holder.setPlayerId(player.getUniqueId());
        holder.setSourceSlot(source.slot());
        holder.setSourceItemId(items.getItemId(source.item()).orElse(""));
        holder.setSourceCategory(tiers.getCategory(source.item()));
        holder.setSourcePromotionStage(promotions.getStage(source.item()));
    }

    private boolean matchesSnapshot(Player player, EquipmentSupportMenuHolder holder, SourceRef source) {
        if (source == null || !player.getUniqueId().equals(holder.playerId())) return false;
        if (!instances.is(source.item(), holder.sourceId())) return false;
        if (!holder.sourceItemId().equals(items.getItemId(source.item()).orElse(""))) return false;
        if (holder.sourceCategory() != tiers.getCategory(source.item())) return false;
        if (!holder.sourcePromotionStage().equals(promotions.getStage(source.item()))) return false;
        return Math.abs(promotions.getOptionValue(source.item(), holder.selectedId()) - holder.snapshotValue()) <= 0.000001D;
    }

    private boolean matchesExtractionSnapshot(Player player, EquipmentSupportMenuHolder holder, SourceRef source) {
        return source != null && player.getUniqueId().equals(holder.playerId())
                && instances.is(source.item(), holder.sourceId())
                && holder.sourceItemId().equals(items.getItemId(source.item()).orElse(""))
                && holder.sourceCategory() == tiers.getCategory(source.item())
                && holder.sourcePromotionStage().equals(promotions.getStage(source.item()))
                && holder.sourceEnchantLevel() > 0
                && enchants.getEnchantLevel(source.item(), holder.selectedId()) == holder.sourceEnchantLevel();
    }

    private boolean matchesExtractionResult(SourceRef source, EquipmentSupportMenuHolder holder) {
        return source != null && instances.is(source.item(), holder.sourceId())
                && !enchants.hasEquipped(source.item(), holder.selectedId());
    }

    private boolean matchesSnapshotAfterWrite(Player player, EquipmentSupportMenuHolder holder, SourceRef source,
                                             double expectedValue) {
        if (source == null || !player.getUniqueId().equals(holder.playerId())) return false;
        if (!instances.is(source.item(), holder.sourceId())) return false;
        if (!holder.sourceItemId().equals(items.getItemId(source.item()).orElse(""))) return false;
        if (holder.sourceCategory() != tiers.getCategory(source.item())) return false;
        if (!holder.sourcePromotionStage().equals(promotions.getStage(source.item()))) return false;
        return Math.abs(promotions.getOptionValue(source.item(), holder.selectedId()) - expectedValue) <= 0.000001D;
    }

    private SourceRef appliedSource(Player player, UUID sourceId) {
        return findSource(player, sourceId);
    }

    private void renderRerollConfirm(Player player, EquipmentSupportMenuHolder holder, SourceRef source) {
        if (source == null || holder.getInventory() == null) return;
        bindSource(player, holder, source);
        EquipmentPromotionService.OptionBounds bounds = promotions.getOptionBounds(holder.selectedId());
        boolean atMaximum = holder.snapshotValue() >= bounds.maximum() - 0.000001D;
        boolean available = !atMaximum && hasTickets(player, "promotion-option-reroll");
        Material button = available ? Material.LIME_WOOL : Material.GRAY_WOOL;
        String state = atMaximum ? "최대 수치라 리롤할 수 없습니다." : available ? "클릭하여 리롤" : "초기화권이 부족합니다.";
        holder.getInventory().setItem(13, display(source.item(), "옵션 초기화 장비", "선택 옵션: " + promotions.getOptionDisplayName(holder.selectedId())));
        holder.getInventory().setItem(22, icon(button, "초기화 실행", List.of(
                "현재 값: " + formatChatValue(holder.selectedId(), holder.snapshotValue()),
                "가능 범위: " + formatChatValue(holder.selectedId(), bounds.minimum()) + " - " + formatChatValue(holder.selectedId(), bounds.maximum()),
                "남은 초기화권: " + ticketCount(player, "promotion-option-reroll"),
                "필요 코인: " + currencyCost("promotion-option-reroll"), state)));
    }

    private void sendOptionChanges(Player player, Map<String, Double> before, Map<String, Double> after) {
        Set<String> ids = new HashSet<>();
        ids.addAll(before.keySet());
        ids.addAll(after.keySet());
        ids.stream().filter(promotions::isPromotionEligible).sorted().forEach(id -> {
            double oldValue = before.getOrDefault(id, 0.0D);
            double newValue = after.getOrDefault(id, 0.0D);
            double delta = newValue - oldValue;
            String change = Math.abs(delta) < 0.000001D
                    ? "변화 없음"
                    : formatChatValue(id, oldValue) + " -> " + formatChatValue(id, newValue)
                    + " (" + formatChatDelta(id, delta) + ")";
            player.sendMessage(Component.text(promotions.getOptionDisplayName(id) + ": " + change, NamedTextColor.GRAY));
        });
    }

    private String equipmentLabel(ItemStack item) {
        String id = items.getItemId(item).orElse(item.getType().name());
        String stage = promotions.getStage(item);
        return stage.isBlank() ? id : id + " [" + stage + "]";
    }

    private String formatChatValue(String optionId, double value) {
        boolean percent = "PERCENT".equalsIgnoreCase(promotions.getOptionUnit(optionId))
                || "PERCENT_POINT".equalsIgnoreCase(promotions.getOptionUnit(optionId));
        return String.format(Locale.ROOT, "%.2f", percent ? value * 100.0D : value) + (percent ? "%" : "");
    }

    private String formatChatDelta(String optionId, double value) {
        boolean percent = "PERCENT".equalsIgnoreCase(promotions.getOptionUnit(optionId))
                || "PERCENT_POINT".equalsIgnoreCase(promotions.getOptionUnit(optionId));
        return String.format(Locale.ROOT, "%+.2f", percent ? value * 100.0D : value) + (percent ? "%p" : "");
    }

    private boolean processing(Player player, EquipmentSupportMenuHolder holder) {
        return processing(player, holder, ClickType.LEFT);
    }

    private boolean processing(Player player, EquipmentSupportMenuHolder holder, ClickType click) {
        long tick = plugin.getServer().getCurrentTick();
        RerollClickKey key = new RerollClickKey(player.getUniqueId(), holder.sourceId(), click, tick);
        if (holder.view() == EquipmentSupportMenuHolder.View.REROLL_CONFIRM && rerollClicks.putIfAbsent(key, tick) != null) return false;
        if (sessions.putIfAbsent(player.getUniqueId(), holder.sessionId()) == null) return true;
        if (holder.view() == EquipmentSupportMenuHolder.View.REROLL_CONFIRM) rerollClicks.remove(key, tick);
        return false;
    }

    private void openFromView(Player player, EquipmentSupportMenuHolder holder) {
        switch (holder.view()) {
            case EXTRACTION_ENCHANTS, EXTRACTION_CONFIRM -> openExtraction(player);
            case REROLL_OPTIONS, REROLL_CONFIRM -> openReroll(player);
            case FUTURE_DETAIL -> openFuture(player);
            default -> player.closeInventory();
        }
    }

    private List<SourceRef> sources(Player player, boolean extraction, boolean reroll) {
        List<SourceRef> result = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!tiers.isSupported(item)) continue;
            UUID id = instances.ensure(item);
            if (id.equals(new UUID(0L, 0L))) continue;
            boolean matches = extraction ? !enchants.getAppliedEnchants(item).isEmpty() : !promotions.getOptionIds(item).isEmpty();
            if (matches && allowed(item, extraction ? "extraction" : "promotion-option-reroll")) result.add(new SourceRef(slot, id, item));
        }
        return result;
    }

    private SourceRef findSource(Player player, UUID id) {
        if (id == null) return null;
        ItemStack[] contents = player.getInventory().getContents();
        SourceRef found = null;
        for (int slot = 0; slot < contents.length; slot++) {
            if (!instances.is(contents[slot], id)) continue;
            if (found != null) return null;
            found = new SourceRef(slot, id, contents[slot]);
        }
        return found;
    }

    private boolean allowed(ItemStack item, String path) {
        if ("extraction".equals(path) && !config.getEquipmentSupportBoolean("extraction.enabled", true)) return false;
        if ("promotion-option-reroll".equals(path) && !config.getEquipmentSupportBoolean("promotion-option-reroll.enabled", true)) return false;
        if (growthPolicy.special(item) != null && !config.getEquipmentSupportBoolean(path + ".allow-special-equipment", true)) return false;
        if (growthPolicy.isEndgame(item) && !config.getEquipmentSupportBoolean(path + ".allow-endgame-equipment", true)) return false;
        return true;
    }

    private int ticketAmount(String path) { return Math.max(1, config.getEquipmentSupportInt(path + ".ticket-amount", 1)); }
    private long currencyCost(String path) { return Math.max(0L, config.getEquipmentSupportLong(path + ".currency-cost", 0L)); }
    private String ticketId(String path) { return config.getEquipmentSupportString(path + ".ticket-item-id", ""); }

    private boolean hasTickets(Player player, String path) {
        return ticketCount(player, path) >= ticketAmount(path);
    }

    private int ticketCount(Player player, String path) {
        int count = 0; String id = ticketId(path);
        for (ItemStack item : player.getInventory().getContents()) if (items.isItem(item, id)) count += item.getAmount();
        return count;
    }

    private TicketPlan removeTickets(Player player, String path) {
        int needed = ticketAmount(path); String id = ticketId(path); List<SlotBackup> backups = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getSize() && needed > 0; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!items.isItem(item, id)) continue;
            backups.add(new SlotBackup(slot, item.clone()));
            int remove = Math.min(needed, item.getAmount()); item.setAmount(item.getAmount() - remove); needed -= remove;
            player.getInventory().setItem(slot, item.getAmount() <= 0 ? null : item);
        }
        if (needed != 0) {
            for (SlotBackup backup : backups) player.getInventory().setItem(backup.slot(), backup.item());
            return null;
        }
        return new TicketPlan(player, backups);
    }

    private boolean canFit(Player player, ItemStack result) {
        int amount = result.getAmount();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(result)) amount -= Math.max(0, item.getMaxStackSize() - item.getAmount());
            else if (item == null) amount -= result.getMaxStackSize();
            if (amount <= 0) return true;
        }
        return amount <= 0;
    }

    private ItemStack display(ItemStack source, String title, String action) {
        ItemStack clone = source.clone(); ItemMeta meta = clone.getItemMeta();
        if (meta != null) { List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore()); lore.add(Component.text(title, NamedTextColor.YELLOW)); lore.add(Component.text(action, NamedTextColor.GRAY)); meta.lore(lore); clone.setItemMeta(meta); }
        return clone;
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.displayName(Component.text(name, NamedTextColor.YELLOW));
        meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList()); item.setItemMeta(meta); return item;
    }

    private void footer(Inventory inv, boolean pages) {
        inv.setItem(BACK, icon(Material.ARROW, "뒤로", List.of()));
        inv.setItem(49, icon(Material.GOLD_NUGGET, "장비 보조", List.of("추출권과 초기화권은 보조 상점에서 구매합니다.")));
    }

    private boolean unsafe(ClickType click) {
        return click == ClickType.NUMBER_KEY || click == ClickType.DOUBLE_CLICK || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP || click == ClickType.SWAP_OFFHAND || click == ClickType.CREATIVE;
    }

    public record SourceRef(int slot, UUID id, ItemStack item) { }
    private record RerollClickKey(UUID playerId, UUID equipmentId, ClickType click, long tick) { }
    private record SlotBackup(int slot, ItemStack item) { }
    private static final class TicketPlan {
        private final Player player; private final List<SlotBackup> backups;
        private TicketPlan(Player player, List<SlotBackup> backups) { this.player = player; this.backups = backups; }
        private void restore(Player ignored) { for (SlotBackup backup : backups) player.getInventory().setItem(backup.slot(), backup.item()); }
    }
}
