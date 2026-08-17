package com.hyunseo.hyunseorpg.economy;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class CoinService {
    private final PlayerDataService playerDataService;

    public CoinService(PlayerDataService playerDataService) {
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
    }

    public long getCoins(Player player) {
        return playerDataService.getOrLoad(player).getCoins();
    }

    public void addCoins(Player player, long amount) {
        if (amount <= 0L) {
            return;
        }

        PlayerRPGData data = playerDataService.getOrLoad(player);
        long currentCoins = data.getCoins();
        long nextCoins = Long.MAX_VALUE - currentCoins < amount ? Long.MAX_VALUE : currentCoins + amount;
        data.setCoins(nextCoins);
        playerDataService.savePlayer(player);
    }

    public boolean canAddCoins(Player player, long amount) {
        if (amount <= 0L) {
            return true;
        }
        return Long.MAX_VALUE - getCoins(player) >= amount;
    }

    public boolean takeCoins(Player player, long amount) {
        if (amount <= 0L) {
            return true;
        }

        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (data.getCoins() < amount) {
            return false;
        }

        data.setCoins(data.getCoins() - amount);
        playerDataService.savePlayer(player);
        return true;
    }

    public void setCoins(Player player, long amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setCoins(Math.max(0L, amount));
        playerDataService.savePlayer(player);
    }
}
