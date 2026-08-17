package com.hyunseo.hyunseorpg.farming;

import java.util.Objects;

/** Runtime state for one canonical crop position. */
public final class CropInstance {
    private final String cropId;
    private final CropPosition position;
    private final int dataVersion;
    private final long plantedAt;
    private int stage;
    private long nextGrowthAt;

    public CropInstance(String cropId, CropPosition position, int stage,
                        long plantedAt, long nextGrowthAt, int dataVersion) {
        this.cropId = normalize(cropId);
        this.position = Objects.requireNonNull(position, "position");
        this.stage = Math.max(0, stage);
        this.plantedAt = Math.max(0L, plantedAt);
        this.nextGrowthAt = Math.max(0L, nextGrowthAt);
        this.dataVersion = Math.max(1, dataVersion);
        if (this.cropId.isBlank()) throw new IllegalArgumentException("cropId must not be blank");
    }

    public String cropId() { return cropId; }
    public CropPosition position() { return position; }
    public int stage() { return stage; }
    public long plantedAt() { return plantedAt; }
    public long nextGrowthAt() { return nextGrowthAt; }
    public int dataVersion() { return dataVersion; }

    public void advanceTo(int nextStage, long nextGrowthAt) {
        this.stage = Math.max(this.stage, nextStage);
        this.nextGrowthAt = Math.max(0L, nextGrowthAt);
    }

    public void applyGrowth(int nextStage, long nextGrowthAt) {
        this.stage = Math.max(0, nextStage);
        this.nextGrowthAt = Math.max(0L, nextGrowthAt);
    }

    public void stopGrowth() {
        this.nextGrowthAt = Long.MAX_VALUE;
    }

    public boolean isDue(long now) {
        return nextGrowthAt != Long.MAX_VALUE && nextGrowthAt <= now;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
