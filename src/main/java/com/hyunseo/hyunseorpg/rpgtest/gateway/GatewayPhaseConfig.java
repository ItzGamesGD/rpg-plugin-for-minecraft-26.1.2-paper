package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

/** One immutable geometry/range snapshot shared by boss phases and all Gateway RPGTest commands. */
record GatewayPhaseConfig(int gatewayCount, double minRadius, double maxRadius, double upperHeight,
                          double minimumSpacing, double phaseSpacing, int payloadCap,
                          double volumeMargin, double volumeMaxRange, int projectileLifetimeTicks) {
    static GatewayPhaseConfig from(ConfigService config, GatewayBossConfig boss) {
        String p = "gateway-boss.gateway.";
        double min = bounded(config.getBossesDouble(p + "radial-min", 16), 10, 40, 16);
        double max = Math.max(min + 2, bounded(config.getBossesDouble(p + "radial-max", 30), 12, 48, 30));
        return new GatewayPhaseConfig(
                clamp(config.getBossesInt(p + "count", 7), 4, 16), min, max,
                bounded(config.getBossesDouble(p + "upper-height", 10), 4, 24, 10),
                bounded(config.getBossesDouble(p + "minimum-spacing", 5), 3, 12, 5),
                boss.phaseSpacing(), clamp(config.getBossesInt(p + "global-payload-cap", 16), 1, 48),
                bounded(config.getBossesDouble("gateway-boss.payload.volume-margin", 6), 2, 24, 6),
                bounded(config.getBossesDouble("gateway-boss.payload.volume-max-range", 64), 20, 96, 64),
                clamp(config.getBossesInt("gateway-boss.payload.projectile-lifetime-ticks", 900), 200, 1800));
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double bounded(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
