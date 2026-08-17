package com.hyunseo.hyunseorpg.quest.availability;

import java.util.Set;

public record ItemObtainabilityResult(
        boolean obtainable,
        AvailabilityReason reason,
        String itemId,
        String displayName,
        QuestTargetSource source,
        Set<AcquisitionSource> acquisitionSources,
        String detail
) {
    public static ItemObtainabilityResult allowed(String id, String displayName, QuestTargetSource source,
                                                   Set<AcquisitionSource> sources, String detail) {
        return new ItemObtainabilityResult(true, AvailabilityReason.ELIGIBLE, id, displayName, source,
                Set.copyOf(sources), detail);
    }

    public static ItemObtainabilityResult denied(AvailabilityReason reason, String id, String displayName,
                                                  QuestTargetSource source, Set<AcquisitionSource> sources,
                                                  String detail) {
        return new ItemObtainabilityResult(false, reason, id, displayName, source, Set.copyOf(sources), detail);
    }
}
