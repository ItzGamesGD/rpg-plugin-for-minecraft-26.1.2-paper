package com.hyunseo.hyunseorpg.boss;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/** Blocks vanilla boss re-summon setup at the canonical End portal and Wither base. */
public final class BossSummonProtectionListener implements Listener {
    private final BossSessionManager sessions;

    public BossSummonProtectionListener(BossSessionManager sessions) {
        this.sessions = sessions;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        Block clicked = event.getClickedBlock();
        if (item == null || clicked == null) return;

        if (item.getType() == Material.END_CRYSTAL && isEndPortalCore(clicked)) {
            if (sessions.isSummonBlocked(BossType.ENDER_DRAGON)) {
                event.setCancelled(true);
                sessions.showRemainingTime(event.getPlayer(), BossType.ENDER_DRAGON);
            }
            return;
        }

        if (isWitherSkull(item.getType()) && isSoulBase(clicked.getType())) {
            if (sessions.isSummonBlocked(BossType.WITHER)) {
                event.setCancelled(true);
                sessions.showRemainingTime(event.getPlayer(), BossType.WITHER);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSkullPlace(BlockPlaceEvent event) {
        if (!isWitherSkull(event.getBlockPlaced().getType())) return;
        Block below = event.getBlockPlaced().getRelative(0, -1, 0);
        if (!isSoulBase(below.getType())) return;
        if (sessions.isSummonBlocked(BossType.WITHER)) {
            event.setCancelled(true);
            sessions.showRemainingTime(event.getPlayer(), BossType.WITHER);
        }
    }

    private boolean isEndPortalCore(Block block) {
        if (block.getWorld().getEnvironment() != World.Environment.THE_END) return false;
        double x = block.getX() + 0.5D;
        double z = block.getZ() + 0.5D;
        return x * x + z * z <= 25.0D;
    }

    private boolean isSoulBase(Material material) {
        return material == Material.SOUL_SAND || material == Material.SOUL_SOIL;
    }

    private boolean isWitherSkull(Material material) {
        return material == Material.WITHER_SKELETON_SKULL || material == Material.WITHER_SKELETON_WALL_SKULL;
    }
}
