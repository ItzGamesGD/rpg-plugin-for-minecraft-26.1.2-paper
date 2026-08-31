package com.hyunseo.hyunseorpg.special.water;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Pure, server-independent state rules used by the water-trident runtime. */
public final class WaterTridentState {
    public static final int SYNTHETIC_COUNT = 8;

    public enum FlightPhase { OUTWARD, RETURNING, REMOVED }
    public enum SignaturePhase { VORTEX, EIGHT_WAY, COMPLETE }
    public enum MovementEnd { NORMAL, COLLISION, CANCELLED, INVALIDATED }

    private final Map<UUID, Combo> combos = new HashMap<>();

    public int registerCombo(UUID player, UUID target, long now, long timeoutMillis) {
        Combo previous = combos.get(player);
        int hits = previous == null || previous.expiresAt < now || !previous.target.equals(target)
                ? 1 : previous.hits + 1;
        if (hits >= 3) combos.remove(player);
        else combos.put(player, new Combo(target, hits, now + timeoutMillis));
        return hits;
    }

    public void clear(UUID player) { combos.remove(player); }
    public int activeCombos() { return combos.size(); }

    public static boolean currentMayAttack(FlightPhase phase, boolean projectileInWater) {
        return phase == FlightPhase.OUTWARD && projectileInWater;
    }

    /** Direct impact is valid on land, but never during the synthetic return phase. */
    public static boolean directHitMayAttack(FlightPhase phase) {
        return phase == FlightPhase.OUTWARD;
    }

    public static SignaturePhase advanceSignature(SignaturePhase phase) {
        return switch (phase) {
            case VORTEX -> SignaturePhase.EIGHT_WAY;
            case EIGHT_WAY, COMPLETE -> SignaturePhase.COMPLETE;
        };
    }

    public static boolean applyNormalVerticalBoost(MovementEnd reason) {
        return reason == MovementEnd.NORMAL;
    }

    public static boolean rainExposed(boolean onLand, boolean storm, boolean biomeReceivesRain,
                                      int highestBlockingY, int playerBlockY) {
        return onLand && storm && biomeReceivesRain && highestBlockingY <= playerBlockY;
    }

    public static final class SyntheticAttack {
        private final Set<UUID> hitTargets = new HashSet<>();
        private final int maximumHits;

        public SyntheticAttack(int maximumHits) { this.maximumHits = maximumHits; }

        public boolean tryHit(UUID target) {
            if (hitTargets.size() >= maximumHits || !hitTargets.add(target)) return false;
            return true;
        }

        public int hitCount() { return hitTargets.size(); }
        public int maximumHits() { return maximumHits; }
    }

    private record Combo(UUID target, int hits, long expiresAt) { }
}
