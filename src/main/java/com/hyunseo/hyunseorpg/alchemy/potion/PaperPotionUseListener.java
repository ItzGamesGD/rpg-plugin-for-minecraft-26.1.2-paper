package com.hyunseo.hyunseorpg.alchemy.potion;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/** Routes only canonical PDC potions through PotionUseService before vanilla consumption. */
public final class PaperPotionUseListener implements Listener {
    private final PotionPdcContract<ItemStack> pdc;
    private final PotionUseService<ItemStack> useService;
    private final PaperPotionUseService paperService;

    public PaperPotionUseListener(PotionPdcContract<ItemStack> pdc,
                                  PotionUseService<ItemStack> useService) {
        this.pdc = pdc;
        this.useService = useService;
        this.paperService = useService instanceof PaperPotionUseService service ? service : null;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        if (paperService == null) return;
        ThrownPotion potion = event.getPotion();
        ItemStack item = potion.getItem();
        if (pdc.readPotionId(item).isBlank() || !"SPLASH".equalsIgnoreCase(pdc.readDelivery(item))) return;
        event.setCancelled(true);
        UUID source = potion.getShooter() instanceof Player player ? player.getUniqueId() : null;
        for (LivingEntity target : potion.getLocation().getNearbyLivingEntities(4.0D, 2.0D, 4.0D)) {
            paperService.useOnTarget(source, target.getUniqueId(), item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLingering(LingeringPotionSplashEvent event) {
        if (paperService == null) return;
        ThrownPotion potion = event.getEntity();
        ItemStack item = potion.getItem();
        if (pdc.readPotionId(item).isBlank() || !"LINGERING".equalsIgnoreCase(pdc.readDelivery(item))) return;
        UUID source = potion.getShooter() instanceof Player player ? player.getUniqueId() : null;
        Bukkit.getScheduler().runTaskLater((org.bukkit.plugin.java.JavaPlugin) potion.getServer().getPluginManager()
                .getPlugin("HyunseoRPG"), () -> {
            for (LivingEntity target : potion.getLocation().getNearbyLivingEntities(3.5D, 2.0D, 3.5D)) {
                paperService.useOnTarget(source, target.getUniqueId(), item);
            }
        }, 1L);
    }
}
