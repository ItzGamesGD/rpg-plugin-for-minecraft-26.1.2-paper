package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

/** Immutable, bounded tuning snapshot for a Gateway Boss encounter. */
public record GatewayBossConfig(
        double attackRange, double orbitRadius, double orbitSpeed, int maxOrbitWeapons,
        double ringPlaneSpeed,
        double weaponThrowSpeed, int weaponThrowTelegraphTicks,
        double swordThrowDamage, double axeSpinDamage, double hoeSweepDamage,
        int parryTelegraphTicks, double parryReturnSpeed, int parryCooldownTicks,
        int basicActorCap, int basicActorLifetimeTicks, int maxActiveDrivers, int driverLifetimeTicks,
        double phaseSpacing, double burstHealthThreshold, int attackCooldownTicks, int burstCooldownTicks,
        int cleanupTimeoutTicks) {

    public static GatewayBossConfig from(ConfigService config) {
        String p = "gateway-boss.";
        return bounded(
                config.getBossesDouble(p + "attack-range", 28.0),
                config.getBossesDouble(p + "orbit.radius", 5.5),
                config.getBossesDouble(p + "orbit.speed", .075),
                config.getBossesInt(p + "orbit.max-weapons", 10),
                config.getBossesDouble(p + "orbit.ring-plane-speed", .011),
                config.getBossesDouble(p + "basic.weapon-throw-speed", .42),
                config.getBossesInt(p + "basic.detach-telegraph-ticks", 16),
                config.getBossesDouble(p + "basic.mace-melee-damage", 8.0),
                config.getBossesDouble(p + "basic.axe-melee-damage", 7.0),
                config.getBossesDouble(p + "basic.hoe-melee-damage", 6.0),
                config.getBossesInt(p + "reflection.parry-telegraph-ticks", 22),
                config.getBossesDouble(p + "reflection.parry-return-speed", .42),
                config.getBossesInt(p + "reflection.parry-cooldown-ticks", 100),
                config.getBossesInt(p + "basic.actor-total-cap", 14),
                config.getBossesInt(p + "basic.actor-lifetime-ticks", 100),
                config.getBossesInt(p + "internal-ai-driver.max-active", 6),
                config.getBossesInt(p + "internal-ai-driver.lifetime-ticks", 240),
                config.getBossesDouble(p + "gateway.phase-spacing", 16.0D),
                config.getBossesDouble(p + "phase-health-thresholds.burst-ratio", .45),
                config.getBossesInt(p + "attack-cooldown-ticks", 80),
                config.getBossesInt(p + "burst-cooldown-ticks", 48),
                config.getBossesInt(p + "gateway.cleanup-timeout-ticks", 100));
    }

    static GatewayBossConfig bounded(double range, double radius, double orbitSpeed, int weapons, double ringPlaneSpeed,
                                     double throwSpeed, int throwTelegraph, double sword, double axe, double hoe,
                                     int parryTelegraph, double parrySpeed, int parryCooldown, int actorCap, int actorLifetime,
                                     int maxDrivers, int driverLifetime, double phaseSpacing, double burstThreshold,
                                     int cooldown, int burstCooldown, int cleanupTimeout) {
        return new GatewayBossConfig(finite(range, 20, 48, 28), finite(radius, 2, 12, 5.5), finite(orbitSpeed, .01, .30, .075),
                clamp(weapons, 5, 20), finite(ringPlaneSpeed, .002, .08, .011), finite(throwSpeed, .12, .85, .48), clamp(throwTelegraph, 10, 60),
                finite(sword, 0, 30, 8), finite(axe, 0, 30, 7), finite(hoe, 0, 30, 6),
                clamp(parryTelegraph, 15, 60), finite(parrySpeed, .12, .75, .42), clamp(parryCooldown, 40, 600),
                clamp(actorCap, 1, 32), clamp(actorLifetime, 40, 1200), clamp(maxDrivers, 1, 16), clamp(driverLifetime, 40, 1200),
                finite(phaseSpacing, 8, 32, 16), finite(burstThreshold, .05, .90, .45), clamp(cooldown, 30, 400), clamp(burstCooldown, 20, 300),
                clamp(cleanupTimeout, 40, 600));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double finite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
