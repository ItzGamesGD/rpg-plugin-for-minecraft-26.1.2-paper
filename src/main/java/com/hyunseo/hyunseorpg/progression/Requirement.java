package com.hyunseo.hyunseorpg.progression;

public record Requirement(
        RequirementType type,
        String target,
        long amount
) {
    public Requirement {
        target = target == null ? "" : target.trim().toLowerCase();
        amount = Math.max(0L, amount);
    }
}
