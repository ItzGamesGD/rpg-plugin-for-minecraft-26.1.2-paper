package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test void volumeParticlesSupplyRuntimeDataWhenPaperRequiresIt() {
        for (Particle particle : List.of(
                Particle.SONIC_BOOM,
                Particle.DRAGON_BREATH,
                Particle.FLAME,
                Particle.END_ROD)) {
            Object data = GatewayPrototypeService.volumeParticleData(particle);
            if (particle.getDataType() == Void.class) {
                assertNull(data, particle.name());
            } else {
                assertTrue(particle.getDataType().isInstance(data),
                        () -> particle + " requires " + particle.getDataType().getName()
                                + " but got " + (data == null ? "null" : data.getClass().getName()));
            }
        }
    }
}
