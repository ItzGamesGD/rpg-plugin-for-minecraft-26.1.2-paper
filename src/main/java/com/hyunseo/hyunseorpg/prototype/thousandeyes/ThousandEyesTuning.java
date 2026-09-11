package com.hyunseo.hyunseorpg.prototype.thousandeyes;

/** Prototype balance and presentation values; intentionally centralized for Astra playtesting. */
public final class ThousandEyesTuning {
    private ThousandEyesTuning() { }
    public static final int INNER_COUNT = 8, SATELLITE_COUNT = 9, OUTER_COUNT = 5;
    public static final float CENTRAL_EYE_SCALE = 3.0F, SATELLITE_SCALE = .62F;
    public static final double INNER_RADIUS = 2.05, SATELLITE_RADIUS = 3.0, OUTER_RADIUS = 4.25;
    public static final double W_NORMAL = .055, W_CHARGE = .018, W_RELEASE = .19;
    public static final double OUTER_W_NORMAL = -.032, OUTER_W_CHARGE = -.105, FRONT_OFFSET = 1.25;
    public static final int CENTRAL_CHARGE_TICKS = 50, CENTRAL_RELEASE_TICKS = 24, RETURN_TICKS = 16;
    public static final double CENTRAL_LASER_DAMAGE = 8, CENTRAL_LASER_RANGE = 22, RAY_HIT_RADIUS = .85;
    public static final int GATEWAY_COUNT = 8, GATEWAY_INTERVAL_TICKS = 10, GATEWAY_DELAY_TICKS = 20, GATEWAY_TELEGRAPH_TICKS = 10;
    public static final double GATEWAY_DAMAGE = 6, GATEWAY_RADIUS = 2.6;
    public static final int SCATTER_MOVE_TICKS = 18, SCATTER_FIRE_SPACING = 3, SCATTER_CHARGE_TICKS = 12;
    public static final double SCATTER_DAMAGE = 4, SCATTER_RANGE = 18;
    public static final int MARKER_INTERVAL_TICKS = 10, DASH_SEGMENT_TICKS = 10;
    public static final double HAZARD_DAMAGE = 2, HAZARD_RADIUS = 1.35, DASH_DAMAGE = 5, DASH_HIT_RADIUS = 1.15;
    public static final int HAZARD_CIRCLE_POINTS = 12, BURST_PARTICLES = 14, ORBIT_PARTICLE_INTERVAL = 5;
    public static final boolean USE_END_GATEWAY_DISPLAY = true;
}
