package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/** Gives ordinary vanilla mobs one configurable random elemental fragment. */
public final class VanillaMobFragmentDropListener implements Listener {
    private final ConfigService config;
    private final MobService mobService;
    private final RPGItemService itemService;
    private final InventoryDeliveryService delivery;
    private final ElementalFragmentPolicy elementalPolicy;

    public VanillaMobFragmentDropListener(ConfigService config, MobService mobService,
                                          RPGItemService itemService, InventoryDeliveryService delivery,
                                          ElementalFragmentPolicy elementalPolicy) {
        this.config = config;
        this.mobService = mobService;
        this.itemService = itemService;
        this.delivery = delivery;
        this.elementalPolicy = elementalPolicy;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity defeated = event.getEntity();
        Player killer = defeated.getKiller();
        if (killer == null || defeated instanceof Player) return;
        String mobId = mobService.getMobId(defeated);
        String configuredItem = elementalPolicy.allowedItem(killer, defeated).orElse("");
        if (configuredItem.isBlank()) return;
        if (!config.getMobsBoolean("vanilla-fragment-drops.enabled", true)) return;
        double chance = Math.max(0.0D, Math.min(1.0D,
                config.getMobsDouble("vanilla-fragment-drops.chance", 1.0D)));
        if (ThreadLocalRandom.current().nextDouble() > chance) return;
        String itemId = configuredItem;
        int min = Math.max(1, config.getMobsInt("vanilla-fragment-drops.min-amount", 1));
        int max = Math.max(min, config.getMobsInt("vanilla-fragment-drops.max-amount", min));
        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        itemService.create(itemId, amount).ifPresent(item -> delivery.giveOrDiscard(killer, item));
    }
}
