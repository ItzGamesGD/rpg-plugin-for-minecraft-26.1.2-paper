package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.crafting.CraftingGuiService;
import com.hyunseo.hyunseorpg.ui.KoreanDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Navigation-only farming entry point. Existing services own every actual operation. */
public final class FarmingHubGuiService implements Listener {
    private static final int PROCESSING_SLOT = 10;
    private static final int COOKING_SLOT = 12;
    private static final int DELIVERY_SLOT = 14;
    private static final int INFO_SLOT = 16;
    private static final int ESSENCE_SLOT = 18;
    private static final int BACK_SLOT = 22;
    private static final int CLOSE_SLOT = 24;

    private final CraftingGuiService crafting;
    private final DeliveryGuiService deliveries;
    private final FarmingProfileService profiles;
    private Consumer<Player> mainMenuOpener = Player::closeInventory;

    public FarmingHubGuiService(CraftingGuiService crafting,
                                DeliveryGuiService deliveries, FarmingProfileService profiles) {
        this.crafting = crafting;
        this.deliveries = deliveries;
        this.profiles = profiles;
    }

    public void setMainMenuOpener(Consumer<Player> opener) {
        this.mainMenuOpener = opener == null ? Player::closeInventory : opener;
    }

    public void open(Player player) {
        openView(player, FarmingHubMenuHolder.View.HUB);
    }

    private void openView(Player player, FarmingHubMenuHolder.View view) {
        if (player == null || !player.isOnline()) return;
        FarmingHubMenuHolder holder = new FarmingHubMenuHolder(player.getUniqueId(), view);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text(view == FarmingHubMenuHolder.View.HUB ? "\uB18D\uC0AC" : "\uB18D\uC0AC \uC815\uBCF4"));
        holder.setInventory(inventory);
        fill(inventory);
        if (view == FarmingHubMenuHolder.View.HUB) renderHub(inventory);
        else if (view == FarmingHubMenuHolder.View.DELIVERY) renderDelivery(inventory);
        else renderInfo(inventory, player);
        player.openInventory(inventory);
    }

    private void renderHub(Inventory inventory) {
        inventory.setItem(PROCESSING_SLOT, icon(Material.CAULDRON, "\uB18D\uC0AC \uAC00\uACF5", List.of(
                "\uAE30\uC874 \uC81C\uC791 Registry\uC758 \uAC00\uACF5 \uB808\uC2DC\uD53C",
                "\uD074\uB9AD\uD558\uC5EC \uAC00\uACF5 \uBA54\uB274 \uC5F4\uAE30")));
        inventory.setItem(COOKING_SLOT, icon(Material.BARRIER, "\uC694\uB9AC (\uBE44\uD65C\uC131)", List.of(
                "\uD604\uC7AC \uC0AC\uC6A9\uD558\uC9C0 \uC54A\uB294 \uAE30\uB2A5\uC785\uB2C8\uB2E4.")));
        inventory.setItem(DELIVERY_SLOT, icon(Material.CHEST, "\uBC30\uB2EC \uC758\uB8B0", List.of(
                "\uB18D\uBD80\u00B7\uC5F0\uAE08\uC220\uC0AC \uBC30\uB2EC \uC758\uB8B0",
                "\uD074\uB9AD\uD558\uC5EC \uC81C\uACF5\uC790 \uC120\uD0DD")));
        inventory.setItem(INFO_SLOT, icon(Material.BOOK, "\uB18D\uC0AC \uC815\uBCF4", List.of(
                "\uD604\uC7AC \uB18D\uC0AC \uB2E8\uACC4\uC640 \uD574\uAE08 \uC791\uBB3C",
                "\uC720\uD6A8 \uC218\uD655\uB7C9\uACFC \uBC30\uB2EC \uC644\uB8CC \uD69F\uC218")));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "\uBA54\uC778 \uBA54\uB274", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "\uB2EB\uAE30", List.of()));
        inventory.setItem(ESSENCE_SLOT, icon(Material.AMETHYST_SHARD, "\uD48D\uC694\uC758 \uC815\uC218", List.of(
                "\uD48D\uC694 \uD3EC\uC778\uD2B8 \uC804\uC6A9 \uC81C\uC791",
                "\uC804\uBB38 \uB18D\uC0AC \uB2E8\uACC4 \uD544\uC694",
                "\uD074\uB9AD\uD558\uC5EC \uC804\uC6A9 \uC81C\uC791 \uBA54\uB274 \uC5F4\uAE30")));
    }

    private void renderDelivery(Inventory inventory) {
        inventory.setItem(11, icon(Material.WHEAT, "\uB18D\uBD80 \uC758\uB8B0", List.of(
                "\uAE30\uC874 DeliveryGuiService \uC0AC\uC6A9", "\uD074\uB9AD\uD558\uC5EC \uC758\uB8B0 \uC5F4\uAE30")));
        inventory.setItem(15, icon(Material.BREWING_STAND, "\uC5F0\uAE08\uC220\uC0AC \uC758\uB8B0", List.of(
                "\uAE30\uC874 DeliveryGuiService \uC0AC\uC6A9", "\uD074\uB9AD\uD558\uC5EC \uC758\uB8B0 \uC5F4\uAE30")));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "\uB18D\uC0AC \uBA54\uB274", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "\uB2EB\uAE30", List.of()));
    }

    private void renderInfo(Inventory inventory, Player player) {
        FarmingProfile profile = profiles.getFarmingProfile(player);
        List<String> lore = new ArrayList<>();
        lore.add("\uD48D\uC694 \uD3EC\uC778\uD2B8: " + profile.abundancePoints());
        lore.add("\uC81C\uACF5\uC790\uBCC4 \uD638\uAC10\uB3C4: " + profile.favor());
        lore.add("\uB18D\uC0AC \uB2E8\uACC4: " + profile.stage().displayName());
        lore.add("\uD574\uAE08 \uC791\uBB3C: " + profile.unlockedCrops().stream()
                .sorted().map(KoreanDisplay::crop).toList());
        lore.add("\uC720\uD6A8 \uC218\uD655\uB7C9: " + profile.totalValidHarvests());
        int deliveriesCompleted = profile.deliveryCompletedCounts().values().stream()
                .mapToInt(Integer::intValue).sum();
        lore.add("\uBC30\uB2EC \uC644\uB8CC \uD69F\uC218: " + deliveriesCompleted);
        inventory.setItem(13, icon(Material.BOOK, "\uB18D\uC0AC \uC815\uBCF4", lore));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "\uB18D\uC0AC \uBA54\uB274", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "\uB2EB\uAE30", List.of()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof FarmingHubMenuHolder holder)) return;
        event.setCancelled(true);
        if (!holder.playerId().equals(player.getUniqueId()) || event.getClickedInventory() != top) return;
        if (!event.isLeftClick()) return;

        switch (holder.view()) {
            case HUB -> handleHubClick(player, event.getRawSlot());
            case DELIVERY -> handleDeliveryClick(player, event.getRawSlot());
            case INFO -> {
                if (event.getRawSlot() == BACK_SLOT) open(player);
                else if (event.getRawSlot() == CLOSE_SLOT) player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FarmingHubMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleHubClick(Player player, int slot) {
        switch (slot) {
            case PROCESSING_SLOT -> crafting.openProcessing(player, this::open);
            case COOKING_SLOT -> { }
            case DELIVERY_SLOT -> openView(player, FarmingHubMenuHolder.View.DELIVERY);
            case INFO_SLOT -> openView(player, FarmingHubMenuHolder.View.INFO);
            case ESSENCE_SLOT -> crafting.openFarmingEssence(player, this::open);
            case BACK_SLOT -> mainMenuOpener.accept(player);
            case CLOSE_SLOT -> player.closeInventory();
            default -> { }
        }
    }

    private void handleDeliveryClick(Player player, int slot) {
        switch (slot) {
            case 11 -> deliveries.open(player, DeliveryProvider.FARMER);
            case 15 -> deliveries.open(player, DeliveryProvider.ALCHEMIST);
            case BACK_SLOT -> open(player);
            case CLOSE_SLOT -> player.closeInventory();
            default -> { }
        }
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
