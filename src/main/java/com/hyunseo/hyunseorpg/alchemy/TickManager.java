package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TickManager {
    private final JavaPlugin plugin;
    private int taskId = -1;
    private Runnable action;

    public TickManager(JavaPlugin plugin) { this.plugin = plugin; }
    public void start(Runnable action) {
        stop();
        this.action = action;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, action, 1L, 1L);
    }
    public void stop() {
        if (taskId >= 0) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        action = null;
    }

    public boolean isRunning() {
        return taskId >= 0;
    }
}
