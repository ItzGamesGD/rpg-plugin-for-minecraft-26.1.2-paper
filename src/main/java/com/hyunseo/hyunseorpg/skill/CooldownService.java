package com.hyunseo.hyunseorpg.skill;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public final class CooldownService {
    private final Clock clock;
    private final boolean disabled;
    private final Map<UUID, Map<String, Long>> cooldownEndsByPlayer = new HashMap<>();

    public CooldownService() {
        this(Clock.systemUTC());
    }

    CooldownService(Clock clock) {
        this(clock, false);
    }

    public CooldownService(boolean disabled) {
        this(Clock.systemUTC(), disabled);
    }

    CooldownService(Clock clock, boolean disabled) {
        this.clock = clock;
        this.disabled = disabled;
    }

    public boolean isOnCooldown(UUID playerId, String cooldownId) {
        return getRemainingMillis(playerId, cooldownId) > 0L;
    }

    public long getRemainingMillis(UUID playerId, String cooldownId) {
        OptionalLong expiresAt = getExpiresAt(playerId, cooldownId);
        if (expiresAt.isEmpty()) {
            return 0L;
        }

        long remainingMillis = expiresAt.getAsLong() - clock.millis();
        if (remainingMillis <= 0L) {
            clearCooldown(playerId, cooldownId);
            return 0L;
        }
        return remainingMillis;
    }

    public OptionalLong getExpiresAt(UUID playerId, String cooldownId) {
        Map<String, Long> playerCooldowns = cooldownEndsByPlayer.get(playerId);
        if (playerCooldowns == null) {
            return OptionalLong.empty();
        }

        Long expiresAt = playerCooldowns.get(normalizeId(cooldownId));
        return expiresAt == null ? OptionalLong.empty() : OptionalLong.of(expiresAt);
    }

    public void startCooldown(UUID playerId, String cooldownId, long durationMillis) {
        if (disabled || durationMillis <= 0L) {
            clearCooldown(playerId, cooldownId);
            return;
        }

        cooldownEndsByPlayer
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(normalizeId(cooldownId), clock.millis() + durationMillis);
    }

    public void startCooldownTicks(UUID playerId, String cooldownId, long durationTicks) {
        startCooldown(playerId, cooldownId, durationTicks * 50L);
    }

    public void clearCooldown(UUID playerId, String cooldownId) {
        Map<String, Long> playerCooldowns = cooldownEndsByPlayer.get(playerId);
        if (playerCooldowns == null) {
            return;
        }

        playerCooldowns.remove(normalizeId(cooldownId));
        if (playerCooldowns.isEmpty()) {
            cooldownEndsByPlayer.remove(playerId);
        }
    }

    public void clearPlayer(UUID playerId) {
        cooldownEndsByPlayer.remove(playerId);
    }

    public Map<String, Long> getRemainingCooldowns(UUID playerId) {
        Map<String, Long> playerCooldowns = cooldownEndsByPlayer.get(playerId);
        if (playerCooldowns == null || playerCooldowns.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> remainingCooldowns = new HashMap<>();
        for (String cooldownId : playerCooldowns.keySet().toArray(String[]::new)) {
            long remainingMillis = getRemainingMillis(playerId, cooldownId);
            if (remainingMillis > 0L) {
                remainingCooldowns.put(cooldownId, remainingMillis);
            }
        }
        return Map.copyOf(remainingCooldowns);
    }

    public void clearAll() {
        cooldownEndsByPlayer.clear();
    }

    private String normalizeId(String cooldownId) {
        String normalized = cooldownId == null ? "" : cooldownId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("cooldownId must not be empty");
        }
        return normalized;
    }
}
