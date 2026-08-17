package com.hyunseo.hyunseorpg.mana;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ManaBossBarService {
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public void showOrUpdate(Player player, double currentMana, double maxMana) {
        if (maxMana <= 0.0D) {
            remove(player);
            return;
        }

        BossBar bossBar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> createBossBar());
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }

        bossBar.setTitle(formatTitle(currentMana, maxMana));
        bossBar.setProgress(clampProgress(currentMana / maxMana));
        bossBar.setVisible(true);
    }

    public void remove(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void removeAll() {
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
    }

    private BossBar createBossBar() {
        return Bukkit.createBossBar("MP 0 / 0", BarColor.BLUE, BarStyle.SEGMENTED_10);
    }

    private String formatTitle(double currentMana, double maxMana) {
        return "MP " + Math.round(currentMana) + " / " + Math.round(maxMana);
    }

    private double clampProgress(double progress) {
        if (progress < 0.0D) {
            return 0.0D;
        }
        if (progress > 1.0D) {
            return 1.0D;
        }
        return progress;
    }
}
