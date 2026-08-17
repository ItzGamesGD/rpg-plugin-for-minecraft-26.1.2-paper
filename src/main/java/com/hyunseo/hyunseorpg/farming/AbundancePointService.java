package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/** Persistence-aware access boundary for the non-item abundance currency. */
public final class AbundancePointService {
    private final PlayerDataService playerData;
    private final Set<String> registeredActivities = ConcurrentHashMap.newKeySet();

    public AbundancePointService(PlayerDataService playerData) {
        this.playerData = Objects.requireNonNull(playerData, "playerData");
    }

    /** Registers a real farming activity without assigning it a balance value. */
    public boolean registerFarmingActivity(String activityId) {
        String id = normalizeActivityId(activityId);
        return !id.isBlank() && registeredActivities.add(id);
    }

    public boolean unregisterFarmingActivity(String activityId) {
        String id = normalizeActivityId(activityId);
        return !id.isBlank() && registeredActivities.remove(id);
    }

    /**
     * Common reward boundary for future registered farming activities.
     * Unregistered IDs cannot grant points, preventing accidental rewards from
     * sales, processing, brewing, or arbitrary event names.
     */
    public boolean addRegisteredActivityPoints(UUID playerId, String activityId, long amount) {
        String id = normalizeActivityId(activityId);
        if (id.isBlank() || !registeredActivities.contains(id)) return false;
        return addPoints(playerId, amount, Source.FARMING_ACTIVITY);
    }

    public boolean isFarmingActivityRegistered(String activityId) {
        String id = normalizeActivityId(activityId);
        return !id.isBlank() && registeredActivities.contains(id);
    }

    public long getPoints(UUID playerId) {
        return playerId == null ? 0L : playerData.getOrLoad(playerId).getFarmingAbundancePoints();
    }

    public boolean addPoints(UUID playerId, long amount, Source source) {
        if (playerId == null || amount < 0L || source == null) return false;
        if (amount == 0L) return true;
        PlayerRPGData data = playerData.getOrLoad(playerId);
        long previous = data.getFarmingAbundancePoints();
        applyTo(data, amount, source);
        if (playerData.savePlayerNow(playerId)) return true;
        data.setFarmingAbundancePoints(previous);
        return false;
    }

    public boolean canSpend(UUID playerId, long amount) {
        return playerId != null && amount >= 0L && getPoints(playerId) >= amount;
    }

    public boolean spend(UUID playerId, long amount) {
        if (!canSpend(playerId, amount)) return false;
        PlayerRPGData data = playerData.getOrLoad(playerId);
        long previous = data.getFarmingAbundancePoints();
        data.setFarmingAbundancePoints(previous - amount);
        if (playerData.savePlayerNow(playerId)) return true;
        data.setFarmingAbundancePoints(previous);
        return false;
    }

    /**
     * Atomically reserves points around a main-thread operation such as crafting.
     * The operation must update its external state only after this method invokes
     * it; rollback restores that state when persistence or the operation fails.
     */
    public boolean spendForTransaction(UUID playerId, long amount,
                                       BooleanSupplier operation, Runnable rollback) {
        if (playerId == null || amount < 0L || operation == null || rollback == null) return false;
        if (amount == 0L) return operation.getAsBoolean();
        PlayerRPGData data = playerData.getOrLoad(playerId);
        long previous = data.getFarmingAbundancePoints();
        if (previous < amount) return false;
        data.setFarmingAbundancePoints(previous - amount);
        boolean applied = false;
        try {
            applied = operation.getAsBoolean();
            if (applied && playerData.savePlayerNow(playerId)) return true;
        } catch (RuntimeException ignored) {
            // The caller's rollback restores its non-player state below.
        }
        data.setFarmingAbundancePoints(previous);
        try {
            rollback.run();
        } catch (RuntimeException ignored) {
            // Keep the point state consistent even if external rollback reports an error.
        }
        return false;
    }

    /** Applies a delivery result inside DeliveryDataService's single save transaction. */
    void applyTo(PlayerRPGData data, long points, Source source) {
        if (data == null || points < 0L || source == null) {
            throw new IllegalArgumentException("invalid abundance point result");
        }
        data.addFarmingAbundancePoints(points);
    }

    public enum Source {
        HARVEST,
        DELIVERY,
        FARMING_ACTIVITY
    }

    private String normalizeActivityId(String activityId) {
        return activityId == null ? "" : activityId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
