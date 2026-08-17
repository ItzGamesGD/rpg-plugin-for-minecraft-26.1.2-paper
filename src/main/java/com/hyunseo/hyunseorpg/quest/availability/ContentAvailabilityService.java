package com.hyunseo.hyunseorpg.quest.availability;

import org.bukkit.entity.Player;

import java.util.List;

/** Single query facade used by all automatic quest candidate generation. */
public final class ContentAvailabilityService {
    private final MonsterEligibilityService monsters;
    private final ItemObtainabilityService items;

    public ContentAvailabilityService(MonsterEligibilityService monsters, ItemObtainabilityService items) {
        this.monsters = monsters;
        this.items = items;
    }

    public MonsterEligibilityResult checkMonster(Player player, String monsterId, MonsterEligibilityContext context) {
        return monsters.checkCustom(player, monsterId, context);
    }

    public ItemObtainabilityResult checkItem(Player player, String itemId, ItemObtainabilityContext context) {
        return items.checkItem(player, itemId, context);
    }

    public List<QuestTargetCandidate> eligibleHuntTargets(Player player) {
        return monsters.eligibleHuntTargets(player);
    }

    public List<QuestTargetCandidate> eligibleItemTargets(Player player) {
        return items.eligibleItemTargets(player);
    }
}
