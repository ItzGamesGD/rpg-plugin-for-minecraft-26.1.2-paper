package com.hyunseo.hyunseorpg.special.thanatos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pure authoritative lifecycle state; Bukkit entities and displays remain adapters. */
public final class ThanatosState {
    public enum MortalPhase { WAITING, FALLING }
    public enum UltimatumPhase { CHARGING, LAUNCHED, FALLING }

    private final Map<UUID, Mortal> mortals = new HashMap<>();
    private final Map<UUID, Ultimatum> ultimatums = new HashMap<>();

    public boolean beginMortal(UUID target, long now, long delayTicks) {
        if (mortals.containsKey(target)) return false;
        mortals.put(target, new Mortal(now + Math.max(1L, delayTicks), MortalPhase.WAITING));
        return true;
    }

    public MortalPhase mortalPhase(UUID target) {
        Mortal mortal = mortals.get(target);
        return mortal == null ? null : mortal.phase;
    }

    public boolean mortalDue(UUID target, long now) {
        Mortal mortal = mortals.get(target);
        return mortal != null && mortal.phase == MortalPhase.WAITING && mortal.deadline <= now;
    }

    public boolean beginMortalFall(UUID target, long now, long fallTicks) {
        Mortal mortal = mortals.get(target);
        if (mortal == null || mortal.phase != MortalPhase.WAITING || now < mortal.deadline) return false;
        mortal.phase = MortalPhase.FALLING;
        mortal.deadline = now + Math.max(1L, fallTicks);
        return true;
    }

    public boolean mortalImpactDue(UUID target, long now) {
        Mortal mortal = mortals.get(target);
        return mortal != null && mortal.phase == MortalPhase.FALLING && mortal.deadline <= now;
    }

    public boolean consumeMortalImpact(UUID target, long now) {
        if (!mortalImpactDue(target, now)) return false;
        mortals.remove(target);
        return true;
    }

    public boolean hasMortal(UUID target) { return mortals.containsKey(target); }
    public void endMortal(UUID target) { mortals.remove(target); }
    public int mortalCount() { return mortals.size(); }

    public boolean beginUltimatum(UUID player, UUID world, long now, long chargeTicks) {
        return ultimatums.putIfAbsent(player,
                new Ultimatum(world, now + Math.max(1L, chargeTicks), UltimatumPhase.CHARGING)) == null;
    }

    public UltimatumPhase phase(UUID player) {
        Ultimatum ultimatum = ultimatums.get(player);
        return ultimatum == null ? null : ultimatum.phase;
    }

    public boolean chargeDue(UUID player, long now) {
        Ultimatum ultimatum = ultimatums.get(player);
        return ultimatum != null && ultimatum.phase == UltimatumPhase.CHARGING && now >= ultimatum.deadline;
    }

    public boolean launch(UUID player) { return transition(player, UltimatumPhase.CHARGING, UltimatumPhase.LAUNCHED); }
    public boolean beginFall(UUID player) { return transition(player, UltimatumPhase.LAUNCHED, UltimatumPhase.FALLING); }

    public boolean land(UUID player, UUID world, boolean grounded) {
        Ultimatum ultimatum = ultimatums.get(player);
        if (ultimatum == null || ultimatum.phase != UltimatumPhase.FALLING
                || !ultimatum.world.equals(world) || !grounded) return false;
        ultimatums.remove(player);
        return true;
    }

    public boolean hasUltimatum(UUID player) { return ultimatums.containsKey(player); }
    public void cancelUltimatum(UUID player) { ultimatums.remove(player); }
    public void clear() { mortals.clear(); ultimatums.clear(); }

    private boolean transition(UUID id, UltimatumPhase from, UltimatumPhase to) {
        Ultimatum ultimatum = ultimatums.get(id);
        if (ultimatum == null || ultimatum.phase != from) return false;
        ultimatum.phase = to;
        return true;
    }

    private static final class Mortal {
        private long deadline;
        private MortalPhase phase;
        private Mortal(long deadline, MortalPhase phase) { this.deadline = deadline; this.phase = phase; }
    }

    private static final class Ultimatum {
        private final UUID world;
        private final long deadline;
        private UltimatumPhase phase;
        private Ultimatum(UUID world, long deadline, UltimatumPhase phase) {
            this.world = world; this.deadline = deadline; this.phase = phase;
        }
    }
}
