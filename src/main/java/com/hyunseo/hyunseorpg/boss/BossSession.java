package com.hyunseo.hyunseorpg.boss;

import org.bukkit.boss.BossBar;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossSession {
    private final BossType bossType;
    private final UUID bossUuid;
    private final UUID worldUuid;
    private final String worldName;
    private final long startTimeMillis;
    private final long durationSeconds;
    private final Set<UUID> participants = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> damageContributors = new ConcurrentHashMap<>();
    private final Map<UUID, Double> crystalContributors = new ConcurrentHashMap<>();
    private final Set<UUID> countedCrystals = ConcurrentHashMap.newKeySet();
    private final BossBar bossBar;
    private volatile boolean rewardGiven;
    private volatile boolean ended;
    private volatile BossEndReason endReason;

    public BossSession(BossType bossType, UUID bossUuid, String worldName, long startTimeMillis,
                       long durationSeconds, BossBar bossBar) {
        this(bossType, bossUuid, null, worldName, startTimeMillis, durationSeconds, bossBar);
    }

    public BossSession(BossType bossType, UUID bossUuid, UUID worldUuid, String worldName, long startTimeMillis,
                       long durationSeconds, BossBar bossBar) {
        this.bossType = bossType;
        this.bossUuid = bossUuid;
        this.worldUuid = worldUuid;
        this.worldName = worldName;
        this.startTimeMillis = startTimeMillis;
        this.durationSeconds = durationSeconds;
        this.bossBar = bossBar;
    }

    public BossType bossType() { return bossType; }
    public UUID bossUuid() { return bossUuid; }
    public UUID worldUuid() { return worldUuid; }
    public String worldName() { return worldName; }
    public long startTimeMillis() { return startTimeMillis; }
    public long durationSeconds() { return durationSeconds; }
    public BossBar bossBar() { return bossBar; }
    public boolean rewardGiven() { return rewardGiven; }
    public void setRewardGiven(boolean value) { rewardGiven = value; }
    public boolean ended() { return ended; }
    public void setEnded(boolean value) { ended = value; }
    public BossEndReason endReason() { return endReason; }
    public void setEndReason(BossEndReason value) { endReason = value; }
    public Set<UUID> participants() { return Collections.unmodifiableSet(participants); }
    public Map<UUID, Double> damageContributors() { return Collections.unmodifiableMap(damageContributors); }
    public Map<UUID, Double> crystalContributors() { return Collections.unmodifiableMap(crystalContributors); }
    public Map<UUID, Double> contributionScores() {
        Map<UUID, Double> scores = new java.util.HashMap<>(damageContributors);
        crystalContributors.forEach((uuid, value) -> scores.merge(uuid, value, Double::sum));
        return Collections.unmodifiableMap(scores);
    }
    public void addParticipant(UUID uuid) { if (uuid != null) participants.add(uuid); }
    public void addDamage(UUID uuid, double amount) {
        if (uuid == null) return;
        participants.add(uuid);
        damageContributors.merge(uuid, Math.max(0.0D, amount), Double::sum);
    }

    public boolean addCrystalContribution(UUID crystalUuid, UUID playerUuid, double score) {
        if (crystalUuid == null || playerUuid == null || score <= 0.0D || !countedCrystals.add(crystalUuid)) return false;
        participants.add(playerUuid);
        crystalContributors.merge(playerUuid, score, Double::sum);
        return true;
    }
}
