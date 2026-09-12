package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

/** Immutable, bounded tuning snapshot for a Gateway Boss encounter. */
public record GatewayBossConfig(
        double attackRange, double orbitRadius, double orbitSpeed, int maxOrbitWeapons,
        double weaponThrowSpeed, int weaponThrowTelegraphTicks,
        double swordThrowDamage, double axeSpinDamage, double hoeSweepDamage,
        int parryTelegraphTicks, double parryReturnSpeed, int parryCooldownTicks,
        int vexSummonCount, int maxActiveSummons, int summonLifetimeTicks,
        double burstHealthThreshold, int attackCooldownTicks, int burstCooldownTicks,
        int cleanupTimeoutTicks) {

    public static GatewayBossConfig from(ConfigService config) {
        String p = "gateway-boss.";
        return bounded(
                config.getBossesDouble(p + "attack-range", 28.0),
                config.getBossesDouble(p + "orbit-radius", 5.5),
                config.getBossesDouble(p + "orbit-speed", .075),
                config.getBossesInt(p + "max-orbit-weapons", 6),
                config.getBossesDouble(p + "weapon-throw-speed", .48),
                config.getBossesInt(p + "weapon-throw-telegraph-ticks", 16),
                config.getBossesDouble(p + "sword-throw-damage", 8.0),
                config.getBossesDouble(p + "axe-spin-damage", 7.0),
                config.getBossesDouble(p + "hoe-sweep-damage", 6.0),
                config.getBossesInt(p + "parry-telegraph-ticks", 22),
                config.getBossesDouble(p + "parry-return-speed", .42),
                config.getBossesInt(p + "parry-cooldown-ticks", 100),
                config.getBossesInt(p + "vex-summon-count", 2),
                config.getBossesInt(p + "max-active-summons", 6),
                config.getBossesInt(p + "summon-lifetime-ticks", 240),
                config.getBossesDouble(p + "phase-health-thresholds.burst-ratio", .45),
                config.getBossesInt(p + "attack-cooldown-ticks", 80),
                config.getBossesInt(p + "burst-cooldown-ticks", 48),
                config.getBossesInt(p + "cleanup-timeout-ticks", 100));
    }

    static GatewayBossConfig bounded(double range, double radius, double orbitSpeed, int weapons,
                                     double throwSpeed, int throwTelegraph, double sword, double axe, double hoe,
                                     int parryTelegraph, double parrySpeed, int parryCooldown, int vexCount,
                                     int maxSummons, int summonLifetime, double burstThreshold,
                                     int cooldown, int burstCooldown, int cleanupTimeout) {
        return new GatewayBossConfig(finite(range, 20, 48, 28), finite(radius, 2, 12, 5.5), finite(orbitSpeed, .01, .30, .075),
                clamp(weapons, 3, 12), finite(throwSpeed, .12, .85, .48), clamp(throwTelegraph, 10, 60),
                finite(sword, 0, 30, 8), finite(axe, 0, 30, 7), finite(hoe, 0, 30, 6),
                clamp(parryTelegraph, 15, 60), finite(parrySpeed, .12, .75, .42), clamp(parryCooldown, 40, 600),
                clamp(vexCount, 1, 6), clamp(maxSummons, 1, 16), clamp(summonLifetime, 40, 1200),
                finite(burstThreshold, .05, .90, .45), clamp(cooldown, 30, 400), clamp(burstCooldown, 20, 300),
                clamp(cleanupTimeout, 40, 600));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double finite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
