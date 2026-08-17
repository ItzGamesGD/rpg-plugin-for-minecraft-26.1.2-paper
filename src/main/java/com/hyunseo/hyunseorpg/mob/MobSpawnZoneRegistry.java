package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class MobSpawnZoneRegistry {
    private final ConfigService configService;
    private final List<MobSpawnZone> zones = new ArrayList<>();

    public MobSpawnZoneRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        zones.clear();
        Set<String> zoneIds = configService.getMobsKeys("forced-rpg-zones");
        for (String zoneId : zoneIds) {
            if (!configService.getMobsBoolean("forced-rpg-zones." + zoneId + ".enabled", false)) {
                continue;
            }
            zones.add(parseZone(zoneId));
        }
    }

    public Optional<MobSpawnZone> findZone(Location location) {
        return zones.stream()
                .filter(zone -> zone.contains(location))
                .findFirst();
    }

    public List<MobSpawnZone> getZones() {
        return List.copyOf(zones);
    }

    private MobSpawnZone parseZone(String zoneId) {
        String path = "forced-rpg-zones." + zoneId + ".";
        return new MobSpawnZone(
                zoneId,
                configService.getMobsString(path + "world", "world"),
                configService.getMobsInt(path + "min.x", 0),
                configService.getMobsInt(path + "min.y", -64),
                configService.getMobsInt(path + "min.z", 0),
                configService.getMobsInt(path + "max.x", 0),
                configService.getMobsInt(path + "max.y", 320),
                configService.getMobsInt(path + "max.z", 0),
                configService.getMobsInt(path + "mob-level", 1),
                configService.getMobsString(path + "mob-id-prefix", zoneId),
                configService.getMobsStringList(path + "tags")
        );
    }
}
