package com.hyunseo.hyunseorpg.alchemy.potion;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
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
import java.util.HashMap;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import com.hyunseo.hyunseorpg.alchemy.catalyst.BoundedSpecialCatalystExecutionService;

/** Routes only canonical PDC potions through PotionUseService before vanilla consumption. */
public final class PaperPotionUseListener implements Listener {
    private final PotionPdcContract<ItemStack> pdc;
    private final PotionUseService<ItemStack> useService;
    private final PaperPotionUseService paperService;
    private final BoundedSpecialCatalystExecutionService specialExecutions;
    private final JavaPlugin plugin;
    private final Map<UUID, LingeringPayload> lingering = new HashMap<>();
    private final Map<UUID, BukkitTask> lingeringCleanup = new HashMap<>();

    public PaperPotionUseListener(PotionPdcContract<ItemStack> pdc,
                                  PotionUseService<ItemStack> useService) {
        this(null, pdc, useService, null);
    }

    public PaperPotionUseListener(JavaPlugin plugin, PotionPdcContract<ItemStack> pdc,
                                  PotionUseService<ItemStack> useService) {
        this(plugin, pdc, useService, null);
    }

    public PaperPotionUseListener(JavaPlugin plugin, PotionPdcContract<ItemStack> pdc,
                                  PotionUseService<ItemStack> useService,
                                  BoundedSpecialCatalystExecutionService specialExecutions) {
        this.plugin = plugin;
        this.pdc = pdc;
        this.useService = useService;
        this.paperService = useService instanceof PaperPotionUseService service ? service : null;
        this.specialExecutions = specialExecutions;
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
        if ("slime".equalsIgnoreCase(pdc.readCatalystId(item)) && specialExecutions != null) {
            specialExecutions.handleSlimeSplash(potion, pdc.readPotionId(item), source, event.getAffectedEntities());
            return;
        }
        if ("echo_shard".equalsIgnoreCase(pdc.readCatalystId(item)) && specialExecutions != null) {
            specialExecutions.handleEchoSplash(pdc.readPotionId(item), source, event.getAffectedEntities());
            return;
        }
        for (LivingEntity target : event.getAffectedEntities()) {
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
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        if (cloud == null) return;
        synchronized (lingering) {
            lingering.put(cloud.getUniqueId(), new LingeringPayload(source, item.clone()));
        }
        if (plugin != null) {
            BukkitTask cleanup = Bukkit.getScheduler().runTaskLater(plugin, () -> removeCloud(cloud.getUniqueId()),
                    Math.max(20L, cloud.getDuration() + 40L));
            synchronized (lingering) { lingeringCleanup.put(cloud.getUniqueId(), cleanup); }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLingeringApply(AreaEffectCloudApplyEvent event) {
        LingeringPayload payload;
        synchronized (lingering) { payload = lingering.get(event.getEntity().getUniqueId()); }
        if (payload == null || paperService == null) return;
        event.setCancelled(true);
        for (LivingEntity target : event.getAffectedEntities()) {
            paperService.useOnTarget(payload.sourceId(), target.getUniqueId(), payload.item());
        }
    }

    private void removeCloud(UUID cloudId) {
        synchronized (lingering) {
            lingering.remove(cloudId);
            BukkitTask task = lingeringCleanup.remove(cloudId);
            if (task != null && !task.isCancelled()) task.cancel();
        }
    }

    private record LingeringPayload(UUID sourceId, ItemStack item) { }
}
