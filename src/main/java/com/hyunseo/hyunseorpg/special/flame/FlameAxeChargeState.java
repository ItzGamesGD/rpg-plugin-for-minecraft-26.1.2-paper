package com.hyunseo.hyunseorpg.special.flame;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pure charge-session state; presentation/use-item lifecycle is deliberately external. */
public final class FlameAxeChargeState {
    public enum Phase { CHARGING, FULL }
    public record Release(boolean existed, boolean heavyAttack) { }
    private final Map<UUID, Session> sessions = new HashMap<>();

    public boolean start(UUID player, UUID instance, long tick) {
        if (sessions.containsKey(player)) return false;
        sessions.put(player, new Session(instance, tick));
        return true;
    }

    public void advance(UUID player, long tick, int requiredTicks) {
        Session session = sessions.get(player);
        if (session != null && tick - session.startedAt >= Math.max(1, requiredTicks)) session.phase = Phase.FULL;
    }

    public void advanceAll(long tick, int requiredTicks) {
        sessions.values().forEach(session -> {
            if (tick - session.startedAt >= Math.max(1, requiredTicks)) session.phase = Phase.FULL;
        });
    }

    public Release release(UUID player, UUID instance) {
        Session session = sessions.remove(player);
        if (session == null || !session.instance.equals(instance)) return new Release(session != null, false);
        return new Release(true, session.phase == Phase.FULL);
    }

    public boolean contains(UUID player) { return sessions.containsKey(player); }
    public Phase phase(UUID player) { return sessions.get(player) == null ? null : sessions.get(player).phase; }
    public void clear(UUID player) { sessions.remove(player); }
    public void clear() { sessions.clear(); }

    private static final class Session {
        final UUID instance; final long startedAt; Phase phase = Phase.CHARGING;
        Session(UUID instance, long startedAt) { this.instance = instance; this.startedAt = startedAt; }
    }
}
