package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

/** One immutable geometry/range snapshot shared by boss phases and all Gateway RPGTest commands. */
record GatewayPhaseConfig(int gatewayCount, double minRadius, double maxRadius, double upperHeight,
                          double minimumSpacing, double phaseSpacing, int payloadCap,
                          double volumeMargin, double volumeMaxRange, int projectileLifetimeTicks,
                          int payloadInitialDelayTicks, int payloadRetryDelayTicks, int payloadEmissionIntervalTicks,
                          int payloadResolveGraceTicks, int gatewayRecoveryTicks, int sustainedVolumeDurationTicks,
                          int sonicBoomDurationTicks, int volumeDefaultTelegraphTicks, int volumeAdvancedTelegraphTicks,
                          int volumeDamageCadenceTicks, int crystalFuseTicks) {
    static GatewayPhaseConfig from(ConfigService config, GatewayBossConfig boss) {
        String p = "gateway-boss.gateway.";
        double min = bounded(config.getGatewayBossDouble(p + "radial-min", 16), 10, 40, 16);
        double max = Math.max(min + 2, bounded(config.getGatewayBossDouble(p + "radial-max", 30), 12, 48, 30));
        return new GatewayPhaseConfig(
                clamp(config.getGatewayBossInt(p + "count", 7), 4, 16), min, max,
                bounded(config.getGatewayBossDouble(p + "upper-height", 10), 4, 24, 10),
                bounded(config.getGatewayBossDouble(p + "minimum-spacing", 5), 3, 12, 5),
                boss.phaseSpacing(), clamp(config.getGatewayBossInt(p + "global-payload-cap", 16), 1, 48),
                bounded(config.getGatewayBossDouble("gateway-boss.payload.volume-margin", 6), 2, 24, 6),
                bounded(config.getGatewayBossDouble("gateway-boss.payload.volume-max-range", 64), 20, 96, 64),
                clamp(config.getGatewayBossInt("gateway-boss.payload.projectile-lifetime-ticks", 900), 200, 3600),
                clamp(config.getGatewayBossInt("gateway-boss.timing.payload-initial-delay-ticks", 16), 4, 240),
                clamp(config.getGatewayBossInt("gateway-boss.timing.payload-retry-delay-ticks", 3), 1, 120),
                clamp(config.getGatewayBossInt("gateway-boss.timing.payload-emission-interval-ticks", 7), 2, 240),
                clamp(config.getGatewayBossInt("gateway-boss.timing.payload-resolve-grace-ticks", 60), 10, 1200),
                clamp(config.getGatewayBossInt("gateway-boss.timing.gateway-recovery-ticks", 8), 1, 240),
                clamp(config.getGatewayBossInt("gateway-boss.timing.sustained-volume-duration-ticks", 50), 10, 1200),
                clamp(config.getGatewayBossInt("gateway-boss.timing.sonic-boom-duration-ticks", 20), 10, 600),
                clamp(config.getGatewayBossInt("gateway-boss.timing.volume-default-telegraph-ticks", 4), 1, 240),
                clamp(config.getGatewayBossInt("gateway-boss.timing.volume-advanced-telegraph-ticks", 12), 1, 240),
                clamp(config.getGatewayBossInt("gateway-boss.timing.volume-damage-cadence-ticks", 8), 1, 240),
                clamp(config.getGatewayBossInt("gateway-boss.timing.crystal-fuse-ticks", 18), 4, 240));
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double bounded(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
