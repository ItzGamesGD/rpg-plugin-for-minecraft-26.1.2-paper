package com.hyunseo.hyunseorpg.exploration.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitExplorationPortsTest {
    @Test
    void productionCompositionUsesRealPrimitivePorts() {
        ExplorationPorts ports = BukkitExplorationPorts.compose(null,
                (mobId, location, count, options) -> java.util.List.of(),
                (player, rewardId, amount, fallback, options) -> true,
                (world, bounds, entityType) -> 0);
        assertNotNull(ports.displays());
        assertNotSame(ExplorationPorts.DisplayPort.NOOP, ports.displays());
        assertNotSame(ExplorationPorts.InteractionPort.NOOP, ports.interactions());
        assertNotSame(ExplorationPorts.WorldMutationPort.NOOP, ports.worldMutations());
        assertNotSame(ExplorationPorts.TeleportPort.NOOP, ports.teleports());
    }

    @Test
    void vanillaRewardResolverAcceptsMinecraftAndLegacyNamespaces() {
        assertTrue(ExistingHyunseoRpgAdapters.isVanillaMaterialId("minecraft:nautilus_shell"));
        assertTrue(ExistingHyunseoRpgAdapters.isVanillaMaterialId("vanilla:nautilus_shell"));
        assertFalse(ExistingHyunseoRpgAdapters.isVanillaMaterialId("custom:nautilus_shell"));
    }
}
