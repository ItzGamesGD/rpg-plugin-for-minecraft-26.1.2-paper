package com.hyunseo.hyunseorpg.special.thunder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-tick gameplay charge state, deliberately independent of item-use presentation. */
public final class ThunderAxeChargeState {
    public static final float PRESENTATION_SECONDS = 1_200F;
    public record Release(boolean existed, boolean fullCharge) { }

    private final Map<UUID, Session> sessions = new HashMap<>();

    public void start(UUID owner, UUID instance, long tick) {
        sessions.put(owner, new Session(instance, tick));
    }

    public void advance(UUID owner, long tick, int requiredTicks) {
        Session session = sessions.get(owner);
        if (session != null && tick - session.startedAt >= Math.max(1, requiredTicks)) session.full = true;
    }

    public void advanceAll(long tick, int requiredTicks) {
        for (UUID owner : Set.copyOf(sessions.keySet())) advance(owner, tick, requiredTicks);
    }

    public Release release(UUID owner, UUID instance) {
        Session session = sessions.remove(owner);
        if (session == null || instance == null || !session.instance.equals(instance)) {
            return new Release(session != null, false);
        }
        return new Release(true, session.full);
    }

    public boolean contains(UUID owner) { return sessions.containsKey(owner); }
    public Set<UUID> owners() { return Set.copyOf(sessions.keySet()); }
    public void clear(UUID owner) { sessions.remove(owner); }
    public void clear() { sessions.clear(); }

    private static final class Session {
        final UUID instance;
        final long startedAt;
        boolean full;

        private Session(UUID instance, long startedAt) {
            this.instance = instance;
            this.startedAt = startedAt;
        }
    }
}
