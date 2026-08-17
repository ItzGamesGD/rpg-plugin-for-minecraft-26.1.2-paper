package com.hyunseo.hyunseorpg.enhancement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class AnvilGrowthListener implements Listener {
    private static final Set<Material> ANVILS = Set.of(Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
    private final JavaPlugin plugin;
    private final EquipmentGrowthGuiService guiService;

    public AnvilGrowthListener(JavaPlugin plugin, EquipmentGrowthGuiService guiService) {
        this.plugin = plugin;
        this.guiService = guiService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAnvilInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || !ANVILS.contains(event.getClickedBlock().getType()) || event.getPlayer().hasPermission("hyunseorpg.admin.vanilla-anvil")) return;
        event.setCancelled(true);
        event.setUseInteractedBlock(Result.DENY);
        event.setUseItemInHand(Result.DENY);
        Bukkit.getScheduler().runTask(plugin, () -> { if (event.getPlayer().isOnline()) guiService.openMain(event.getPlayer()); });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof EquipmentGrowthMenuHolder) {
            event.setCancelled(true);
            if (event.getRawSlot() == 15) {
                guiService.openEnchant(player);
                return;
            }
            if (event.getClickedInventory() != top) return;
            if (event.getRawSlot() == 11) guiService.openEnhancement(player);
            else if (event.getRawSlot() == 13) guiService.openPromotion(player);
            else if (event.getRawSlot() == 17) guiService.openRepair(player);
            else if (event.getRawSlot() == 15) player.sendMessage(Component.text("인챈트는 다음 단계에서 구현됩니다.", NamedTextColor.YELLOW));
            else if (event.getRawSlot() == 20 && guiService.getSupportService() != null) guiService.getSupportService().openReroll(player);
            else if (event.getRawSlot() == 24 && guiService.getSupportService() != null) guiService.getSupportService().openFuture(player);
            else if (event.getRawSlot() == EquipmentGrowthGuiService.CLOSE_SLOT) player.closeInventory();
            return;
        }
        boolean enhancement = top.getHolder() instanceof EnhancementInventoryHolder;
        boolean promotion = top.getHolder() instanceof PromotionInventoryHolder;
        boolean enchant = top.getHolder() instanceof EnchantInventoryHolder;
        boolean repair = top.getHolder() instanceof RepairInventoryHolder;
        if (!enhancement && !promotion && !enchant && !repair) return;
        if (unsafe(event.getClick()) || event.isShiftClick()) { event.setCancelled(true); return; }
        if (event.getClickedInventory() != top) return;
        int slot = event.getRawSlot();
        if (repair && slot == EquipmentGrowthGuiService.STONE_SLOT) {
            event.setCancelled(true);
            return;
        }
        if (guiService.isInputSlot(slot)) { guiService.refreshLater(top); return; }
        event.setCancelled(true);
        if (slot == EquipmentGrowthGuiService.EXECUTE_SLOT) {
            if (enhancement) guiService.enhance(player, top);
            else if (promotion) guiService.promote(player, top);
            else if (enchant) guiService.enchant(player, top);
            else guiService.repair(player, top);
        } else if (slot == EquipmentGrowthGuiService.BACK_SLOT) {
            guiService.returnInputs(player, top);
            guiService.openMain(player);
        } else if (slot == EquipmentGrowthGuiService.CLOSE_SLOT) player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnhancementInventoryHolder) && !(top.getHolder() instanceof PromotionInventoryHolder)
                && !(top.getHolder() instanceof EnchantInventoryHolder)
                && !(top.getHolder() instanceof RepairInventoryHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize() && !guiService.isInputSlot(slot))) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && (event.getInventory().getHolder() instanceof EnhancementInventoryHolder
                || event.getInventory().getHolder() instanceof PromotionInventoryHolder
                || event.getInventory().getHolder() instanceof EnchantInventoryHolder
                || event.getInventory().getHolder() instanceof RepairInventoryHolder)) {
            guiService.returnInputs(player, event.getInventory());
        }
    }

    private boolean unsafe(ClickType click) {
        return click == ClickType.NUMBER_KEY || click == ClickType.DOUBLE_CLICK || click == ClickType.CREATIVE || click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.SWAP_OFFHAND;
    }
}
