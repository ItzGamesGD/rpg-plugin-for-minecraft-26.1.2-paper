package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

/** Access and mutation boundary for player-bound farming progression. */
public final class FarmingProfileService {
    private final PlayerDataService playerDataService;
    private final CropRegistry cropRegistry;

    public FarmingProfileService(PlayerDataService playerDataService, CropRegistry cropRegistry) {
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
        this.cropRegistry = Objects.requireNonNull(cropRegistry, "cropRegistry");
    }

    public FarmingProfile getFarmingProfile(Player player) {
        return getFarmingProfile(Objects.requireNonNull(player, "player").getUniqueId());
    }

    public FarmingProfile getFarmingProfile(UUID playerId) {
        return snapshot(data(playerId));
    }

    public long getAbundancePoints(UUID playerId) {
        return data(playerId).getFarmingAbundancePoints();
    }

    public long getFavor(UUID playerId, DeliveryProvider provider) {
        return provider == null ? 0L : data(playerId).getFarmingFavor(provider.id());
    }

    /** Administrative boundary for setting the persisted abundance balance. */
    public boolean setAbundancePoints(UUID playerId, long amount) {
        if (playerId == null || amount < 0L) return false;
        PlayerRPGData data = data(playerId);
        long previous = data.getFarmingAbundancePoints();
        data.setFarmingAbundancePoints(amount);
        if (playerDataService.savePlayerNow(playerId)) return true;
        data.setFarmingAbundancePoints(previous);
        return false;
    }

    public boolean addAbundancePoints(UUID playerId, long amount) {
        if (playerId == null || amount < 0L) return false;
        long previous = data(playerId).getFarmingAbundancePoints();
        long updated = amount > Long.MAX_VALUE - previous ? Long.MAX_VALUE : previous + amount;
        return setAbundancePoints(playerId, updated);
    }

    /** Administrative boundary for setting provider-specific favor. */
    public boolean setFavor(UUID playerId, DeliveryProvider provider, long amount) {
        if (playerId == null || provider == null || provider == DeliveryProvider.ESTATE_RESERVED || amount < 0L) {
            return false;
        }
        PlayerRPGData data = data(playerId);
        long previous = data.getFarmingFavor(provider.id());
        data.setFarmingFavor(provider.id(), amount);
        if (playerDataService.savePlayerNow(playerId)) return true;
        data.setFarmingFavor(provider.id(), previous);
        return false;
    }

    public boolean addFavor(UUID playerId, DeliveryProvider provider, long amount) {
        if (playerId == null || provider == null || amount < 0L) return false;
        long previous = data(playerId).getFarmingFavor(provider.id());
        long updated = amount > Long.MAX_VALUE - previous ? Long.MAX_VALUE : previous + amount;
        return setFavor(playerId, provider, updated);
    }

    public boolean isReady(Player player) {
        return player != null && playerDataService.isLoaded(player.getUniqueId());
    }

    public boolean isCropUnlocked(Player player, String cropId) {
        return isCropUnlocked(Objects.requireNonNull(player, "player").getUniqueId(), cropId);
    }

    public boolean isCropUnlocked(UUID playerId, String cropId) {
        String id = knownCropId(cropId);
        return id != null && data(playerId).isFarmingCropUnlocked(id);
    }

    public boolean unlockCrop(Player player, String cropId) {
        return unlockCrop(Objects.requireNonNull(player, "player").getUniqueId(), cropId);
    }

    public boolean unlockCrop(UUID playerId, String cropId) {
        String id = knownCropId(cropId);
        if (id == null) return false;
        PlayerRPGData data = data(playerId);
        boolean wasUnlocked = data.isFarmingCropUnlocked(id);
        data.unlockFarmingCrop(id);
        if (playerDataService.savePlayerNow(playerId)) return true;
        if (!wasUnlocked) data.lockFarmingCrop(id);
        return false;
    }

    public boolean lockCrop(Player player, String cropId) {
        return lockCrop(Objects.requireNonNull(player, "player").getUniqueId(), cropId);
    }

    public boolean lockCrop(UUID playerId, String cropId) {
        String id = knownCropId(cropId);
        if (id == null) return false;
        PlayerRPGData data = data(playerId);
        boolean wasUnlocked = data.isFarmingCropUnlocked(id);
        data.lockFarmingCrop(id);
        if (playerDataService.savePlayerNow(playerId)) return true;
        if (wasUnlocked) data.unlockFarmingCrop(id);
        return false;
    }

    /** Changes only the farming stage. Existing crop unlocks are deliberately preserved. */
    public boolean setStage(Player player, FarmingStage stage) {
        return setStage(Objects.requireNonNull(player, "player").getUniqueId(), stage);
    }

    public boolean setStage(UUID playerId, FarmingStage stage) {
        PlayerRPGData data = data(playerId);
        FarmingStage previous = data.getFarmingStage();
        data.setFarmingStage(stage);
        if (playerDataService.savePlayerNow(playerId)) return true;
        data.setFarmingStage(previous);
        return false;
    }

    /** Adds missing unlocks implied by a stage without removing existing unlocks. */
    public boolean recalculateUnlocks(UUID playerId) {
        PlayerRPGData data = data(playerId);
        for (String cropId : stageUnlocks(data.getFarmingStage())) {
            if (knownCropId(cropId) != null) data.unlockFarmingCrop(cropId);
        }
        return playerDataService.savePlayerNow(playerId);
    }

    /** Records one account-bound token use and persists it atomically with the profile. */
    public boolean useStatToken(UUID playerId, String tokenId, int maximumUses) {
        if (playerId == null || tokenId == null || tokenId.isBlank() || maximumUses < 1) return false;
        PlayerRPGData data = data(playerId);
        String id = tokenId.trim().toLowerCase(java.util.Locale.ROOT);
        int previous = data.getFarmingStatTokenUses(id);
        if (previous >= maximumUses) return false;
        data.setFarmingStatTokenUses(id, previous + 1);
        if (playerDataService.savePlayerNow(playerId)) return true;
        data.setFarmingStatTokenUses(id, previous);
        return false;
    }

    /**
     * Commit point for a future farming promotion flow. Stage and its new unlock
     * are changed together and persisted once; no promotion condition is decided here.
     */
    public boolean applyPromotionResult(UUID playerId, FarmingStage newStage, String cropId) {
        String id = cropId == null || cropId.isBlank() ? null : knownCropId(cropId);
        if (cropId != null && !cropId.isBlank() && id == null) return false;
        if (newStage == null) return false;
        PlayerRPGData data = data(playerId);
        FarmingStage oldStage = data.getFarmingStage();
        Set<String> oldUnlocks = new HashSet<>(data.getFarmingUnlockedCrops());
        try {
            data.setFarmingStage(newStage);
            if (id != null) data.unlockFarmingCrop(id);
            if (playerDataService.savePlayerNow(playerId)) return true;
        } catch (RuntimeException ignored) {
            // Restore the in-memory snapshot below; the caller receives a failed transaction.
        }
        data.setFarmingStage(oldStage);
        data.clearFarmingUnlockedCrops();
        oldUnlocks.forEach(data::unlockFarmingCrop);
        playerDataService.savePlayer(playerId);
        return false;
    }

    public boolean resetFarmingProfile(UUID playerId) {
        PlayerRPGData data = data(playerId);
        FarmingSnapshot previous = FarmingSnapshot.capture(data);
        try {
            data.setFarmingStage(FarmingStage.BASIC);
            data.clearFarmingUnlockedCrops();
            if (knownCropId("corn") != null) data.unlockFarmingCrop("corn");
            data.clearFarmingHarvestData();
            data.clearFarmingStatTokenUses();
            data.setFarmingAbundancePoints(0L);
            data.clearFarmingFavor();
            data.clearFarmingDeliveries();
            data.clearFarmingDeliveryCompletedCounts();
            if (playerDataService.savePlayerNow(playerId)) return true;
        } catch (RuntimeException ignored) {
            // Restore the complete farming portion below; the caller receives a failed reset.
        }
        previous.restore(data);
        playerDataService.savePlayer(playerId);
        return false;
    }

    /** Administrative test boundary: changes only the aggregate harvest gate. */
    public boolean setTotalValidHarvests(UUID playerId, long amount) {
        if (amount < 0L) return false;
        PlayerRPGData data = data(playerId);
        long previous = data.getFarmingTotalValidHarvests();
        try {
            data.setFarmingTotalValidHarvests(amount);
            if (playerDataService.savePlayerNow(playerId)) return true;
        } catch (RuntimeException ignored) {
            // Restore below.
        }
        data.setFarmingTotalValidHarvests(previous);
        playerDataService.savePlayer(playerId);
        return false;
    }

    /** Administrative test boundary: saturates at Long.MAX_VALUE instead of wrapping. */
    public boolean addTotalValidHarvests(UUID playerId, long amount) {
        if (amount < 0L) return false;
        PlayerRPGData data = data(playerId);
        long previous = data.getFarmingTotalValidHarvests();
        long updated = amount > Long.MAX_VALUE - previous ? Long.MAX_VALUE : previous + amount;
        return setTotalValidHarvests(playerId, updated);
    }

    /** Administrative test boundary for account-bound token counters. */
    public boolean setStatTokenUses(UUID playerId, String tokenId, int uses) {
        if (tokenId == null || tokenId.isBlank() || uses < 0) return false;
        String id = tokenId.trim().toLowerCase(java.util.Locale.ROOT);
        PlayerRPGData data = data(playerId);
        int previous = data.getFarmingStatTokenUses(id);
        try {
            data.setFarmingStatTokenUses(id, uses);
            if (playerDataService.savePlayerNow(playerId)) return true;
        } catch (RuntimeException ignored) {
            // Restore below.
        }
        data.setFarmingStatTokenUses(id, previous);
        playerDataService.savePlayer(playerId);
        return false;
    }

    public void addValidHarvest(Player player, String cropId, long amount) {
        addValidHarvest(Objects.requireNonNull(player, "player").getUniqueId(), cropId, amount);
    }

    public void addValidHarvest(UUID playerId, String cropId, long amount) {
        String id = knownCropId(cropId);
        if (id == null) throw new IllegalArgumentException("Unknown farming crop: " + cropId);
        PlayerRPGData data = data(playerId);
        data.addFarmingValidHarvest(id, amount);
        playerDataService.savePlayer(playerId);
    }

    public boolean isKnownCrop(String cropId) {
        return knownCropId(cropId) != null;
    }

    public Optional<FarmingStage> requiredStageForCrop(String cropId) {
        String id = knownCropId(cropId);
        if (id == null) return Optional.empty();
        if (id.equals("corn")) return Optional.of(FarmingStage.BASIC);
        if (id.equals("onion")) return Optional.of(FarmingStage.SKILLED);
        if (id.equals("chili")) return Optional.of(FarmingStage.PROFICIENT);
        if (id.equals("garlic")) return Optional.of(FarmingStage.ADVANCED);
        return Optional.empty();
    }

    public long getCropHarvestCount(Player player, String cropId) {
        return getCropHarvestCount(Objects.requireNonNull(player, "player").getUniqueId(), cropId);
    }

    public long getCropHarvestCount(UUID playerId, String cropId) {
        String id = knownCropId(cropId);
        return id == null ? 0L : data(playerId).getFarmingCropHarvestCount(id);
    }

    private PlayerRPGData data(UUID playerId) {
        return playerDataService.getOrLoad(Objects.requireNonNull(playerId, "playerId"));
    }

    private String knownCropId(String cropId) {
        if (cropId == null || cropId.isBlank()) return null;
        String normalized = cropId.trim().toLowerCase(java.util.Locale.ROOT);
        return cropRegistry.get(normalized).isPresent() ? normalized : null;
    }

    private Set<String> stageUnlocks(FarmingStage stage) {
        return switch (stage) {
            case BASIC -> Set.of("corn");
            case SKILLED -> Set.of("corn", "onion");
            case PROFICIENT -> Set.of("corn", "onion", "chili");
            case ADVANCED, EXPERT -> Set.of("corn", "onion", "chili", "garlic");
        };
    }

    private FarmingProfile snapshot(PlayerRPGData data) {
        return new FarmingProfile(
                data.getFarmingDataVersion(), data.getFarmingStage(), data.getFarmingTotalValidHarvests(),
                data.getFarmingCropHarvests(), data.getFarmingUnlockedCrops(), data.getFarmingStatTokenUses(),
                data.getFarmingAbundancePoints(), data.getFarmingFavor(),
                data.getFarmingDeliveries(), data.getFarmingDeliveryCompletedCounts());
    }

    private record FarmingSnapshot(FarmingStage stage, Set<String> unlockedCrops,
                                   long totalHarvests, java.util.Map<String, Long> cropHarvests,
                                   java.util.Map<String, Integer> tokenUses, long abundancePoints,
                                   java.util.Map<String, Long> favor,
                                   java.util.Map<String, FarmingDeliveryState> deliveries,
                                   java.util.Map<String, Integer> deliveryCounts) {
        static FarmingSnapshot capture(PlayerRPGData data) {
            return new FarmingSnapshot(data.getFarmingStage(), new HashSet<>(data.getFarmingUnlockedCrops()),
                    data.getFarmingTotalValidHarvests(), new java.util.HashMap<>(data.getFarmingCropHarvests()),
                    new java.util.HashMap<>(data.getFarmingStatTokenUses()), data.getFarmingAbundancePoints(),
                    new java.util.HashMap<>(data.getFarmingFavor()), new java.util.HashMap<>(data.getFarmingDeliveries()),
                    new java.util.HashMap<>(data.getFarmingDeliveryCompletedCounts()));
        }

        void restore(PlayerRPGData data) {
            data.setFarmingStage(stage);
            data.clearFarmingUnlockedCrops();
            unlockedCrops.forEach(data::unlockFarmingCrop);
            data.clearFarmingHarvestData();
            data.setFarmingTotalValidHarvests(totalHarvests);
            cropHarvests.forEach(data::setFarmingCropHarvestCount);
            data.clearFarmingStatTokenUses();
            tokenUses.forEach(data::setFarmingStatTokenUses);
            data.setFarmingAbundancePoints(abundancePoints);
            data.clearFarmingFavor();
            favor.forEach(data::setFarmingFavor);
            data.clearFarmingDeliveries();
            deliveries.forEach(data::setFarmingDelivery);
            data.clearFarmingDeliveryCompletedCounts();
            deliveryCounts.forEach(data::setFarmingDeliveryCompletedCount);
        }
    }
}
