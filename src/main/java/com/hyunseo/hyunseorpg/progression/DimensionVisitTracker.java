package com.hyunseo.hyunseorpg.progression;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** Records only the three vanilla dimensions; it never blocks world entry. */
public final class DimensionVisitTracker implements Listener {
    private final PlayerDataService playerDataService;

    public DimensionVisitTracker(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        record(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        record(event.getPlayer(), event.getPlayer().getWorld());
    }

    private void record(org.bukkit.entity.Player player, World world) {
        String dimension = dimensionId(world);
        if (dimension == null) return;
        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (data.hasVisitedWorld(dimension)) return;
        data.addVisitedWorld(dimension);
        data.addProgressionFlag("dimension_visit_" + dimension.toLowerCase(java.util.Locale.ROOT));
        playerDataService.savePlayer(player);
    }

    private String dimensionId(World world) {
        if (world == null) return null;
        return switch (world.getEnvironment()) {
            case NORMAL -> "OVERWORLD";
            case NETHER -> "NETHER";
            case THE_END -> "THE_END";
            default -> null;
        };
    }
}
