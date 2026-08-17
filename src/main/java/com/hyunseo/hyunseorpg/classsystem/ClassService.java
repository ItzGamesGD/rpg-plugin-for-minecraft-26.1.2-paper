package com.hyunseo.hyunseorpg.classsystem;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.mana.ManaService;
import org.bukkit.entity.Player;

public final class ClassService {
    private final PlayerDataService playerDataService;
    private final ClassWeaponService classWeaponService;
    private final ManaService manaService;

    public ClassService(PlayerDataService playerDataService, ClassWeaponService classWeaponService, ManaService manaService) {
        this.playerDataService = playerDataService;
        this.classWeaponService = classWeaponService;
        this.manaService = manaService;
    }

    public ClassSelectionResult selectClass(Player player, RPGClass rpgClass) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        RPGClass currentClass = data.getSelectedClass();
        if (currentClass != null) {
            return ClassSelectionResult.alreadySelected(currentClass);
        }

        data.setSelectedClass(rpgClass);
        manaService.fillToMax(player);
        playerDataService.savePlayer(player);
        return ClassSelectionResult.selected(rpgClass);
    }

    public RPGClass getSelectedClass(Player player) {
        return playerDataService.getOrLoad(player).getSelectedClass();
    }

    public RPGClass resetClass(Player player) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        RPGClass previousClass = data.getSelectedClass();
        if (previousClass == null) {
            return null;
        }

        data.setSelectedClass(null);
        manaService.clearMana(player);
        playerDataService.savePlayer(player);
        return previousClass;
    }
}
