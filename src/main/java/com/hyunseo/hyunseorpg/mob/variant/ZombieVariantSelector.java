package com.hyunseo.hyunseorpg.mob.variant;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class ZombieVariantSelector {
    private final ConfigService configService;

    public ZombieVariantSelector(ConfigService configService) {
        this.configService = configService;
    }

    public boolean shouldConsider(EntityType entityType, CreatureSpawnEvent.SpawnReason reason, World world) {
        if (!configService.getMobsBoolean("zombie-variants.enabled", true)
                || entityType != EntityType.ZOMBIE
                || world == null
                || !isAllowedReason(reason)
                || !isAllowedWorld(world.getName())) {
            return false;
        }
        return true;
    }

    public ZombieVariant select() {
        double customChance = clamp(configService.getMobsDouble("zombie-variants.total-custom-chance", 15.0D), 0.0D, 100.0D);
        if (ThreadLocalRandom.current().nextDouble(100.0D) >= customChance) {
            return ZombieVariant.NORMAL;
        }

        List<WeightedVariant> candidates = new ArrayList<>();
        addIfEnabled(candidates, ZombieVariant.BOMB, "bomb");
        addIfEnabled(candidates, ZombieVariant.LEAP, "leap");
        double totalWeight = candidates.stream().mapToDouble(WeightedVariant::weight).sum();
        if (totalWeight <= 0.0D) {
            return ZombieVariant.NORMAL;
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (WeightedVariant candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0.0D) {
                return candidate.variant();
            }
        }
        return candidates.get(candidates.size() - 1).variant();
    }

    private void addIfEnabled(List<WeightedVariant> candidates, ZombieVariant variant, String path) {
        if (!configService.getMobsBoolean("zombie-variants.variants." + path + ".enabled", true)) {
            return;
        }
        double weight = Math.max(0.0D, configService.getMobsDouble("zombie-variants.variants." + path + ".weight", 50.0D));
        if (weight > 0.0D) {
            candidates.add(new WeightedVariant(variant, weight));
        }
    }

    private boolean isAllowedReason(CreatureSpawnEvent.SpawnReason reason) {
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL) {
            return configService.getMobsBoolean("zombie-variants.spawn-reasons.natural", true);
        }
        if (reason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) {
            return configService.getMobsBoolean("zombie-variants.spawn-reasons.reinforcements", false);
        }
        return false;
    }

    private boolean isAllowedWorld(String worldName) {
        List<String> whitelist = configService.getMobsStringList("zombie-variants.worlds.whitelist");
        List<String> blacklist = configService.getMobsStringList("zombie-variants.worlds.blacklist");
        String normalized = worldName.toLowerCase(Locale.ROOT);
        boolean whitelisted = whitelist.isEmpty() || whitelist.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
        boolean blacklisted = blacklist.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
        return whitelisted && !blacklisted;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record WeightedVariant(ZombieVariant variant, double weight) {
    }
}
