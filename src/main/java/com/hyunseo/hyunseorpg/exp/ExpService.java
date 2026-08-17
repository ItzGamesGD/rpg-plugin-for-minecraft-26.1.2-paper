package com.hyunseo.hyunseorpg.exp;

import org.bukkit.entity.Player;

public final class ExpService {
    private final LevelService levelService;

    public ExpService(LevelService levelService) {
        this.levelService = levelService;
    }

    public LevelUpResult giveBaseExp(Player player, long amount) {
        return levelService.addBaseExp(player, amount);
    }

    public ClassLevelUpResult giveClassExp(Player player, long amount) {
        return levelService.addClassExp(player, amount);
    }
}
