package com.hyunseo.hyunseorpg.skill;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.weapon.WeaponProficiencyService;
import org.bukkit.entity.Player;

/** Skill points unlock skills after their weapon proficiency requirement. */
public final class SkillStatService {
    private final ConfigService configService;
    private final PlayerDataService playerDataService;
    private final SkillRegistry skillRegistry;
    private final WeaponProficiencyService weaponProficiencyService;

    public SkillStatService(
            ConfigService configService,
            PlayerDataService playerDataService,
            SkillRegistry skillRegistry,
            WeaponProficiencyService weaponProficiencyService
    ) {
        this.configService = configService;
        this.playerDataService = playerDataService;
        this.skillRegistry = skillRegistry;
        this.weaponProficiencyService = weaponProficiencyService;
    }

    public int getSkillStatLevel(Player player, SkillStatType skillStatType) {
        return playerDataService.getOrLoad(player).getSkillStatLevel(skillStatType.id());
    }

    public PlayerRPGData getPlayerData(Player player) {
        return playerDataService.getOrLoad(player);
    }

    public int getSkillPoints(Player player) {
        return playerDataService.getOrLoad(player).getSkillPoints();
    }

    public int getSkillLevel(Player player, SkillData skillData) {
        return playerDataService.getOrLoad(player).getSkillLevel(skillData.skillId());
    }

    public boolean isSkillUnlocked(Player player, SkillData skillData) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        return data.hasUnlockedSkill(skillData.skillId()) && data.getSkillLevel(skillData.skillId()) > 0;
    }

    public int getMaxSkillStat() {
        return configService.getStatsInt("max-skill-stat", 8);
    }

    public SkillStatInvestmentResult investSkillStatPoint(Player player, SkillStatType skillStatType) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        int maxLevel = getMaxSkillStat();
        int currentLevel = data.getSkillStatLevel(skillStatType.id());
        if (currentLevel >= maxLevel) {
            return SkillStatInvestmentResult.failure("스킬 스탯이 이미 최대치입니다.", currentLevel, maxLevel, data.getSkillPoints());
        }
        if (data.getSkillPoints() <= 0) {
            return SkillStatInvestmentResult.failure("사용 가능한 스킬 포인트가 없습니다.", currentLevel, maxLevel, data.getSkillPoints());
        }
        data.setSkillPoints(data.getSkillPoints() - 1);
        data.setSkillStatLevel(skillStatType.id(), currentLevel + 1);
        playerDataService.savePlayer(player);
        return SkillStatInvestmentResult.success(skillStatType, currentLevel + 1, maxLevel, data.getSkillPoints());
    }

    public SkillUpgradeResult upgradeSkill(Player player, String skillId) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        SkillData skillData = skillRegistry.getSkill(skillId).orElse(null);
        if (skillData == null) {
            return SkillUpgradeResult.failure("알 수 없는 스킬입니다: " + skillId, 0, 0, data.getSkillPoints());
        }
        int currentLevel = data.getSkillLevel(skillData.skillId());
        boolean alreadyUnlocked = data.hasUnlockedSkill(skillData.skillId()) && currentLevel > 0;
        if (!alreadyUnlocked && !weaponProficiencyService.meetsRequirement(player, skillData.weaponType(), skillData.requiredProficiencyLevel())) {
            return SkillUpgradeResult.failure(
                    skillData.weaponType().displayName() + " 숙련도 Lv." + skillData.requiredProficiencyLevel() + "이 필요합니다.",
                    data.getSkillLevel(skillData.skillId()),
                    skillData.maxLevel(),
                    data.getSkillPoints()
            );
        }
        if (currentLevel >= skillData.maxLevel()) {
            return SkillUpgradeResult.failure("스킬이 이미 최대 레벨입니다.", currentLevel, skillData.maxLevel(), data.getSkillPoints());
        }
        if (data.getSkillPoints() <= 0) {
            return SkillUpgradeResult.failure("사용 가능한 스킬 포인트가 없습니다.", currentLevel, skillData.maxLevel(), data.getSkillPoints());
        }
        data.setSkillPoints(data.getSkillPoints() - 1);
        data.unlockSkill(skillData.skillId());
        data.setSkillLevel(skillData.skillId(), currentLevel + 1);
        playerDataService.savePlayer(player);
        return SkillUpgradeResult.success(skillData, currentLevel + 1, data.getSkillPoints());
    }

    public SkillRegistry skillRegistry() {
        return skillRegistry;
    }
}
