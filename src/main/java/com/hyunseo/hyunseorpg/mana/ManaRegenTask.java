package com.hyunseo.hyunseorpg.mana;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ManaRegenTask implements Runnable {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final ManaService manaService;
    private BukkitTask task;
    private long intervalTicks;

    public ManaRegenTask(JavaPlugin plugin, ConfigService configService, ManaService manaService) {
        this.plugin = plugin;
        this.configService = configService;
        this.manaService = manaService;
    }

    public void start() {
        if (task != null) {
            return;
        }

        this.intervalTicks = Math.max(1L, configService.getLong("mana.regen-interval-ticks", 20L));
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, intervalTicks, intervalTicks);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            manaService.regenerate(player, intervalTicks);
        }
    }
}
