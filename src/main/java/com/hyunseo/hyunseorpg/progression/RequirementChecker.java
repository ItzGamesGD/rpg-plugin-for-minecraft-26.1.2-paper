package com.hyunseo.hyunseorpg.progression;

import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.entity.Player;

import java.util.List;

public final class RequirementChecker {
    private final PlayerDataService playerDataService;
    private final CoinService coinService;

    public RequirementChecker(PlayerDataService playerDataService, CoinService coinService) {
        this.playerDataService = playerDataService;
        this.coinService = coinService;
    }

    public boolean areMet(Player player, List<Requirement> requirements) {
        for (Requirement requirement : requirements) {
            if (!isMet(player, requirement)) {
                return false;
            }
        }
        return true;
    }

    public boolean isMet(Player player, Requirement requirement) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        return switch (requirement.type()) {
            case LEVEL -> data.getBaseLevel() >= requirement.amount();
            case CLASS_LEVEL -> data.getClassLevel() >= requirement.amount();
            case CLASS -> data.getSelectedClass() != null && data.getSelectedClass().id().equalsIgnoreCase(requirement.target());
            case COINS -> coinService.getCoins(player) >= requirement.amount();
            case STAT -> data.getStats().entrySet().stream()
                    .anyMatch(entry -> entry.getKey().name().equalsIgnoreCase(requirement.target()) && entry.getValue() >= requirement.amount());
            case SKILL_STAT -> data.getSkillStatLevel(requirement.target()) >= requirement.amount();
            case CLASS_STAT -> data.getClassStatLevel(requirement.target()) >= requirement.amount();
            case QUEST_COMPLETE -> data.hasCompletedQuest(requirement.target());
            case WORLD_VISIT -> data.hasVisitedWorld(requirement.target());
            case WORLD_CLEAR -> data.hasClearedWorld(requirement.target());
            case PROGRESSION_FLAG -> data.hasProgressionFlag(requirement.target());
            case ITEM, MOB_KILL, BOSS_KILL -> false;
        };
    }
}
