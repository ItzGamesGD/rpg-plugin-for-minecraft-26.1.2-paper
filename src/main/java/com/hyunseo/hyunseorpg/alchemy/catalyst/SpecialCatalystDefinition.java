package com.hyunseo.hyunseorpg.alchemy.catalyst;

import java.util.Objects;

public final class SpecialCatalystDefinition {
    private final String catalystId;
    private final boolean enabled;
    private final Kind kind;
    private final String materialId;
    private final int maxCount;
    private final double radius;
    private final int delayTicks;
    private final int lifetimeTicks;
    private final double maxDistance;
    private final double maxTotalDistance;
    private final double attenuation;

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind) {
        this(catalystId, enabled, kind, catalystId.toUpperCase(), 8, 8.0D, 10, 40, 16.0D, 0.5D);
    }

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind, String materialId,
                                     int maxCount, double radius, int delayTicks,
                                     int lifetimeTicks, double maxDistance) {
        this(catalystId, enabled, kind, materialId, maxCount, radius, delayTicks,
                lifetimeTicks, maxDistance, Math.max(maxDistance, maxDistance * maxCount), 0.5D);
    }

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind, String materialId,
                                     int maxCount, double radius, int delayTicks,
                                     int lifetimeTicks, double maxDistance, double attenuation) {
        this(catalystId, enabled, kind, materialId, maxCount, radius, delayTicks,
                lifetimeTicks, maxDistance, Math.max(maxDistance, maxDistance * maxCount), attenuation);
    }

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind, String materialId,
                                     int maxCount, double radius, int delayTicks,
                                     int lifetimeTicks, double maxDistance, double maxTotalDistance,
                                     double attenuation) {
        this.catalystId = Objects.requireNonNull(catalystId, "catalystId");
        this.enabled = enabled;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.materialId = materialId == null ? "" : materialId.trim().toUpperCase();
        this.maxCount = Math.max(1, maxCount);
        this.radius = Math.max(1.0D, radius);
        this.delayTicks = Math.max(1, delayTicks);
        this.lifetimeTicks = Math.max(1, lifetimeTicks);
        this.maxDistance = Math.max(1.0D, maxDistance);
        this.maxTotalDistance = Math.max(this.maxDistance, maxTotalDistance);
        this.attenuation = Math.max(0.05D, Math.min(1.0D, attenuation));
    }

    public String catalystId() { return catalystId; }
    public boolean enabled() { return enabled; }
    public Kind kind() { return kind; }
    public String materialId() { return materialId; }
    public int maxCount() { return maxCount; }
    public double radius() { return radius; }
    public int delayTicks() { return delayTicks; }
    public int lifetimeTicks() { return lifetimeTicks; }
    public double maxDistance() { return maxDistance; }
    public double maxTotalDistance() { return maxTotalDistance; }
    public double attenuation() { return attenuation; }

    public enum Kind { SCULK, ECHO, SLIME, WIND_CHARGE, FIREBALL }
}
