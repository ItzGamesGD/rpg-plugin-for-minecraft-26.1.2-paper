package com.hyunseo.hyunseorpg.skill;

public record SkillInputResult(
        boolean accepted,
        boolean cancelVanillaAction
) {
    public static SkillInputResult ignored() {
        return new SkillInputResult(false, false);
    }

    public static SkillInputResult accepted(boolean cancelVanillaAction) {
        return new SkillInputResult(true, cancelVanillaAction);
    }
}
