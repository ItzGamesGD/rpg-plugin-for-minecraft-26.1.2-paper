package com.hyunseo.hyunseorpg.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ConfigMigrationPyramidPhaseTest {
    @Test
    void pushPillarsMigrateToPillarOnlyRestorePhase() {
        assertEquals("pyramid_pillar_restore",
                ConfigMigrationService.canonicalPyramidPhase("pyramid_push_pillars"));
    }

    @Test
    void nonPillarComponentsDoNotAcquirePillarRestorePhase() {
        assertEquals("", ConfigMigrationService.canonicalPyramidPhase("pyramid_room_reveal"));
    }
}
