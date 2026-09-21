package com.hyunseo.hyunseorpg.rpgtest.gateway;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Server-free encounter state. Keeps every display slot and hidden AI driver bounded. */
public final class GatewayBossState {
    public enum SlotStatus { ORBITING, TELEGRAPHING, FLYING }
    private final SlotStatus[] slots;
    private final Set<UUID> activeDrivers = new HashSet<>();

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
    /** Selects among the caller's eligible family slots; it never falls back to a sequential slot. */
    public int randomOrbitingSlot(List<Integer> candidates, RandomGenerator random) {
        java.util.ArrayList<Integer> available = new java.util.ArrayList<>();
        for (int slot : candidates) if (slot >= 0 && slot < slots.length && slots[slot] == SlotStatus.ORBITING) available.add(slot);
        return available.isEmpty() ? -1 : available.get(random.nextInt(available.size()));
    }
    public boolean addDriver(UUID id, int cap) {
        return activeDrivers.size() < cap && activeDrivers.add(id);
    }
    public void removeDriver(UUID id) { activeDrivers.remove(id); }
    public int activeDrivers() { return activeDrivers.size(); }
    public void cleanup() { java.util.Arrays.fill(slots, SlotStatus.ORBITING); activeDrivers.clear(); }
}
