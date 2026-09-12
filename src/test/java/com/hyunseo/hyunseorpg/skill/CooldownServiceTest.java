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
    @Test void normalModeStillEnforcesCooldowns() {
        var service = new CooldownService();
        var player = UUID.randomUUID();
        service.startCooldownTicks(player, "test", 240);
        assertTrue(service.isOnCooldown(player, "test"));
    }
}
