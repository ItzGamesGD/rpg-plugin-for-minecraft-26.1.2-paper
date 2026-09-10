package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GatewayPayloadSchedulerTest {
    @Test void enforcesTotalAndLocalCapsWithoutWeights() {
        GatewayPayloadScheduler scheduler = new GatewayPayloadScheduler(3, Map.of(GatewayPayloadType.BEAM, 1));
        assertTrue(scheduler.reserve(1, GatewayPayloadType.BEAM, 0, 10, 2));
        assertFalse(scheduler.reserve(2, GatewayPayloadType.BEAM, 0, 10, 2));
        assertTrue(scheduler.reserve(2, GatewayPayloadType.ARROW, 0, 1, 2));
        assertTrue(scheduler.reserve(3, GatewayPayloadType.TRIDENT, 0, 1, 2));
        assertFalse(scheduler.reserve(4, GatewayPayloadType.ARROW, 0, 1, 2));
        assertEquals(3, scheduler.total());
    }

    @Test void preventsSameGatewayOverlapThenAllowsReuse() {
        GatewayPayloadScheduler scheduler = new GatewayPayloadScheduler(4, Map.of());
        assertTrue(scheduler.reserve(7, GatewayPayloadType.DRAGON_BREATH, 5, 50, 8));
        assertFalse(scheduler.reserve(7, GatewayPayloadType.ARROW, 62, 1, 8));
        assertTrue(scheduler.ready(7, 63));
        assertTrue(scheduler.reserve(7, GatewayPayloadType.ARROW, 63, 1, 8));
    }
}
