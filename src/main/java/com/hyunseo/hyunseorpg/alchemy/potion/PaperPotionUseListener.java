package com.hyunseo.hyunseorpg.alchemy.potion;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

/** Routes only canonical PDC potions through PotionUseService before vanilla consumption. */
public final class PaperPotionUseListener implements Listener {
    private final PotionPdcContract<ItemStack> pdc;
    private final PotionUseService<ItemStack> useService;

    public PaperPotionUseListener(PotionPdcContract<ItemStack> pdc,
                                  PotionUseService<ItemStack> useService) {
        this.pdc = pdc;
        this.useService = useService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (pdc.readPotionId(item).isBlank()) return;
        Player player = event.getPlayer();
        PotionUseService.UseResult result = useService.use(player.getUniqueId(), item);
        if (result != PotionUseService.UseResult.USED) {
            event.setCancelled(true);
            player.sendMessage(Component.text("이 물약은 사용할 수 없습니다. (" + result.name() + ")"));
        }
    }

    /** Transformed delivery potions are handled here instead of falling through vanilla drink behavior. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTransformedInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || pdc.readPotionId(item).isBlank()) return;
        String delivery = pdc.readDelivery(item);
        if (delivery.isBlank() || "ORIGINAL".equalsIgnoreCase(delivery)) return;
        event.setCancelled(true);
        PotionUseService.UseResult result = useService.use(event.getPlayer().getUniqueId(), item);
        if (result != PotionUseService.UseResult.USED) {
            event.getPlayer().sendMessage(Component.text("이 변환 포션은 사용할 수 없습니다. (" + result.name() + ")"));
            return;
        }
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) {
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) event.getPlayer().getInventory().setItemInOffHand(null);
            else event.getPlayer().getInventory().setItemInMainHand(null);
        }
    }
}
