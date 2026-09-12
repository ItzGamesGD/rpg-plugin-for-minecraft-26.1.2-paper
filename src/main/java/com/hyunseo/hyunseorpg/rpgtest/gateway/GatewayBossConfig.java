package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

/** Immutable, bounded tuning snapshot for a Gateway Boss encounter. */
public record GatewayBossConfig(
        double attackRange, double orbitRadius, double orbitSpeed, int maxOrbitWeapons,
        double ringPlaneSpeed,
        double weaponThrowSpeed, int weaponThrowTelegraphTicks,
        double maceDamage, double axeDamage, double hoeDamage,
        int basicActorCap, int basicActorLifetimeTicks, int maxActiveDrivers, int driverLifetimeTicks,
        int maceMeleeCadenceTicks, int spearMeleeCadenceTicks, int axeMeleeCadenceTicks, int hoeMeleeCadenceTicks,
        int spearLungeLifetimeTicks, int tridentRetreatTicks, int tridentRetreatMotionTicks,
        double phaseSpacing, double burstHealthThreshold, double gatewaySpecialChance, int attackCooldownTicks, int burstCooldownTicks,
        int cleanupTimeoutTicks) {

    public static GatewayBossConfig from(ConfigService config) {
        String p = "gateway-boss.";
        return bounded(
                config.getGatewayBossDouble(p + "attack-range", 28.0),
                config.getGatewayBossDouble(p + "orbit.radius", 5.5),
                config.getGatewayBossDouble(p + "orbit.speed", .075),
                config.getGatewayBossInt(p + "orbit.max-weapons", 10),
                config.getGatewayBossDouble(p + "orbit.ring-plane-speed", .011),
                config.getGatewayBossDouble(p + "basic.weapon-throw-speed", .42),
                config.getGatewayBossInt(p + "basic.detach-telegraph-ticks", 16),
                config.getGatewayBossDouble(p + "basic.mace-melee-damage", 8.0),
                config.getGatewayBossDouble(p + "basic.axe-melee-damage", 7.0),
                config.getGatewayBossDouble(p + "basic.hoe-melee-damage", 6.0),
                config.getGatewayBossInt(p + "basic.actor-total-cap", 14),
                config.getGatewayBossInt(p + "basic.actor-lifetime-ticks", 100),
                config.getGatewayBossInt(p + "internal-ai-driver.max-active", 6),
                config.getGatewayBossInt(p + "internal-ai-driver.lifetime-ticks", 240),
                config.getGatewayBossInt(p + "basic.mace-melee-cadence-ticks", 30),
                config.getGatewayBossInt(p + "basic.spear-melee-cadence-ticks", 18),
                config.getGatewayBossInt(p + "basic.axe-melee-cadence-ticks", 24),
                config.getGatewayBossInt(p + "basic.hoe-melee-cadence-ticks", 16),
                config.getGatewayBossInt(p + "basic.spear-lunge-lifetime-ticks", 36),
                config.getGatewayBossInt(p + "basic.trident-retreat-ticks", 28),
                config.getGatewayBossInt(p + "basic.trident-retreat-motion-ticks", 14),
                config.getGatewayBossDouble(p + "gateway.phase-spacing", 16.0D),
                config.getGatewayBossDouble(p + "phase-health-thresholds.burst-ratio", .45),
                config.getGatewayBossDouble(p + "gateway.special-chance", .20D),
                config.getGatewayBossInt(p + "attack-cooldown-ticks", 80),
                config.getGatewayBossInt(p + "burst-cooldown-ticks", 48),
                config.getGatewayBossInt(p + "gateway.cleanup-timeout-ticks", 100));
    }

    static GatewayBossConfig bounded(double range, double radius, double orbitSpeed, int weapons, double ringPlaneSpeed,
                                     double throwSpeed, int throwTelegraph, double sword, double axe, double hoe,
                                     int actorCap, int actorLifetime,
                                     int maxDrivers, int driverLifetime, int maceCadence, int spearCadence, int axeCadence, int hoeCadence,
                                     int lungeLifetime, int tridentRetreat, int tridentMotion, double phaseSpacing, double burstThreshold, double specialChance,
                                     int cooldown, int burstCooldown, int cleanupTimeout) {
        int safeActorCap = clamp(actorCap, 1, 24);
        double safeSpacing = finite(phaseSpacing, 12, 40, 24);
        return new GatewayBossConfig(Math.max(finite(range, 28, 72, 40), safeSpacing + 8), finite(radius, 1.4, 6, 3.4), finite(orbitSpeed, .01, .30, .075),
                orbitSlots(weapons, safeActorCap), finite(ringPlaneSpeed, .002, .08, .011), finite(throwSpeed, .12, .85, .48), clamp(throwTelegraph, 10, 60),
                finite(sword, 0, 30, 8), finite(axe, 0, 30, 7), finite(hoe, 0, 30, 6),
                safeActorCap, clamp(actorLifetime, 60, 1200), Math.max(safeActorCap, clamp(maxDrivers, 1, 24)), clamp(driverLifetime, 60, 1200),
                clamp(maceCadence, 8, 240), clamp(spearCadence, 8, 240), clamp(axeCadence, 8, 240), clamp(hoeCadence, 8, 240),
                clamp(lungeLifetime, 20, 240), clamp(tridentRetreat, 20, 240), clamp(tridentMotion, 8, 120),
                safeSpacing, finite(burstThreshold, .05, .90, .45), finite(specialChance, .02, .80, .20), clamp(cooldown, 30, 400), clamp(burstCooldown, 20, 300),
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
