package com.hyunseo.hyunseorpg.economy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.farming.AbundancePointService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CoinDisplayTask {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final CoinService coinService;
    private final AbundancePointService abundancePoints;
    private BukkitTask task;

    public CoinDisplayTask(JavaPlugin plugin, ConfigService configService, CoinService coinService,
                           AbundancePointService abundancePoints) {
        this.plugin = plugin;
        this.configService = configService;
        this.coinService = coinService;
        this.abundancePoints = abundancePoints;
    }

    public void start() {
        cancel();
        if (!configService.getBoolean("economy.coin-display.enabled", true)) {
            return;
        }

        long intervalTicks = Math.max(20L, configService.getLong("economy.coin-display.interval-ticks", 40L));
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sendDisplays, intervalTicks, intervalTicks);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void sendDisplays() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendActionBar(Component.text(
                    "Coins: " + coinService.getCoins(player)
                            + "  |  풍요: " + abundancePoints.getPoints(player.getUniqueId()),
                    NamedTextColor.GOLD));
        }
    }
}
