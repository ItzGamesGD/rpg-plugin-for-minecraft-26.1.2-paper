package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

/** Immutable, bounded tuning snapshot for a Gateway Boss encounter. */
public record GatewayBossConfig(
        double attackRange, double orbitRadius, double orbitSpeed, int maxOrbitWeapons,
        double ringPlaneSpeed,
        double weaponThrowSpeed, int weaponThrowTelegraphTicks,
        double maceDamage, double axeDamage, double hoeDamage,
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
                                     int actorCap, int actorLifetime,
                                     int maxDrivers, int driverLifetime, double phaseSpacing, double burstThreshold,
                                     int cooldown, int burstCooldown, int cleanupTimeout) {
        int safeActorCap = clamp(actorCap, 1, 24);
        double safeSpacing = finite(phaseSpacing, 12, 40, 24);
        return new GatewayBossConfig(Math.max(finite(range, 28, 72, 40), safeSpacing + 8), finite(radius, 1.4, 6, 3.4), finite(orbitSpeed, .01, .30, .075),
                orbitSlots(weapons, safeActorCap), finite(ringPlaneSpeed, .002, .08, .011), finite(throwSpeed, .12, .85, .48), clamp(throwTelegraph, 10, 60),
                finite(sword, 0, 30, 8), finite(axe, 0, 30, 7), finite(hoe, 0, 30, 6),
                safeActorCap, clamp(actorLifetime, 60, 1200), Math.max(safeActorCap, clamp(maxDrivers, 1, 24)), clamp(driverLifetime, 60, 1200),
                safeSpacing, finite(burstThreshold, .05, .90, .45), clamp(cooldown, 30, 400), clamp(burstCooldown, 20, 300),
                clamp(cleanupTimeout, 40, 600));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int orbitSlots(int requested, int actorCap) {
        // Five families include a presentation-only Shield ring. Round to full rings so four
        // eligible families always expose enough real orbital weapons for the executable cap.
        int minimum = (int) Math.ceil(actorCap * 5.0D / 4.0D);
        int slots = Math.max(minimum, clamp(requested, 5, 40));
        return ((slots + 4) / 5) * 5;
    }
    private static double finite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
