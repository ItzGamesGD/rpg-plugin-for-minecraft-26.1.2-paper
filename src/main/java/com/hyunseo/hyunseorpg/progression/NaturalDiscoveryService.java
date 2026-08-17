package com.hyunseo.hyunseorpg.progression;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemData;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/** Connects first-time item discovery and post-End progression to existing player flags. */
public final class NaturalDiscoveryService implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final PlayerDataService playerDataService;
    private final RPGItemService itemService;

    public NaturalDiscoveryService(JavaPlugin plugin, ConfigService configService,
                                  PlayerDataService playerDataService, RPGItemService itemService) {
        this.plugin = plugin;
        this.configService = configService;
        this.playerDataService = playerDataService;
        this.itemService = itemService;
    }

    public void discoverDeliveredItem(Player player, ItemStack item) {
        discover(player, item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) discover(player, event.getItem().getItemStack());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (ItemStack item : event.getPlayer().getInventory().getContents()) {
            discover(event.getPlayer(), item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!configService.getBoolean("progression.ender-dragon.enabled", true)) return;
        Player killer = dragon.getKiller();
        if (killer == null) return;
        PlayerRPGData data = playerDataService.getOrLoad(killer);
        String flag = configService.getString("progression.ender-dragon.flag", "post_ender_dragon");
        if (data.hasProgressionFlag(flag)) return;
        data.addProgressionFlag(flag);
        playerDataService.savePlayer(killer);
        String message = configService.getString("progression.ender-dragon.message",
                "엔더드래곤 처치 기록이 저장되었습니다. 이후 콘텐츠가 해금될 수 있습니다.");
        killer.sendMessage(Component.text(message, NamedTextColor.LIGHT_PURPLE));
    }

    private void discover(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()
                || !configService.getBoolean("natural-discovery.enabled", true)) return;
        String itemId = itemService.getItemId(item).orElse("").toLowerCase(Locale.ROOT);
        if (itemId.isBlank() || !isDiscoverable(itemId)) return;
        PlayerRPGData data = playerDataService.getOrLoad(player);
        String flag = "discovered_item_" + itemId;
        if (data.hasProgressionFlag(flag)) return;
        data.addProgressionFlag(flag);
        playerDataService.savePlayer(player);
        RPGItemData itemData = itemService.getData(itemId).orElse(null);
        String displayName = itemData == null ? itemId : itemData.displayName();
        String message = configService.getString("natural-discovery.message",
                "[!] {item}을(를) 처음 발견했습니다. 아이템 설명과 모루 반응을 확인해 보세요.")
                .replace("{item}", displayName);
        player.sendMessage(Component.text(message, NamedTextColor.AQUA));
        plugin.getLogger().fine("First item discovery: " + player.getName() + " -> " + itemId);
    }

    private boolean isDiscoverable(String itemId) {
        List<String> configured = configService.getStringList("natural-discovery.item-ids");
        return configured.isEmpty() || configured.stream().anyMatch(value -> value.equalsIgnoreCase(itemId));
    }
}
