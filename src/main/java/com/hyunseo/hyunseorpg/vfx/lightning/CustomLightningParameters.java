package com.hyunseo.hyunseorpg.vfx.lightning;

/** Tunable, renderer-only values. None of these values participate in hit detection. */
public record CustomLightningParameters(double coreThickness, double glowThickness,
        double segmentLength, double pathJitter, double branchChance, int maxBranchDepth,
        double branchLength, long lifetimeTicks) {
    public CustomLightningParameters {
        coreThickness = Math.max(0.005, coreThickness);
        glowThickness = Math.max(coreThickness, glowThickness);
        segmentLength = Math.max(0.15, segmentLength);
        pathJitter = Math.max(0, pathJitter);
        branchChance = Math.max(0, Math.min(1, branchChance));
        maxBranchDepth = Math.max(0, Math.min(2, maxBranchDepth));
        branchLength = Math.max(0, branchLength);
        lifetimeTicks = Math.max(1, lifetimeTicks);
    }
}
