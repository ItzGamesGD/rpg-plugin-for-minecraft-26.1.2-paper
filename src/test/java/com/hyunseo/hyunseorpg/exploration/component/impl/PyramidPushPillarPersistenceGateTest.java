package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidGridPoint;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class PyramidPushPillarPersistenceGateTest {
    @Test
    void nullRepositoryCannotCommitNonFinalLogicalMove() {
        PyramidPushPillarService service = new PyramidPushPillarService(null, null, null);
        assertFalse(service.persistLogicalPosition(UUID.randomUUID(), "sun",
                new PyramidGridPoint(-2, -3), false));
    }

    @Test
    void nullRepositoryCannotCommitFinalSolvedIntentOrPosition() {
        PyramidPushPillarService service = new PyramidPushPillarService(null, null, null);
        assertFalse(service.persistSolvedIntentAndLogicalPosition(UUID.randomUUID(), "sun",
                new PyramidGridPoint(-1, -1)));
    }
}
