package com.hyunseo.hyunseorpg.weapon;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.entity.Player;

/** Owns weapon progression and never checks a player's lifestyle profession. */
public final class WeaponProficiencyService {
    private final ConfigService configService;
    private final PlayerDataService playerDataService;

    public WeaponProficiencyService(ConfigService configService, PlayerDataService playerDataService) {
        this.configService = configService;
        this.playerDataService = playerDataService;
    }

    public int getLevel(Player player, WeaponType weaponType) {
        return playerDataService.getOrLoad(player).getWeaponProficiencyLevel(weaponType.id());
    }

    public long getExperience(Player player, WeaponType weaponType) {
        return playerDataService.getOrLoad(player).getWeaponProficiencyExp(weaponType.id());
    }

    public boolean meetsRequirement(Player player, WeaponType weaponType, int requiredLevel) {
        return getLevel(player, weaponType) >= Math.max(0, requiredLevel);
    }

    public WeaponProficiencyResult addExperience(Player player, WeaponType weaponType, long amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (amount <= 0L) {
            return new WeaponProficiencyResult(weaponType, data.getWeaponProficiencyLevel(weaponType.id()), data.getWeaponProficiencyExp(weaponType.id()), false);
        }

        int maxLevel = Math.max(1, configService.getWeaponsInt("proficiency.max-level", 100));
        int level = Math.max(1, data.getWeaponProficiencyLevel(weaponType.id()));
        long experience = data.getWeaponProficiencyExp(weaponType.id()) + amount;
        boolean leveledUp = false;
        while (level < maxLevel) {
            long required = getRequiredExperience(level);
            if (experience < required) {
                break;
            }
            experience -= required;
            level++;
            leveledUp = true;
        }
        if (level >= maxLevel) {
            experience = 0L;
        }

        data.setWeaponProficiencyLevel(weaponType.id(), level);
        data.setWeaponProficiencyExp(weaponType.id(), experience);
        playerDataService.savePlayer(player);
        return new WeaponProficiencyResult(weaponType, level, experience, leveledUp);
    }

    public long getRequiredExperience(int currentLevel) {
        long base = Math.max(1L, configService.getWeaponsInt("proficiency.base-exp", 100));
        long perLevel = Math.max(0L, configService.getWeaponsInt("proficiency.exp-per-level", 35));
        return base + Math.max(0, currentLevel - 1) * perLevel;
    }
}
