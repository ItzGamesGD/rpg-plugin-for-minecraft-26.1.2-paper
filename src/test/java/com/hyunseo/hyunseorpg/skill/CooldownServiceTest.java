package com.hyunseo.hyunseorpg.skill;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CooldownServiceTest {
    @Test void testingModeBypassesBothMillisAndTicksWithoutAccumulatingEntries() {
        var service = new CooldownService(Clock.systemUTC(), true);
        var player = UUID.randomUUID();
        service.startCooldown(player, "solaris:judgment", 60000);
        service.startCooldownTicks(player, "thunder_gods_axe:strike", 240);
        assertFalse(service.isOnCooldown(player, "solaris:judgment"));
        assertFalse(service.isOnCooldown(player, "thunder_gods_axe:strike"));
        assertTrue(service.getRemainingCooldowns(player).isEmpty());
        assertTrue(service.getExpiresAt(player, "solaris:judgment").isEmpty());
    }

    @Test void dedicatedSpecialWeaponsRemainCooldownFreeEvenWhenGlobalTestingModeIsOff() {
        var service = new CooldownService(Clock.systemUTC(), false);
        var player = UUID.randomUUID();
        for (String id : new String[]{
                "special:poseidon_spear:signature",
                "thanatos:sentence",
                "thunder_gods_axe:strike",
                "solaris:judgment",
                "moonlit_afterglow:moon_flash"}) {
            service.startCooldown(player, id, 60000);
            assertFalse(service.isOnCooldown(player, id), id);
            assertTrue(service.getExpiresAt(player, id).isEmpty(), id);
        }
        assertTrue(service.getRemainingCooldowns(player).isEmpty());
    }

    @Test void normalModeStillEnforcesOrdinaryCooldowns() {
        var service = new CooldownService();
        var player = UUID.randomUUID();
        service.startCooldownTicks(player, "test", 240);
        assertTrue(service.isOnCooldown(player, "test"));
    }
}
