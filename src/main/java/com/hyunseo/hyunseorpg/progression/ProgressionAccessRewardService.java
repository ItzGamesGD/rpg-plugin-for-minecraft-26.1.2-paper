package com.hyunseo.hyunseorpg.progression;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.activity.ActivityType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/** Rewards common vanilla activities with the first enhancement material. */
public final class ProgressionAccessRewardService {
    private final ConfigService config;
    private final RPGItemService itemService;
    private final InventoryDeliveryService deliveryService;

    public ProgressionAccessRewardService(ConfigService config, RPGItemService itemService,
                                          InventoryDeliveryService deliveryService) {
        this.config = config;
        this.itemService = itemService;
        this.deliveryService = deliveryService;
    }

    public void roll(Player player, ActivityType activity, Location location) {
        if (player == null || activity == null || !config.getProgressionLoopBoolean("access-materials.enabled", true)) {
            return;
        }
        double chance = Math.max(0.0D, Math.min(1.0D,
                config.getProgressionLoopDouble("access-materials.activities." + activity.name(), 0.0D)));
        if (chance <= 0.0D || ThreadLocalRandom.current().nextDouble() >= chance) return;

        String itemId = config.getProgressionLoopString("access-materials.fragment-item-id", "basic_upgrade_fragment");
        int amount = Math.max(1, config.getProgressionLoopInt("access-materials.amount", 1));
        ItemStack fragment = itemService.create(itemId, amount).orElse(null);
        if (fragment == null) return;
        deliveryService.giveOrDrop(player, location, fragment);
        player.sendMessage(Component.text("강화석 파편을 획득했습니다.", NamedTextColor.AQUA));
    }
}
