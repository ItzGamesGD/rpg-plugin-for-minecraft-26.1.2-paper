package com.hyunseo.hyunseorpg.exp;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** Maintains the retained RPG level without awarding legacy character-sheet points. */
public final class LevelService {
    private final PlayerDataService playerDataService;
    private final ExpTable expTable;
    public LevelService(PlayerDataService playerDataService, ExpTable expTable) { this.playerDataService = playerDataService; this.expTable = expTable; }
    public LevelUpResult addBaseExp(Player player, long amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        int old = data.getBaseLevel(); int gained = 0;
        if (amount > 0) data.setBaseExp(data.getBaseExp() + amount);
        while (data.getBaseLevel() < expTable.getMaxBaseLevel()) {
            long required = expTable.getRequiredExpForNextBaseLevel(data.getBaseLevel());
            if (data.getBaseExp() < required) break;
            data.setBaseExp(data.getBaseExp() - required); data.setBaseLevel(data.getBaseLevel() + 1); gained++;
        }
        if (data.getBaseLevel() >= expTable.getMaxBaseLevel()) data.setBaseExp(0L);
        playerDataService.savePlayer(player);
        LevelUpResult result = new LevelUpResult(old, data.getBaseLevel(), gained, 0, data.getBaseExp(), getRequiredExpForNextLevel(data));
        if (result.leveledUp()) player.sendMessage(Component.text("RPG 레벨이 Lv." + result.newLevel() + "이 되었습니다.", NamedTextColor.GREEN));
        return result;
    }
    public void setBaseLevel(Player player, int level) { PlayerRPGData d=playerDataService.getOrLoad(player); d.setBaseLevel(Math.max(1,Math.min(level,expTable.getMaxBaseLevel()))); d.setBaseExp(0); playerDataService.savePlayer(player); }
    public void setBaseExp(Player player, long exp) { PlayerRPGData d=playerDataService.getOrLoad(player); d.setBaseExp(Math.max(0,exp)); playerDataService.savePlayer(player); }
    public long getRequiredExpForNextLevel(PlayerRPGData data) { return data.getBaseLevel()>=expTable.getMaxBaseLevel()?0:expTable.getRequiredExpForNextBaseLevel(data.getBaseLevel()); }
    public int getMaxBaseLevel() { return expTable.getMaxBaseLevel(); }
}
