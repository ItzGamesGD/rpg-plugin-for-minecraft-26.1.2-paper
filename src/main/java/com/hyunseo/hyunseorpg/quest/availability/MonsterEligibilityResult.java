package com.hyunseo.hyunseorpg.quest.availability;

public record MonsterEligibilityResult(
        boolean eligible,
        AvailabilityReason reason,
        String monsterId,
        String displayName,
        QuestTargetSource source,
        String detail
) {
    public static MonsterEligibilityResult allowed(String id, String displayName, QuestTargetSource source, String detail) {
        return new MonsterEligibilityResult(true, AvailabilityReason.ELIGIBLE, id, displayName, source, detail);
    }

    public static MonsterEligibilityResult denied(AvailabilityReason reason, String id, String displayName,
                                                   QuestTargetSource source, String detail) {
        return new MonsterEligibilityResult(false, reason, id, displayName, source, detail);
    }
}
