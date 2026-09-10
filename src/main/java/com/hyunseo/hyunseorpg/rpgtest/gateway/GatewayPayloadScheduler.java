package com.hyunseo.hyunseorpg.rpgtest.gateway;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class GatewayPayloadScheduler {
    private final int totalCap;
    private final Map<GatewayPayloadType, Integer> localCaps;
    private final Map<GatewayPayloadType, Integer> counts = new EnumMap<>(GatewayPayloadType.class);
    private final Map<Integer, Long> busyUntilTick = new HashMap<>();
    private int total;

    public GatewayPayloadScheduler(int totalCap, Map<GatewayPayloadType, Integer> localCaps) {
        this.totalCap = totalCap;
        this.localCaps = new EnumMap<>(localCaps);
    }

    public boolean reserve(int gatewayId, GatewayPayloadType type, long tick, long activeTicks, long recoveryTicks) {
        if (total >= totalCap || tick < busyUntilTick.getOrDefault(gatewayId, 0L)) return false;
        if (counts.getOrDefault(type, 0) >= localCaps.getOrDefault(type, Integer.MAX_VALUE)) return false;
        total++;
        counts.merge(type, 1, Integer::sum);
        busyUntilTick.put(gatewayId, tick + activeTicks + recoveryTicks);
        return true;
    }

    public int total() { return total; }
    public int count(GatewayPayloadType type) { return counts.getOrDefault(type, 0); }
    public boolean ready(int gatewayId, long tick) { return tick >= busyUntilTick.getOrDefault(gatewayId, 0L); }
}
