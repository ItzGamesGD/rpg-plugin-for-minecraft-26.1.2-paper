package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;

/** Phase-local gateway ownership. NORMAL has no P0 and no gateway visuals. */
public final class GatewayPhaseState {
    public enum Phase { NORMAL, PREPARE, ACTIVE, RESOLVE }
    private Phase phase = Phase.NORMAL;
    private Location snapshot;

    public Phase phase() { return phase; }
    public boolean active() { return phase != Phase.NORMAL; }
    public Location snapshot() { return snapshot == null ? null : snapshot.clone(); }
    public boolean begin(Location p0) {
        if (active()) return false;
        snapshot = p0.clone(); phase = Phase.PREPARE; return true;
    }
    public void deploy() { if (phase == Phase.PREPARE) phase = Phase.ACTIVE; }
    public void resolve() { if (phase == Phase.ACTIVE) phase = Phase.RESOLVE; }
    public void close() { phase = Phase.NORMAL; snapshot = null; }
}
