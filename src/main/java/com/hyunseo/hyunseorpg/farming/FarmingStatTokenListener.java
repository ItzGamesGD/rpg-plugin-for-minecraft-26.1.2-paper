package com.hyunseo.hyunseorpg.farming;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Main-hand interaction boundary for account-bound farming tokens. */
public final class FarmingStatTokenListener implements Listener {
    private final FarmingStatTokenService tokens;

    public FarmingStatTokenListener(FarmingStatTokenService tokens) {
        this.tokens = tokens;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (tokens.definitionForItem(event.getItem()).isEmpty()) return;
        event.setCancelled(true);
        switch (tokens.use(event.getPlayer(), event.getItem())) {
            case SUCCESS -> event.getPlayer().sendMessage(ChatColor.GREEN + "농사 증표 사용이 기록되었습니다.");
            case LIMIT_REACHED -> event.getPlayer().sendMessage(ChatColor.RED + "이 농사 증표는 이미 사용 한도에 도달했습니다.");
            case NOT_OWNER -> event.getPlayer().sendMessage(ChatColor.RED + "이 증표는 다른 플레이어에게 귀속되어 있습니다.");
            case INVALID -> event.getPlayer().sendMessage(ChatColor.RED + "현재 사용할 수 없는 농사 증표입니다.");
        }
    }
}
