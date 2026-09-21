package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatewayPayloadTypeTest {
    @Test void authoritativePoolHasOnlySpecifiedReflectablesAndVolumes() {
        assertEquals(12, GatewayPayloadType.values().length);
        assertTrue(GatewayPayloadType.ARROW.reflectable());
        assertTrue(GatewayPayloadType.END_CRYSTAL_BOMB.reflectable());
        assertFalse(GatewayPayloadType.SONIC_BOOM.reflectable());
        assertTrue(GatewayPayloadType.DRAGON_BREATH.sustained());
        assertFalse(GatewayPayloadType.SONIC_BOOM.sustained());
    }
}
