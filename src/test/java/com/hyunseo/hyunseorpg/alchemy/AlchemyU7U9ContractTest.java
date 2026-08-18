package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.alchemy.catalyst.BoundedSpecialCatalystExecutionService;
import com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystDefinition;
import com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystExecution;
import com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiController;
import com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiControllerService;
import com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiSessionImpl;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlchemyU7U9ContractTest {
    @Test void productionCatalystsAreEnabledOnlyAfterRuntimeCompletionB() {
        YamlConfiguration yaml = load("alchemy/catalysts.yml");
        for (String id : new String[]{"redstone", "glowstone_dust", "gunpowder", "dragon_breath", "fermented_spider_eye", "sculk", "echo_shard", "slime", "wind_charge"}) {
            assertTrue(yaml.getBoolean("catalysts." + id + ".enabled", false), id);
            assertFalse(yaml.getString("catalysts." + id + ".vanilla-material", "").isBlank(), id);
        }
        assertEquals("SCULK_CATALYST", yaml.getString("catalysts.sculk.vanilla-material"));
        assertEquals("SPLASH", yaml.getString("catalysts.slime.delivery"));
        assertTrue(yaml.getBoolean("catalysts.fireball.enabled", false));
        assertEquals("fireball", yaml.getString("catalysts.fireball.item-id"));
        assertEquals("SPLASH", yaml.getString("catalysts.echo_shard.delivery"));
        assertEquals("SPLASH", yaml.getString("catalysts.slime.delivery"));
        assertEquals("SCULK_CATALYST", yaml.getString("catalysts.sculk.vanilla-material"));
        assertTrue(yaml.getStringList("catalysts.fermented_spider_eye.allowed-potions").contains("potion_vulnerability"));
    }

    @Test void specialExecutionIsBoundedDuplicateSafeAndWorldCancellable() {
        SpecialCatalystDefinition definition = new SpecialCatalystDefinition("sculk", true, SpecialCatalystDefinition.Kind.SCULK);
        SpecialCatalystRegistry registry = new SpecialCatalystRegistry() {
            public Optional<SpecialCatalystDefinition> find(String id) { return "sculk".equals(id) ? Optional.of(definition) : Optional.empty(); }
            public Map<String, SpecialCatalystDefinition> all() { return Map.of("sculk", definition); }
            public boolean reload() { return true; }
        };
        BoundedSpecialCatalystExecutionService service = new BoundedSpecialCatalystExecutionService(registry, 1);
        UUID execution = UUID.randomUUID(), world = UUID.randomUUID();
        var request = new SpecialCatalystExecution.Request(execution, "potion_test_speed", "sculk", world, Set.of());
        assertEquals(SpecialCatalystExecution.Result.STARTED, service.execute(request));
        assertEquals(SpecialCatalystExecution.Result.REJECTED_DUPLICATE, service.execute(request));
        service.cancelWorld(world, SpecialCatalystExecution.CancelReason.WORLD_UNLOAD);
        assertEquals(0, service.activeCount());
    }

    @Test void guiSessionReplacementAndUnsafeActionsAreBlocked() {
        AlchemyGuiControllerService controller = new AlchemyGuiControllerService();
        UUID player = UUID.randomUUID();
        AlchemyGuiSessionImpl session = controller.open(player);
        assertEquals(AlchemyGuiController.ClickResult.BLOCKED, controller.handleClick(session, AlchemyGuiController.ClickAction.SHIFT));
        assertEquals(AlchemyGuiController.ClickResult.ACCEPTED, controller.handleClick(session, AlchemyGuiController.ClickAction.NORMAL));
        AlchemyGuiSessionImpl replacement = controller.open(player);
        assertEquals(AlchemyGuiController.ClickResult.SESSION_CLOSED, controller.handleClick(session, AlchemyGuiController.ClickAction.NORMAL));
        assertEquals(AlchemyGuiController.ClickResult.ACCEPTED, controller.handleClick(replacement, AlchemyGuiController.ClickAction.NORMAL));
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
