package com.hyunseo.hyunseorpg.shop;

import com.hyunseo.hyunseorpg.economy.CoinService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Builds shop views; transaction rules remain in ShopService. */
public final class ShopGuiService {
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int COINS_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final JavaPlugin plugin;
    private final ShopRegistry shopRegistry;
    private final ShopService shopService;
    private final CoinService coinService;
    private final Map<UUID, ShopAdminSession> adminSessions = new ConcurrentHashMap<>();

    public ShopGuiService(JavaPlugin plugin, ShopRegistry shopRegistry, ShopService shopService, CoinService coinService) {
        this.plugin = plugin;
        this.shopRegistry = shopRegistry;
        this.shopService = shopService;
        this.coinService = coinService;
    }

    public boolean openShop(Player player, String shopId) {
        return shopRegistry.get(shopId).map(shop -> {
            openShopDialog(player, shop);
            return true;
        }).orElse(false);
    }

    private void openShopDialog(Player player, ShopData shop) {
        List<ShopItemData> products = shop.items();
        List<DialogBody> bodies = new ArrayList<>();
        List<DialogInput> inputs = new ArrayList<>();
        for (int index = 0; index < products.size(); index++) {
            ShopItemData product = products.get(index);
            int buyMaximum = shopService.availableBuyQuantity(player, product);
            int sellMaximum = shopService.availableSellQuantity(player, product);
            int maximum = Math.max(buyMaximum, sellMaximum);
            ItemStack shown = product.template();
            shown.setAmount(Math.min(product.amount(), Math.max(1, shown.getMaxStackSize())));
            Component availability = Component.text("구매 최대 " + buyMaximum + " / 판매 최대 " + sellMaximum,
                    NamedTextColor.GRAY);
            bodies.add(DialogBody.item(shown).description(DialogBody.plainMessage(availability)).build());
            inputs.add(DialogInput.numberRange(ShopDialogSelection.inputKey(index),
                            Component.text(product.productId() + " 수량 (최대 " + maximum + ")"), 0.0F, maximum)
                    .initial(0.0F).step(1.0F).build());
        }

        ClickCallback.Options oneUse = ClickCallback.Options.builder()
                .uses(1).lifetime(Duration.ofMinutes(5)).build();
        ActionButton buy = ActionButton.builder(Component.text("구매", NamedTextColor.GOLD))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player actor && actor.getUniqueId().equals(player.getUniqueId())) {
                        transactDialog(actor, shop, products, response::getFloat, true);
                    }
                }, oneUse)).build();
        ActionButton sell = ActionButton.builder(Component.text("판매", NamedTextColor.GREEN))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player actor && actor.getUniqueId().equals(player.getUniqueId())) {
                        transactDialog(actor, shop, products, response::getFloat, false);
                    }
                }, oneUse)).build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(shop.title()))
                        .externalTitle(Component.text(shop.title()))
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(bodies).inputs(inputs).build())
                .type(DialogType.multiAction(List.of(buy, sell)).columns(2).build()));
        player.showDialog(dialog);
    }

    private void transactDialog(Player player, ShopData displayedShop, List<ShopItemData> displayedProducts,
                                java.util.function.Function<String, Float> input, boolean buy) {
        ShopData currentShop = shopRegistry.get(displayedShop.shopId()).orElse(null);
        if (currentShop == null || !currentShop.items().stream().map(ShopItemData::productId).toList()
                .equals(displayedProducts.stream().map(ShopItemData::productId).toList())) {
            player.sendMessage(Component.text("상점 정보가 변경되었습니다. 상점을 다시 열어 주세요.", NamedTextColor.RED));
            return;
        }
        List<ShopItemData> currentProducts = currentShop.items();
        Map<Integer, Integer> selected = ShopDialogSelection.read(
                currentProducts.stream().map(ShopItemData::amount).toList(), input, index -> buy
                        ? shopService.availableBuyQuantity(player, currentProducts.get(index))
                        : shopService.availableSellQuantity(player, currentProducts.get(index)));
        for (Map.Entry<Integer, Integer> entry : selected.entrySet()) {
            ShopItemData product = currentProducts.get(entry.getKey());
            ShopTransactionResult result = buy
                    ? shopService.buyQuantity(player, product, entry.getValue())
                    : shopService.sellQuantity(player, product, entry.getValue(),
                    player.getInventory().getItemInMainHand());
            sendTransactionMessage(player, result, buy);
        }
        openShopDialog(player, currentShop);
    }

    public List<ShopData> getShops() {
        return shopRegistry.getAll();
    }

    public void openShopPage(Player player, ShopData shop, int page) {
        int safePage = Math.max(0, Math.min(page, shop.pageCount() - 1));
        ShopInventoryHolder holder = new ShopInventoryHolder(shop, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text(shop.title()));
        holder.setInventory(inventory);
        List<ShopItemData> products = shop.page(safePage);
        for (ShopItemData product : products) {
            inventory.setItem(Math.floorMod(product.order(), PAGE_SIZE), displayProduct(player, product));
        }
        fillFooter(inventory, safePage, shop.pageCount(), coinService.getCoins(player), false);
        player.openInventory(inventory);
    }

    public boolean openAdmin(Player player, String shopId) {
        ShopData shop = shopRegistry.get(shopId).orElse(null);
        if (shop == null) return false;
        ShopAdminSession session = new ShopAdminSession(shop);
        adminSessions.put(player.getUniqueId(), session);
        openAdminPage(player, session);
        return true;
    }

    public void openAdminPage(Player player, ShopAdminSession session) {
        ShopAdminInventoryHolder holder = new ShopAdminInventoryHolder(session);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("상점 편집: " + session.original().title()));
        holder.setInventory(inventory);
        List<ShopItemData> products = session.pageItems();
        for (ShopItemData product : products) {
            ItemStack display = product.template();
            display.setAmount(Math.min(product.amount(), Math.max(1, display.getMaxStackSize())));
            inventory.setItem(Math.floorMod(product.order(), PAGE_SIZE), display);
        }
        int visiblePageCount = Math.max(session.pageCount() + 1, session.page() + 1);
        fillFooter(inventory, session.page(), visiblePageCount, 0L, true);
        player.openInventory(inventory);
    }

    public void navigateShop(Player player, ShopInventoryHolder holder, int direction) {
        openShopPage(player, holder.shop(), holder.page() + direction);
    }

    public void navigateAdmin(Player player, ShopAdminInventoryHolder holder, int direction) {
        ShopAdminSession session = holder.session();
        session.capture(holder.getInventory());
        int requested = session.page() + direction;
        if (requested < 0 || requested > session.pageCount()) return;
        session.setPage(requested);
        session.setNavigating(true);
        openAdminPage(player, session);
        Bukkit.getScheduler().runTask(plugin, () -> session.setNavigating(false));
    }

    public void transact(Player player, ShopInventoryHolder holder, int slot, boolean buy, boolean stack) {
        ShopItemData product = holder.shop().itemAt(holder.page(), slot).orElse(null);
        if (product == null) return;
        ShopTransactionResult result = buy
                ? shopService.buy(player, product, stack)
                : shopService.sell(player, product, stack, player.getInventory().getItemInMainHand());
        sendTransactionMessage(player, result, buy);
        if (result.success()) openShopPage(player, holder.shop(), holder.page());
    }

    public void closeAdmin(Player player, ShopAdminInventoryHolder holder) {
        ShopAdminSession session = holder.session();
        if (session.navigating()) return;
        session.capture(holder.getInventory());
        adminSessions.remove(player.getUniqueId(), session);
        if (shopRegistry.save(session.toShopData())) {
            player.sendMessage(Component.text("상점 편집 내용을 저장했습니다. 가격과 거래 가능 여부는 shops.yml에서 설정하세요.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("상점 저장에 실패했습니다. 기존 shops.yml은 유지됩니다.", NamedTextColor.RED));
        }
    }

    public boolean saveSession(Player player, String shopId) {
        ShopAdminSession session = adminSessions.get(player.getUniqueId());
        if (session == null || !session.original().shopId().equalsIgnoreCase(shopId)) return false;
        return shopRegistry.save(session.toShopData());
    }

    public void reload() {
        shopRegistry.reload();
    }

    private ItemStack displayProduct(Player player, ShopItemData product) {
        ItemStack display = product.template();
        display.setAmount(Math.min(product.amount(), Math.max(1, display.getMaxStackSize())));
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;
        List<Component> lore = new ArrayList<>();
        if (meta.lore() != null) {
            for (Component line : meta.lore()) {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                if (!plain.startsWith("구매 단위:") && !plain.startsWith("구매 가격:")
                        && !plain.startsWith("Shift + 구매:") && !plain.startsWith("판매 단위:")
                        && !plain.startsWith("판매 가격:") && !plain.startsWith("Shift + 판매:")
                        && !plain.startsWith("좌클릭:") && !plain.startsWith("우클릭:")) {
                    lore.add(line);
                }
            }
        }
        lore.add(Component.empty());
        if (!product.currencyItemId().isBlank()) {
            lore.add(Component.text("구매 재화: " + product.currencyItemId(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("구매 단위: " + product.amount() + "개", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("구매 가격: " + priceText(product.purchasable(), product.buyPrice())
                + " / " + product.amount() + "개", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift + 구매: " + bulkPurchaseText(product), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("판매 단위: " + product.amount() + "개", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        long sellPrice = shopService.effectiveSellPrice(product, player.getInventory().getItemInMainHand());
        lore.add(Component.text("판매 가격: " + priceText(product.sellable(), sellPrice)
                + " / " + product.amount() + "개", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift + 판매: " + bulkSaleText(product, player), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private String priceText(boolean enabled, long price) {
        return enabled && price > 0L ? formatPrice(price) + " 코인" : "불가능";
    }

    private String bulkPurchaseText(ShopItemData product) {
        if (!product.purchasable()) return "구매 불가";
        long price = shopService.bulkPrice(product, true);
        int amount = shopService.bulkDeliveryAmount(product);
        return price < 0L || amount < 1 ? "계산 불가" : amount + "개 / " + formatPrice(price) + " 코인";
    }

    private String bulkSaleText(ShopItemData product, Player player) {
        if (!product.sellable()) return "판매 불가";
        long price = shopService.bulkPrice(product, false, player.getInventory().getItemInMainHand());
        return price < 0L ? "계산 불가" : "가능한 완전 묶음 / " + formatPrice(price) + " 코인씩";
    }

    private String formatPrice(long price) {
        return String.format(Locale.US, "%,d", price);
    }

    private void fillFooter(Inventory inventory, int page, int pageCount, long coins, boolean admin) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = PREVIOUS_SLOT; slot <= NEXT_SLOT; slot++) inventory.setItem(slot, filler);
        inventory.setItem(PREVIOUS_SLOT, page > 0
                ? item(Material.ARROW, "이전 페이지", List.of("현재 " + (page + 1) + "/" + pageCount + " 페이지"))
                : item(Material.BARRIER, "이전 페이지 없음", List.of()));
        inventory.setItem(NEXT_SLOT, page < pageCount - 1
                ? item(Material.ARROW, "다음 페이지", List.of("현재 " + (page + 1) + "/" + pageCount + " 페이지"))
                : item(Material.BARRIER, "다음 페이지 없음", List.of()));
        inventory.setItem(COINS_SLOT, admin
                ? item(Material.WRITABLE_BOOK, "상품 편집", List.of("아이템을 직접 배치하거나 제거하세요.", "닫으면 상품 목록만 저장됩니다."))
                : item(Material.GOLD_INGOT, "보유 코인: " + coins, List.of("가격은 표시된 묶음 기준입니다.")));
    }

    private ItemStack item(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(loreLines.stream().map(line -> Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void sendTransactionMessage(Player player, ShopTransactionResult result, boolean buy) {
        if (result.success()) {
            player.sendMessage(Component.text((buy ? "구매" : "판매") + " 완료: " + result.amount() + "개, " + result.coins() + " 코인", NamedTextColor.GREEN));
            return;
        }
        String message = switch (result.reason()) {
            case PURCHASE_DISABLED -> "이 상품은 구매할 수 없습니다.";
            case FARMING_PROFILE_NOT_READY -> "농사 데이터가 아직 준비되지 않았습니다.";
            case FARMING_CROP_LOCKED -> "아직 해금하지 않은 작물의 씨앗입니다.";
            case SALE_DISABLED -> "이 상품은 판매할 수 없습니다.";
            case INVALID_PRICE -> "이 상품의 가격 설정이 올바르지 않습니다.";
            case INSUFFICIENT_COINS -> "코인이 부족합니다.";
            case INVENTORY_FULL -> "인벤토리에 충분한 공간이 없습니다.";
            case NO_MATCHING_ITEMS -> "판매할 수 있는 동일 아이템이 없습니다.";
            case PRICE_OVERFLOW -> "거래 금액이 허용 범위를 초과했습니다.";
            default -> "거래 처리 중 오류가 발생했습니다.";
        };
        player.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
