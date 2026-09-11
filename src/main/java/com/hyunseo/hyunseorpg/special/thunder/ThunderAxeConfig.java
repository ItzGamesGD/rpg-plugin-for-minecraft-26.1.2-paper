package com.hyunseo.hyunseorpg.special.thunder;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

public record ThunderAxeConfig(int hitThreshold, double strikeDamage, double chainRadius,
        int chainTargets, double chainDamage, double chainFalloff, int chargeTicks,
        int strikeCooldownTicks, double waveDamage, int waveIntervalTicks,
        double firstDistance, double distanceStep, double fanAngleDegrees, double hitRadius,
        boolean repeatAcrossWaves, int burstCooldownTicks, double burstRadius, double burstDamage,
        int burstTargetsPerWave, int burstWaveIntervalTicks, int burstMaxTargets, int burstMaxWaves,
        double coreThickness, double glowThickness, double segmentLength, double pathJitter,
        double branchChance, int maxBranchDepth, double branchLength, long lightningLifetimeTicks,
        int fanWaveCount, int fanDirectionCount, double verticalLightningHeight) {
    public static ThunderAxeConfig from(ConfigService config) {
        String p = "special-equipment.items.thunder_gods_axe.abilities.";
        String v = "special-equipment.items.thunder_gods_axe.vfx.custom-lightning.";
        return new ThunderAxeConfig(
                config.getSpecialEquipmentInt(p + "lightning-strike.hit-threshold", 5),
                config.getSpecialEquipmentDouble(p + "lightning-strike.damage", 4),
                config.getSpecialEquipmentDouble(p + "chain-lightning.radius", 5),
                config.getSpecialEquipmentInt(p + "chain-lightning.maximum-targets", 4),
                config.getSpecialEquipmentDouble(p + "chain-lightning.damage", 3),
                config.getSpecialEquipmentDouble(p + "chain-lightning.damage-falloff", .75),
                config.getSpecialEquipmentInt(p + "thunder-god-strike.full-charge-ticks", 20),
                config.getSpecialEquipmentInt(p + "thunder-god-strike.cooldown-ticks", 240),
                config.getSpecialEquipmentDouble(p + "thunder-god-strike.damage", 6),
                config.getSpecialEquipmentInt(p + "thunder-god-strike.wave-interval-ticks", 4),
                config.getSpecialEquipmentDouble(p + "thunder-god-strike.first-distance", 3),
                config.getSpecialEquipmentDouble(p + "thunder-god-strike.distance-step", 2),
                config.getSpecialEquipmentDouble(p + "thunder-god-strike.fan-angle-degrees", 50),
                config.getSpecialEquipmentDouble(p + "thunder-god-strike.hit-radius", 1.5),
                config.getSpecialEquipmentBoolean(p + "thunder-god-strike.allow-repeat-across-waves", false),
                config.getSpecialEquipmentInt(p + "heavenly-thunder-burst.cooldown-ticks", 400),
                config.getSpecialEquipmentDouble(p + "heavenly-thunder-burst.radius", 12),
                config.getSpecialEquipmentDouble(p + "heavenly-thunder-burst.damage", 5),
                config.getSpecialEquipmentInt(p + "heavenly-thunder-burst.targets-per-wave", 4),
                config.getSpecialEquipmentInt(p + "heavenly-thunder-burst.wave-interval-ticks", 5),
                config.getSpecialEquipmentInt(p + "heavenly-thunder-burst.maximum-targets", 16),
                config.getSpecialEquipmentInt(p + "heavenly-thunder-burst.maximum-waves", 4),
                config.getSpecialEquipmentDouble(v + "core-thickness", .025),
                config.getSpecialEquipmentDouble(v + "glow-thickness", .065),
                config.getSpecialEquipmentDouble(v + "segment-length", .7),
                config.getSpecialEquipmentDouble(v + "path-jitter", .16),
                config.getSpecialEquipmentDouble(v + "branch-chance", .08),
                config.getSpecialEquipmentInt(v + "max-branch-depth", 2),
                config.getSpecialEquipmentDouble(v + "branch-length", .7),
                config.getSpecialEquipmentInt(v + "lifetime-ticks", 5),
                config.getSpecialEquipmentInt(p + "thunder-god-strike.wave-count", 5),
                config.getSpecialEquipmentInt(p + "thunder-god-strike.direction-count", 5),
                config.getSpecialEquipmentDouble(p + "thunder-god-strike.vertical-lightning-height", 5.5));
    }
}
