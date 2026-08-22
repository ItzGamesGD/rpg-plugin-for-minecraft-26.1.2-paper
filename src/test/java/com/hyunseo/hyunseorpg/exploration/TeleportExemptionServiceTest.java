package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.runtime.TeleportExemptionService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TeleportExemptionServiceTest {
    @Test
    void exemptionCoversTheImmediateHeartbeatAfterTeleport() {
        TeleportExemptionService service = new TeleportExemptionService();
        UUID structure = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        service.exempt(structure, player, 100L, 60L);

        assertTrue(service.isExempt(structure, player, 100L));
        assertTrue(service.isExempt(structure, player, 160L));
        assertFalse(service.isExempt(structure, player, 161L));
    }

    @Test
    void exemptionIsScopedToStructureAndPlayer() {
        TeleportExemptionService service = new TeleportExemptionService();
        UUID structure = UUID.randomUUID();
        UUID otherStructure = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();

        service.exempt(structure, player, 10L, 60L);

        assertTrue(service.isExempt(structure, player, 20L));
        assertFalse(service.isExempt(otherStructure, player, 20L));
        assertFalse(service.isExempt(structure, otherPlayer, 20L));
    }
}
