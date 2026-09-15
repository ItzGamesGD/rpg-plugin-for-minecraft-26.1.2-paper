package com.hyunseo.hyunseorpg.progression;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.entity.Player;

import java.util.List;

public final class RequirementChecker {
    private final PlayerDataService playerDataService;

    public RequirementChecker(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
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
            case QUEST_COMPLETE -> data.hasCompletedQuest(requirement.target());
            case WORLD_VISIT -> data.hasVisitedWorld(requirement.target());
            case WORLD_CLEAR -> data.hasClearedWorld(requirement.target());
            case PROGRESSION_FLAG -> data.hasProgressionFlag(requirement.target());
            case ITEM, MOB_KILL, BOSS_KILL -> false;
        };
    }
}
