package com.hyunseo.hyunseorpg.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Single player-facing crafting GUI and editor for the canonical layout. */
public final class CraftingGuiService implements Listener {
    public static final int PAGE_SIZE = CraftingLayoutRegistry.CONTENT_SLOTS_PER_PAGE;
    public static final String MAIN_TITLE = "제작";

    private final JavaPlugin plugin;
    private final CraftingRecipeRegistry recipes;
    private final CraftingLayoutRegistry layouts;
    private final CraftingTransactionService transactions;
    private final CraftingRecipeRenderer renderer;
    private final Map<UUID, CraftingAdminSession> adminSessions = new ConcurrentHashMap<>();

    public CraftingGuiService(JavaPlugin plugin,
                              com.hyunseo.hyunseorpg.item.RPGItemService itemService,
                              CraftingRecipeRegistry recipes,
                              CraftingLayoutRegistry layouts,
                              CraftingTransactionService transactions) {
        this.plugin = plugin;
        this.recipes = recipes;
        this.layouts = layouts;
        this.transactions = transactions;
        this.renderer = new CraftingRecipeRenderer(plugin, itemService, transactions);
    }

    public void open(Player player) {
        openMain(player);
    }

    public void openMain(Player player) {
        CraftingMenuHolder holder = new CraftingMenuHolder("category", 0, false, null);
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text(MAIN_TITLE));
        holder.setInventory(inventory);
        for (Map.Entry<Integer, String> entry : mainCategories().entrySet()) {
            String category = entry.getValue();
            inventory.setItem(entry.getKey(), menuItem(layouts.icon(category), layouts.displayName(category),
                    List.of("카테고리: " + layouts.displayName(category))));
        }
        player.openInventory(inventory);
    }

    public void openCategory(Player player, String rawCategory, int page) {
        String category = normalize(rawCategory);
        if (category.equals("cooking") || !layouts.categoryIds().contains(category)) {
            openMain(player);
            return;
        }
        int pageCount = layouts.pageCount(category);
        int safePage = Math.max(0, Math.min(page, pageCount - 1));
        CraftingMenuHolder holder = new CraftingMenuHolder(category, safePage, false, null);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text(layouts.displayName(category) + " 제작"));
        holder.setInventory(inventory);
        Set<String> hiddenFarmingRecipes = category.equals("materials")
                ? java.util.stream.Stream.of("processing", "essence")
                .flatMap(type -> recipes.getAllByFarmingType(type).stream())
                .map(CraftingRecipeData::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet())
                : Set.of();
        for (Map.Entry<Integer, String> entry : layouts.pageEntries(category, safePage).entrySet()) {
            if (hiddenFarmingRecipes.contains(entry.getValue())) continue;
            recipes.get(entry.getValue()).ifPresent(recipe ->
                    inventory.setItem(entry.getKey(), renderer.render(player, recipe)));
        }
        footer(inventory, safePage, pageCount, false);
        player.openInventory(inventory);
    }

    /** Opens farming processing recipes through the canonical crafting renderer and transaction service. */
    public void openProcessing(Player player, Consumer<Player> backOpener) {
        List<String> recipeIds = recipes.getAllByFarmingType("processing").stream()
                .map(CraftingRecipeData::id).toList();
        if (recipeIds.isEmpty()) {
            player.sendMessage(Component.text("현재 이용 가능한 농사 가공 레시피가 없습니다."));
            if (backOpener == null) openMain(player);
            else backOpener.accept(player);
            return;
        }
        openFiltered(player, recipeIds, 0, backOpener, "농사 가공");
    }

    /** Opens the canonical essence recipe set through the same renderer and transaction service. */
    public void openFarmingEssence(Player player, Consumer<Player> backOpener) {
        List<String> recipeIds = recipes.getAllByFarmingType("essence").stream()
                .map(CraftingRecipeData::id).toList();
        if (recipeIds.isEmpty()) {
            player.sendMessage(Component.text("현재 이용 가능한 풍요의 정수 레시피가 없습니다."));
            if (backOpener == null) openMain(player);
            else backOpener.accept(player);
            return;
        }
        openFiltered(player, recipeIds, 0, backOpener, "농사 - 풍요의 정수");
    }

    /** Opens alchemy essence recipes through the canonical crafting renderer. */
    public void openAlchemyEssences(Player player, Consumer<Player> backOpener) {
        openAlchemyType(player, "alchemy_essence", "양조 - 정수 제작", backOpener);
    }

    /** Opens active alchemy potion recipes through the canonical crafting transaction path. */
    public void openAlchemyPotions(Player player, Consumer<Player> backOpener) {
        openAlchemyType(player, "alchemy_potion", "양조 - 물약 제작", backOpener);
    }

    /** Opens active catalyst recipes through the canonical crafting transaction path. */
    public void openAlchemyCatalysts(Player player, Consumer<Player> backOpener) {
        openAlchemyType(player, "alchemy_catalyst", "양조 - 촉매 적용", backOpener);
    }

    private void openAlchemyType(Player player, String farmingType, String title, Consumer<Player> backOpener) {
        List<String> recipeIds = recipes.getAllByFarmingType(farmingType).stream()
                .map(CraftingRecipeData::id).toList();
        if (recipeIds.isEmpty()) {
            player.sendMessage(Component.text(title + " 레시피가 현재 활성화되어 있지 않습니다."));
            if (backOpener == null) openMain(player); else backOpener.accept(player);
            return;
        }
        openFiltered(player, recipeIds, 0, backOpener, title);
    }

    private void openFiltered(Player player, List<String> recipeIds, int page, Consumer<Player> backOpener) {
        boolean essence = !recipeIds.isEmpty() && recipeIds.stream().allMatch(id ->
                recipes.get(id).map(recipe -> "essence".equalsIgnoreCase(recipe.farmingType())).orElse(false));
        openFiltered(player, recipeIds, page, backOpener, essence ? "농사 - 풍요의 정수" : "농사 가공");
    }

    private void openFiltered(Player player, List<String> recipeIds, int page,
                              Consumer<Player> backOpener, String title) {
        int pageCount = Math.max(1, (recipeIds.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, pageCount - 1));
        CraftingMenuHolder holder = new CraftingMenuHolder(
                "farming_processing", safePage, false, null, recipeIds, backOpener);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text(title));
        holder.setInventory(inventory);
        int from = safePage * PAGE_SIZE;
        for (int index = from; index < Math.min(recipeIds.size(), from + PAGE_SIZE); index++) {
            int slot = index - from;
            recipes.get(recipeIds.get(index)).ifPresent(recipe ->
                    inventory.setItem(slot, renderer.render(player, recipe)));
        }
        footer(inventory, safePage, pageCount, false);
        player.openInventory(inventory);
    }

    public boolean openAdmin(Player player, String rawCategory) {
        String category = normalize(rawCategory);
        if (category.equals("cooking") || !layouts.categoryIds().contains(category)) return false;
        CraftingAdminSession session = new CraftingAdminSession(category, layouts.positions(category));
        adminSessions.put(player.getUniqueId(), session);
        openAdminPage(player, session);
        return true;
    }

    private void openAdminPage(Player player, CraftingAdminSession session) {
        CraftingMenuHolder holder = new CraftingMenuHolder(session.type(), session.page(), true, session);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text(layouts.displayName(session.type()) + " 배치 편집"));
        holder.setInventory(inventory);
        int from = session.page() * PAGE_SIZE;
        for (Map.Entry<String, Integer> entry : session.positions().entrySet()) {
            if (entry.getValue() < from || entry.getValue() >= from + PAGE_SIZE) continue;
            recipes.get(entry.getKey()).ifPresent(recipe ->
                    inventory.setItem(entry.getValue() - from, renderer.render(player, recipe)));
        }
        footer(inventory, session.page(), session.editorPageCount(), true);
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CraftingMenuHolder holder)
                || !(event.getWhoClicked() instanceof Player player)) return;
        int topSize = event.getView().getTopInventory().getSize();
        int slot = event.getRawSlot();
        if (holder.admin()) {
            if (slot < 0 || slot >= topSize) {
                if (event.isShiftClick()) event.setCancelled(true);
                return;
            }
            if (slot < PAGE_SIZE) return;
            event.setCancelled(true);
            handleAdminFooter(player, holder, slot);
            return;
        }
        event.setCancelled(true);
        if (holder.type().equals("category")) {
            String category = mainCategories().get(slot);
            if (category != null) openCategory(player, category, 0);
            return;
        }
        if (holder.filtered()) {
            if (slot == 45) openFiltered(player, holder.visibleRecipeIds(), holder.page() - 1, holder.backOpener());
            else if (slot == 49) openBack(player, holder);
            else if (slot == 53) openFiltered(player, holder.visibleRecipeIds(), holder.page() + 1, holder.backOpener());
            else if (slot >= 0 && slot < PAGE_SIZE && event.isLeftClick()) {
                int index = holder.page() * PAGE_SIZE + slot;
                if (index < holder.visibleRecipeIds().size()) {
                    craft(player, holder.visibleRecipeIds().get(index), event.isShiftClick());
                }
            }
            return;
        }
        String category = normalize(holder.type());
        if (slot == 45) openCategory(player, category, holder.page() - 1);
        else if (slot == 49) openBack(player, holder);
        else if (slot == 53) openCategory(player, category, holder.page() + 1);
        else if (slot >= 0 && slot < PAGE_SIZE) {
            // The persisted layout is authoritative for a visible slot. The PDC
            // marker remains a compatibility fallback for older open inventories.
            String recipeId = layouts.recipeAt(holder.type(), holder.page(), slot)
                    .orElseGet(() -> resolveRecipeId(event.getCurrentItem()));
            if (recipeId != null && event.isLeftClick()) craft(player, recipeId, event.isShiftClick());
        }
    }

    private void craft(Player player, String recipeId, boolean maximum) {
        CraftingTransactionService.Result result = transactions.craft(player, recipeId, maximum);
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof CraftingMenuHolder holder
                && !holder.type().equals("category")) {
            if (holder.filtered()) openFiltered(player, holder.visibleRecipeIds(), holder.page(), holder.backOpener());
            else openCategory(player, holder.type(), holder.page());
        }
        player.sendMessage(Component.text(resultMessage(result), result.success()
                ? net.kyori.adventure.text.format.NamedTextColor.GREEN
                : net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private String resultMessage(CraftingTransactionService.Result result) {
        if (result.success()) return "제작 완료: " + result.crafted() + "개";
        return switch (result.status()) {
            case MISSING_INGREDIENTS -> "제작 재료가 부족합니다.";
            case INSUFFICIENT_ABUNDANCE_POINTS -> "풍요 포인트가 부족합니다.";
            case NO_SPACE -> "결과물을 받을 공간이 부족합니다.";
            case BUSY -> "제작이 이미 처리 중입니다.";
            case UNKNOWN_RECIPE -> "알 수 없는 제작 레시피입니다.";
            default -> "제작을 완료하지 못했습니다.";
        };
    }

    private void handleAdminFooter(Player player, CraftingMenuHolder holder, int slot) {
        CraftingAdminSession session = holder.session();
        session.capture(holder.getInventory(), renderer::recipeId);
        if (slot == 45 && session.page() > 0) {
            session.setPage(session.page() - 1);
            reopenAdmin(player, session);
        } else if (slot == 53) {
            session.setPage(session.page() + 1);
            reopenAdmin(player, session);
        } else if (slot == 49) {
            if (layouts.save(recipes, session.type(), session.positions())) {
                player.sendMessage(Component.text("제작 레이아웃을 저장했습니다."));
                adminSessions.remove(player.getUniqueId());
                player.closeInventory();
            } else {
                player.sendMessage(Component.text("레이아웃 검증에 실패했습니다."));
            }
        }
    }

    private void reopenAdmin(Player player, CraftingAdminSession session) {
        session.setNavigating(true);
        openAdminPage(player, session);
        Bukkit.getScheduler().runTask(plugin, () -> session.setNavigating(false));
    }

    private void openBack(Player player, CraftingMenuHolder holder) {
        if (holder.backOpener() != null) holder.backOpener().accept(player);
        else openMain(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CraftingMenuHolder holder)) return;
        if (!holder.admin()) {
            event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot >= PAGE_SIZE && slot < topSize)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || !(event.getInventory().getHolder() instanceof CraftingMenuHolder holder)
                || !holder.admin()) return;
        if (!holder.navigating()) adminSessions.remove(player.getUniqueId());
    }

    private String resolveRecipeId(ItemStack item) {
        String id = renderer.recipeId(item);
        return id != null && recipes.get(id).isPresent() ? id : null;
    }

    private void footer(Inventory inventory, int page, int count, boolean admin) {
        inventory.setItem(45, menuItem(page > 0 ? Material.ARROW : Material.BARRIER, "이전", List.of()));
        inventory.setItem(49, menuItem(admin ? Material.WRITABLE_BOOK : Material.BARRIER,
                admin ? "레이아웃 저장" : "뒤로", List.of()));
        inventory.setItem(53, menuItem(page < count - 1 || admin ? Material.ARROW : Material.BARRIER, "다음", List.of()));
    }

    private Map<Integer, String> mainCategories() {
        Map<Integer, String> result = new LinkedHashMap<>();
        List<String> categories = layouts.categoryIds().stream()
                .filter(category -> !category.equalsIgnoreCase("cooking"))
                .sorted(Comparator.naturalOrder()).toList();
        int fallback = 10;
        for (String category : categories) {
            int slot = layouts.menuSlot(category, fallback);
            while (result.containsKey(slot) || slot < 0 || slot >= 27) slot++;
            if (slot >= 27) continue;
            result.put(slot, category);
            fallback = slot + 1;
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private ItemStack menuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
        return item;
    }
}
