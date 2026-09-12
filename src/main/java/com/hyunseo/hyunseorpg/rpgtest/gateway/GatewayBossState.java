package com.hyunseo.hyunseorpg.rpgtest.gateway;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Server-free encounter state. Keeps every display slot and AI summon bounded. */
public final class GatewayBossState {
    public enum SlotStatus { ORBITING, TELEGRAPHING, FLYING }
    private final SlotStatus[] slots;
    private final Set<UUID> activeSummons = new HashSet<>();

    public GatewayBossState(int slotCount) {
        slots = new SlotStatus[Math.max(1, slotCount)];
        java.util.Arrays.fill(slots, SlotStatus.ORBITING);
    }
    public int slotCount() { return slots.length; }
    public SlotStatus slotStatus(int slot) { return slots[slot]; }
    public boolean reserve(int slot) {
        if (slots[slot] != SlotStatus.ORBITING) return false;
        slots[slot] = SlotStatus.TELEGRAPHING; return true;
    }
    public void launch(int slot) { if (slots[slot] == SlotStatus.TELEGRAPHING) slots[slot] = SlotStatus.FLYING; }
    public void recover(int slot) { slots[slot] = SlotStatus.ORBITING; }
    public boolean addSummon(UUID id, int cap) {
        return activeSummons.size() < cap && activeSummons.add(id);
    }
    public void removeSummon(UUID id) { activeSummons.remove(id); }
    public int activeSummons() { return activeSummons.size(); }
    public void cleanup() { java.util.Arrays.fill(slots, SlotStatus.ORBITING); activeSummons.clear(); }
}
