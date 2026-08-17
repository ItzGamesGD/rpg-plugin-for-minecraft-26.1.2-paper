package com.hyunseo.hyunseorpg.mana;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.stat.StatCalculator;
import com.hyunseo.hyunseorpg.stat.StatModifierService;
import org.bukkit.entity.Player;

public final class ManaService {
    private final ConfigService configService;
    private final PlayerDataService playerDataService;
    private final ManaBossBarService bossBarService;
    private final StatCalculator statCalculator;
    private final StatModifierService statModifierService;

    public ManaService(
            ConfigService configService,
            PlayerDataService playerDataService,
            ManaBossBarService bossBarService,
            StatCalculator statCalculator,
            StatModifierService statModifierService
    ) {
        this.configService = configService;
        this.playerDataService = playerDataService;
        this.bossBarService = bossBarService;
        this.statCalculator = statCalculator;
        this.statModifierService = statModifierService;
    }

    public void refresh(Player player) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        double maxMana = getMaxMana(data);
        if (maxMana <= 0.0D) {
            data.setCurrentMana(0.0D);
            bossBarService.remove(player);
            return;
        }

        data.setCurrentMana(clamp(data.getCurrentMana(), 0.0D, maxMana));
        bossBarService.showOrUpdate(player, data.getCurrentMana(), maxMana);
    }

    public void fillToMax(Player player) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        double maxMana = getMaxMana(data);
        data.setCurrentMana(maxMana);
        bossBarService.showOrUpdate(player, data.getCurrentMana(), maxMana);
    }

    public void clearMana(Player player) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setCurrentMana(0.0D);
        bossBarService.remove(player);
    }

    public void setCurrentMana(Player player, double amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        double maxMana = getMaxMana(data);
        data.setCurrentMana(clamp(amount, 0.0D, Math.max(0.0D, maxMana)));
        playerDataService.savePlayer(player);
        bossBarService.showOrUpdate(player, data.getCurrentMana(), maxMana);
    }

    public void addCurrentMana(Player player, double amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        setCurrentMana(player, data.getCurrentMana() + amount);
    }

    public void regenerate(Player player) {
        regenerate(player, 20L);
    }

    public void regenerate(Player player, long intervalTicks) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        double maxMana = getMaxMana(data);
        if (maxMana <= 0.0D) {
            bossBarService.remove(player);
            return;
        }

        double regeneratedMana = Math.min(maxMana, data.getCurrentMana() + getRegenAmount(data, intervalTicks));
        data.setCurrentMana(regeneratedMana);
        bossBarService.showOrUpdate(player, regeneratedMana, maxMana);
    }

    public boolean hasEnoughMana(Player player, double requiredMana) {
        return playerDataService.getOrLoad(player).getCurrentMana() >= requiredMana;
    }

    public double getCurrentMana(Player player) {
        return playerDataService.getOrLoad(player).getCurrentMana();
    }

    public double getMaxMana(Player player) {
        return getMaxMana(playerDataService.getOrLoad(player));
    }

    public boolean consumeMana(Player player, double amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (amount < 0.0D || data.getCurrentMana() < amount) {
            return false;
        }

        data.setCurrentMana(data.getCurrentMana() - amount);
        bossBarService.showOrUpdate(player, data.getCurrentMana(), getMaxMana(data));
        return true;
    }

    public double getMaxMana(PlayerRPGData data) {
        return statCalculator.calculateMaxMana(data, statModifierService.getModifiers(data.getUuid()));
    }

    private double getRegenAmount(PlayerRPGData data, long intervalTicks) {
        double regenPerSecond = statCalculator.calculateManaRegenPerSecond(data, statModifierService.getModifiers(data.getUuid()));
        return regenPerSecond * Math.max(1L, intervalTicks) / 20.0D;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
