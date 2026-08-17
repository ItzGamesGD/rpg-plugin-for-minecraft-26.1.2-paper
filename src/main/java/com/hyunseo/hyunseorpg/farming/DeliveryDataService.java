package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;

import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for delivery state; Bukkit GUI code never touches player YAML. */
public final class DeliveryDataService {
    private final PlayerDataService playerData;
    private final AbundancePointService abundancePoints;

    public DeliveryDataService(PlayerDataService playerData) {
        this(playerData, new AbundancePointService(playerData));
    }

    public DeliveryDataService(PlayerDataService playerData, AbundancePointService abundancePoints) {
        this.playerData = playerData;
        this.abundancePoints = abundancePoints;
    }

    public Optional<FarmingDeliveryState> get(UUID playerId, DeliveryProvider provider) {
        if (playerId == null || provider == null) return Optional.empty();
        return Optional.ofNullable(playerData.getOrLoad(playerId).getFarmingDelivery(provider.id()));
    }

    public int completedCount(UUID playerId, DeliveryProvider provider) {
        if (playerId == null || provider == null) return 0;
        return playerData.getOrLoad(playerId).getFarmingDeliveryCompletedCount(provider.id());
    }

    public boolean save(UUID playerId, DeliveryProvider provider, FarmingDeliveryState state) {
        if (playerId == null || provider == null) return false;
        PlayerRPGData data = playerData.getOrLoad(playerId);
        data.setFarmingDelivery(provider.id(), state);
        return playerData.savePlayerNow(playerId);
    }

    public boolean clear(UUID playerId, DeliveryProvider provider) {
        if (playerId == null || provider == null) return false;
        PlayerRPGData data = playerData.getOrLoad(playerId);
        data.clearFarmingDelivery(provider.id());
        return playerData.savePlayerNow(playerId);
    }

    public boolean complete(UUID playerId, DeliveryProvider provider, String deliveryId) {
        return completeWithRewards(playerId, provider, deliveryId, 0L, 0L,
                System.currentTimeMillis());
    }

    /** Completes a delivery and commits all Stage 9 rewards in one player save. */
    public boolean completeWithRewards(UUID playerId, DeliveryProvider provider, String deliveryId,
                                       long points, long favorIncrease) {
        return completeWithRewards(playerId, provider, deliveryId, points, favorIncrease,
                System.currentTimeMillis());
    }

    /** Completes a delivery with an explicit state transition timestamp. */
    public boolean completeWithRewards(UUID playerId, DeliveryProvider provider, String deliveryId,
                                       long points, long favorIncrease, long completedAt) {
        if (playerId == null || provider == null || deliveryId == null) return false;
        if (points < 0L || favorIncrease < 0L || completedAt < 0L) return false;
        PlayerRPGData data = playerData.getOrLoad(playerId);
        FarmingDeliveryState old = data.getFarmingDelivery(provider.id());
        if (old == null || old.status() != DeliveryStatus.ACTIVE
                || !old.deliveryId().equals(deliveryId) || old.expiresAt() <= completedAt) return false;
        int oldCount = data.getFarmingDeliveryCompletedCount(provider.id());
        long oldPoints = data.getFarmingAbundancePoints();
        long oldFavor = data.getFarmingFavor(provider.id());
        data.setFarmingDelivery(provider.id(), new FarmingDeliveryState(old.deliveryId(), old.definitionId(),
                old.itemFamily(), old.requiredAmount(), old.minimumQuality(), old.createdAt(), old.expiresAt(),
                DeliveryStatus.COMPLETED, completedAt));
        abundancePoints.applyTo(data, points, AbundancePointService.Source.DELIVERY);
        data.addFarmingFavor(provider.id(), favorIncrease);
        data.incrementFarmingDeliveryCompletedCount(provider.id());
        if (playerData.savePlayerNow(playerId)) return true;
        data.setFarmingDelivery(provider.id(), old);
        data.setFarmingDeliveryCompletedCount(provider.id(), oldCount);
        data.setFarmingAbundancePoints(oldPoints);
        data.setFarmingFavor(provider.id(), oldFavor);
        return false;
    }

    public boolean expire(UUID playerId, DeliveryProvider provider, String deliveryId) {
        return expire(playerId, provider, deliveryId, System.currentTimeMillis());
    }

    public boolean expire(UUID playerId, DeliveryProvider provider, String deliveryId, long expiredAt) {
        if (playerId == null || provider == null) return false;
        if (expiredAt < 0L) return false;
        PlayerRPGData data = playerData.getOrLoad(playerId);
        FarmingDeliveryState old = data.getFarmingDelivery(provider.id());
        if (old == null || old.status() != DeliveryStatus.ACTIVE
                || (deliveryId != null && !old.deliveryId().equals(deliveryId))) return false;
        data.setFarmingDelivery(provider.id(), new FarmingDeliveryState(old.deliveryId(), old.definitionId(),
                old.itemFamily(), old.requiredAmount(), old.minimumQuality(), old.createdAt(), old.expiresAt(),
                DeliveryStatus.EXPIRED, expiredAt));
        return playerData.savePlayerNow(playerId);
    }
}
