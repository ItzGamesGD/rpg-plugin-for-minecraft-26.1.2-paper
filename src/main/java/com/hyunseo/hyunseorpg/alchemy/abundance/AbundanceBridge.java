package com.hyunseo.hyunseorpg.alchemy.abundance;

public interface AbundanceBridge {
    long points(String playerId);
    ExchangeResult exchange(String playerId, String essenceId, int amount);
    boolean isRegisteredActivity(String activityId);
    boolean isDuplicateEvent(String eventId);

    enum ExchangeResult { SUCCESS, PLAYER_NOT_FOUND, ACTIVITY_NOT_REGISTERED, DUPLICATE_EVENT,
        INSUFFICIENT_POINTS, ESSENCE_DISABLED, FAILED }
}
