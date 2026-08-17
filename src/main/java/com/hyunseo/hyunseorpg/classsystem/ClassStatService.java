package com.hyunseo.hyunseorpg.classsystem;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Compatibility service for existing class-stat data. In v2 these are weapon
 * specialization stats and are no longer restricted by selectedClass.
 */
public final class ClassStatService {
    private final PlayerDataService playerDataService;
    private final ClassStatRegistry classStatRegistry;

    public ClassStatService(PlayerDataService playerDataService, ClassStatRegistry classStatRegistry) {
        this.playerDataService = playerDataService;
        this.classStatRegistry = classStatRegistry;
    }

    public List<ClassStatData> getAvailableStats(Player player) {
        return classStatRegistry.getAll();
    }

    public int getClassStatLevel(Player player, ClassStatData classStatData) {
        return playerDataService.getOrLoad(player).getClassStatLevel(classStatData.fullId());
    }

    public int getClassStatPoints(Player player) {
        return playerDataService.getOrLoad(player).getClassStatPoints();
    }

    public ClassStatInvestmentResult investClassStatPoint(Player player, String fullId) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        ClassStatData stat = classStatRegistry.get(fullId).orElse(null);
        if (stat == null) {
            return ClassStatInvestmentResult.failure("Unknown weapon specialization stat.", 0, 0, data.getClassStatPoints());
        }
        int currentLevel = data.getClassStatLevel(stat.fullId());
        if (currentLevel >= stat.maxLevel()) {
            return ClassStatInvestmentResult.failure("Weapon specialization stat is at maximum.", currentLevel, stat.maxLevel(), data.getClassStatPoints());
        }
        if (data.getClassStatPoints() < stat.pointCost()) {
            return ClassStatInvestmentResult.failure("Not enough weapon specialization points.", currentLevel, stat.maxLevel(), data.getClassStatPoints());
        }
        data.setClassStatPoints(data.getClassStatPoints() - stat.pointCost());
        data.setClassStatLevel(stat.fullId(), currentLevel + 1);
        playerDataService.savePlayer(player);
        return ClassStatInvestmentResult.success(stat, currentLevel + 1, data.getClassStatPoints());
    }

    public double getClassStatBonus(Player player, String skillId, String targetValue) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        String normalizedSkillId = normalize(skillId);
        String normalizedTargetValue = normalize(targetValue);
        return classStatRegistry.getAll().stream()
                .filter(stat -> stat.skillId().equals(normalizedSkillId))
                .filter(stat -> stat.targetValue().equals(normalizedTargetValue))
                .mapToDouble(stat -> data.getClassStatLevel(stat.fullId()) * stat.bonusPerLevel())
                .sum();
    }

    public ClassStatRegistry registry() {
        return classStatRegistry;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
