package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayVolumeCadenceTest {
    @Test void sonicBoomDamagesExactlyOnceAfterTelegraph() {
        long hits = IntStream.rangeClosed(1, 50)
                .filter(age -> GatewayPrototypeService.shouldDamageVolume(
                        GatewayPayloadType.SONIC_BOOM, age, 12))
                .count();
        assertEquals(1, hits);
        assertTrue(GatewayPrototypeService.shouldDamageVolume(
                GatewayPayloadType.SONIC_BOOM, 12, 12));
    }

    @Test void onlySustainedVolumesUseRepeatedCadence() {
        long beamHits = IntStream.rangeClosed(1, 50)
                .filter(age -> GatewayPrototypeService.shouldDamageVolume(
                        GatewayPayloadType.BEAM, age, 12))
                .count();
        assertTrue(beamHits > 1);
    }

    @Test void configuredCadenceControlsSustainedDamageWindows() {
        long hits = IntStream.rangeClosed(1, 100)
                .filter(age -> GatewayPrototypeService.shouldDamageVolume(
                        GatewayPayloadType.BEAM, age, 24, 16))
                .count();
        assertEquals(5, hits);
    }
}
