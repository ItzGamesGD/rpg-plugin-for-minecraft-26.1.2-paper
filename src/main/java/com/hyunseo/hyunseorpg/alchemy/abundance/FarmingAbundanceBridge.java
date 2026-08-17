package com.hyunseo.hyunseorpg.alchemy.abundance;

import com.hyunseo.hyunseorpg.farming.AbundancePointService;
import com.hyunseo.hyunseorpg.farming.FarmingEssenceService;
import com.hyunseo.hyunseorpg.farming.FarmingProfileService;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Farming-owned boundary. It accepts only registered, non-duplicated activity events. */
public final class FarmingAbundanceBridge implements AbundanceBridge {
    private final FarmingProfileService profiles;
    private final AbundancePointService points;
    private final FarmingEssenceService essence;
    private final EssenceRegistry essences;
    private final Set<String> consumedEvents = ConcurrentHashMap.newKeySet();
    private final Set<String> activities = Set.of("direct_harvest", "registered_delivery", "other");
    public FarmingAbundanceBridge(FarmingProfileService profiles, AbundancePointService points,
                                  FarmingEssenceService essence, EssenceRegistry essences) {
        this.profiles = profiles; this.points = points; this.essence = essence; this.essences = essences;
    }
    @Override public long points(String playerId) { UUID id = parse(playerId); return id == null ? 0L : points.getPoints(id); }
    @Override public ExchangeResult exchange(String playerId, String essenceId, int amount) {
        UUID id = parse(playerId);
        if (id == null) return ExchangeResult.PLAYER_NOT_FOUND;
        if (amount < 1) return ExchangeResult.FAILED;
        EssenceDefinition def = essences.find(essenceId).orElse(null);
        if (def == null || !def.enabled() || !essence.enabled()) return ExchangeResult.ESSENCE_DISABLED;
        long required;
        try { required = Math.multiplyExact(essence.requiredAbundancePoints(), amount); }
        catch (ArithmeticException overflow) { return ExchangeResult.FAILED; }
        return points.spend(id, required) ? ExchangeResult.SUCCESS : ExchangeResult.INSUFFICIENT_POINTS;
    }
    @Override public boolean isRegisteredActivity(String activityId) { return activityId != null && activities.contains(activityId.trim().toLowerCase()); }
    @Override public boolean isDuplicateEvent(String eventId) { return eventId == null || !consumedEvents.add(eventId); }
    public boolean accept(AbundanceActivityEvent event) {
        return event != null && event.playerId() != null && isRegisteredActivity(event.activityId())
                && event.source() != AbundanceActivityEvent.Source.OTHER && !isDuplicateEvent(event.eventId());
    }
    private UUID parse(String value) { try { return value == null ? null : UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; } }
}
