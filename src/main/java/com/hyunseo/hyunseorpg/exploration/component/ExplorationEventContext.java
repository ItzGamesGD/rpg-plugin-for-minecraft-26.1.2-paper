package com.hyunseo.hyunseorpg.exploration.component;

import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime;
import com.hyunseo.hyunseorpg.exploration.runtime.TeleportExemptionService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public record ExplorationEventContext(
        JavaPlugin plugin,
        StructureRecord record,
        ExplorationRuntime runtime,
        ExplorationPorts ports,
        TeleportExemptionService teleportExemptions,
        long currentTick
) {
    public Optional<World> world() { return Optional.ofNullable(Bukkit.getWorld(record.worldId())); }
    public Optional<Location> anchorLocation() {
        return world().map(world -> new Location(world, record.anchor().x(), record.anchor().y(), record.anchor().z()));
    }
}
