package com.hyunseo.hyunseorpg.exp;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** Manages base progression and combat specialization progression. */
public final class LevelService {
    private final PlayerDataService playerDataService;
    private final ExpTable expTable;

    public LevelService(PlayerDataService playerDataService, ExpTable expTable) {
        this.playerDataService = playerDataService;
        this.expTable = expTable;
    }

    public LevelUpResult addBaseExp(Player player, long amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (amount <= 0L) {
            refreshVanillaExpBar(player);
            return resultWithoutChange(data);
        }

        int oldLevel = data.getBaseLevel();
        int levelsGained = 0;
        int statPointsGained = 0;
        data.setBaseExp(data.getBaseExp() + amount);

        int maxLevel = expTable.getMaxBaseLevel();
        while (data.getBaseLevel() < maxLevel) {
            long requiredExp = expTable.getRequiredExpForNextBaseLevel(data.getBaseLevel());
            if (data.getBaseExp() < requiredExp) {
                break;
            }
            data.setBaseExp(data.getBaseExp() - requiredExp);
            data.setBaseLevel(data.getBaseLevel() + 1);
            int reward = expTable.getStatPointsPerBaseLevel();
            data.setStatPoints(data.getStatPoints() + reward);
            statPointsGained += reward;
            levelsGained++;
        }

        if (data.getBaseLevel() >= maxLevel) {
            data.setBaseLevel(maxLevel);
            data.setBaseExp(0L);
        }

        playerDataService.savePlayer(player);
        refreshVanillaExpBar(player);
        LevelUpResult result = new LevelUpResult(oldLevel, data.getBaseLevel(), levelsGained, statPointsGained,
                data.getBaseExp(), getRequiredExpForNextLevel(data));
        if (result.leveledUp()) {
            player.sendMessage(Component.text("기본 레벨이 Lv." + result.newLevel()
                    + "이 되었습니다. 스탯 포인트 +" + result.statPointsGained(), NamedTextColor.GREEN));
        }
        return result;
    }

    /**
     * Legacy YAML field names retain backwards compatibility, but this progress
     * is no longer gated by the removed combat-class selection.
     */
    public ClassLevelUpResult addClassExp(Player player, long amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (amount <= 0L) {
            return classResultWithoutChange(data);
        }

        int oldLevel = data.getClassLevel();
        int levelsGained = 0;
        int skillPointsGained = 0;
        int classStatPointsGained = 0;
        data.setClassExp(data.getClassExp() + amount);

        int maxLevel = expTable.getMaxClassLevel();
        while (data.getClassLevel() < maxLevel) {
            long requiredExp = expTable.getRequiredExpForNextClassLevel(data.getClassLevel());
            if (data.getClassExp() < requiredExp) {
                break;
            }
            data.setClassExp(data.getClassExp() - requiredExp);
            data.setClassLevel(data.getClassLevel() + 1);
            int skillPointReward = expTable.getSkillPointsPerClassLevel();
            int specializationPointReward = expTable.getClassStatPointsPerClassLevel();
            data.setSkillPoints(data.getSkillPoints() + skillPointReward);
            data.setClassStatPoints(data.getClassStatPoints() + specializationPointReward);
            skillPointsGained += skillPointReward;
            classStatPointsGained += specializationPointReward;
            levelsGained++;
        }

        if (data.getClassLevel() >= maxLevel) {
            data.setClassLevel(maxLevel);
            data.setClassExp(0L);
        }

        playerDataService.savePlayer(player);
        ClassLevelUpResult result = new ClassLevelUpResult(oldLevel, data.getClassLevel(), levelsGained,
                skillPointsGained, classStatPointsGained, data.getClassExp(), getRequiredClassExpForNextLevel(data));
        if (result.leveledUp()) {
            player.sendMessage(Component.text("전문화 레벨이 Lv." + result.newLevel()
                    + "이 되었습니다. 스킬 포인트 +" + result.skillPointsGained()
                    + ", 무기 전문화 포인트 +" + result.classStatPointsGained(), NamedTextColor.AQUA));
        }
        return result;
    }

    public void setBaseLevel(Player player, int level) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setBaseLevel(Math.max(1, Math.min(level, expTable.getMaxBaseLevel())));
        data.setBaseExp(0L);
        playerDataService.savePlayer(player);
        refreshVanillaExpBar(player);
    }

    public void setBaseExp(Player player, long exp) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setBaseExp(Math.max(0L, exp));
        playerDataService.savePlayer(player);
        refreshVanillaExpBar(player);
    }

    public void setClassLevel(Player player, int level) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setClassLevel(Math.max(1, Math.min(level, expTable.getMaxClassLevel())));
        data.setClassExp(0L);
        playerDataService.savePlayer(player);
    }

    public void setClassExp(Player player, long exp) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setClassExp(Math.max(0L, exp));
        playerDataService.savePlayer(player);
    }

    public void refreshVanillaExpBar(Player player) {
        if (!expTable.shouldSyncVanillaExpBar()) {
            return;
        }
        PlayerRPGData data = playerDataService.getOrLoad(player);
        player.setLevel(data.getBaseLevel());
        long requiredExp = getRequiredExpForNextLevel(data);
        player.setExp(requiredExp <= 0L ? 1.0F
                : Math.max(0.0F, Math.min(1.0F, (float) data.getBaseExp() / (float) requiredExp)));
    }

    public long getRequiredExpForNextLevel(PlayerRPGData data) {
        return data.getBaseLevel() >= expTable.getMaxBaseLevel() ? 0L
                : expTable.getRequiredExpForNextBaseLevel(data.getBaseLevel());
    }

    public long getRequiredClassExpForNextLevel(PlayerRPGData data) {
        return data.getClassLevel() >= expTable.getMaxClassLevel() ? 0L
                : expTable.getRequiredExpForNextClassLevel(data.getClassLevel());
    }

    public int getMaxBaseLevel() {
        return expTable.getMaxBaseLevel();
    }

    public int getMaxClassLevel() {
        return expTable.getMaxClassLevel();
    }

    private LevelUpResult resultWithoutChange(PlayerRPGData data) {
        return new LevelUpResult(data.getBaseLevel(), data.getBaseLevel(), 0, 0,
                data.getBaseExp(), getRequiredExpForNextLevel(data));
    }

    private ClassLevelUpResult classResultWithoutChange(PlayerRPGData data) {
        return new ClassLevelUpResult(data.getClassLevel(), data.getClassLevel(), 0, 0, 0,
                data.getClassExp(), getRequiredClassExpForNextLevel(data));
    }
}
