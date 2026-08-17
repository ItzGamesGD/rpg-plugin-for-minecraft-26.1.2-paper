package com.hyunseo.hyunseorpg.alchemy.abundance;

import java.util.UUID;

public record AbundanceActivityEvent(String eventId, UUID playerId, String activityId, Source source) {
    public enum Source { DIRECT_HARVEST, REGISTERED_DELIVERY, OTHER }
}
