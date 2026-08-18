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
    private final double visualRadius;
    private final int visualColorRed;
    private final int visualColorGreen;
    private final int visualColorBlue;

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind) {
        this(catalystId, enabled, kind, catalystId.toUpperCase(), 8, 8.0D, 10, 40, 16.0D, 128.0D, 0.5D,
                8.0D, 255, 255, 255);
    }

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind, String materialId,
                                     int maxCount, double radius, int delayTicks,
                                     int lifetimeTicks, double maxDistance) {
        this(catalystId, enabled, kind, materialId, maxCount, radius, delayTicks,
                lifetimeTicks, maxDistance, Math.max(maxDistance, maxDistance * maxCount), 0.5D,
                radius, 255, 255, 255);
    }

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind, String materialId,
                                     int maxCount, double radius, int delayTicks,
                                     int lifetimeTicks, double maxDistance, double attenuation) {
        this(catalystId, enabled, kind, materialId, maxCount, radius, delayTicks,
                lifetimeTicks, maxDistance, Math.max(maxDistance, maxDistance * maxCount), attenuation,
                radius, 255, 255, 255);
    }

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind, String materialId,
                                     int maxCount, double radius, int delayTicks,
                                     int lifetimeTicks, double maxDistance, double maxTotalDistance,
                                     double attenuation) {
        this(catalystId, enabled, kind, materialId, maxCount, radius, delayTicks, lifetimeTicks,
                maxDistance, maxTotalDistance, attenuation, radius, 255, 255, 255);
    }

    public SpecialCatalystDefinition(String catalystId, boolean enabled, Kind kind, String materialId,
                                     int maxCount, double radius, int delayTicks,
                                     int lifetimeTicks, double maxDistance, double maxTotalDistance,
                                     double attenuation, double visualRadius,
                                     int visualColorRed, int visualColorGreen, int visualColorBlue) {
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
        this.visualRadius = Math.max(0.1D, visualRadius);
        this.visualColorRed = clampColor(visualColorRed);
        this.visualColorGreen = clampColor(visualColorGreen);
        this.visualColorBlue = clampColor(visualColorBlue);
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
    public double visualRadius() { return visualRadius; }
    public int visualColorRed() { return visualColorRed; }
    public int visualColorGreen() { return visualColorGreen; }
    public int visualColorBlue() { return visualColorBlue; }

    private static int clampColor(int value) { return Math.max(0, Math.min(255, value)); }

    public enum Kind { SCULK, ECHO, SLIME, WIND_CHARGE, FIREBALL }
}
