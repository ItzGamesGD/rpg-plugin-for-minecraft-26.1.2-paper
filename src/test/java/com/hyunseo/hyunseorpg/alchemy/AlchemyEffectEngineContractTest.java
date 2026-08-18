package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlchemyEffectEngineContractTest {
    @Test
    void productionEffectsAreCataloguedWithExplicitRuntimeActivation() {
        YamlConfiguration effects = load("alchemy/effects.yml");
        assertEquals(1, effects.getInt("schema-version"));
        assertTrue(effects.isConfigurationSection("effects.effect_test_speed"));
        for (String id : java.util.List.of("effect_vampire", "effect_berserk", "effect_corrosion",
                "effect_frostbite", "effect_shock", "effect_bleed", "effect_vulnerability", "effect_necrosis")) {
            assertTrue(effects.isConfigurationSection("effects." + id), "missing production effect " + id);
            assertTrue(effects.getBoolean("effects." + id + ".enabled"));
            assertEquals("IMPLEMENTED_RUNTIME", effects.getString("effects." + id + ".status"));
            assertFalse(effects.getString("effects." + id + ".handler-id", "").isBlank());
            assertEquals("BALANCE_PENDING", effects.getString("effects." + id + ".balance"));
        }
        assertFalse(effects.isConfigurationSection("potions"));
        assertFalse(effects.isConfigurationSection("recipes"));
    }

    @Test
    void shockPulseUsesConfiguredThreeSecondIntervalAndFortyTickRoot() {
        YamlConfiguration effects = load("alchemy/effects.yml");
        assertEquals(60, effects.getInt("effects.effect_shock.baseline.tick-interval"));
        assertEquals(1.0D, effects.getDouble("effects.effect_shock.baseline.damage"));
        assertEquals(20, effects.getInt("effects.effect_shock.baseline.stun-duration-ticks"));
    }

    @Test
    void effectSourcePreservesApplicationTypeAndIdentity() {
        UUID source = UUID.randomUUID();
        EffectSource value = new EffectSource(source, EffectSourceType.COMMAND, "Effect_Test_Speed");
        assertEquals(source, value.applicatorId());
        assertEquals(EffectSourceType.COMMAND, value.type());
        assertEquals("effect_test_speed", value.sourceId());
    }

    @Test
    void activeInstanceRefreshesWithoutChangingIdentityOrSource() {
        UUID target = UUID.randomUUID();
        EffectSource source = new EffectSource(null, EffectSourceType.OTHER, "test");
        CustomEffectDefinition definition = new CustomEffectDefinition("effect_test_speed", "테스트 속도", true,
                10, 20, 1, 1, EffectTargetPolicy.PLAYER, EffectStackPolicy.REFRESH_DURATION,
                true, false, false, "", java.util.List.of());
        ActiveEffectInstance instance = new ActiveEffectInstance(UUID.randomUUID(), target, definition, source, 20L, 1);
        UUID instanceId = instance.instanceId();
        instance.refresh(80L);
        assertEquals(instanceId, instance.instanceId());
        assertEquals(80L, instance.expiresAtTick());
        assertEquals(source, instance.source());
    }

    @Test
    void activeInstanceExpiryIsPureAndStackCountIsBounded() {
        UUID target = UUID.randomUUID();
        CustomEffectDefinition definition = new CustomEffectDefinition("effect_test_speed", "테스트 속도", true,
                10, 20, 1, 2, EffectTargetPolicy.PLAYER, EffectStackPolicy.ADD_STACK,
                true, false, false, "", java.util.List.of());
        ActiveEffectInstance instance = new ActiveEffectInstance(UUID.randomUUID(), target, definition,
                new EffectSource(null, EffectSourceType.OTHER, "test"), 20L, 1);

        assertFalse(instance.expired(19L));
        assertTrue(instance.expired(20L));
        assertEquals(1L, instance.remainingTicks(19L));
        instance.addStack();
        instance.addStack();
        assertEquals(2, instance.stacks());
    }

    @Test
    void effectDefinitionRejectsInvalidCoreValues() {
        assertThrows(IllegalArgumentException.class, () -> new CustomEffectDefinition(
                "Effect Test", "잘못된 효과", true, 0, 20, 0, 1,
                EffectTargetPolicy.PLAYER, EffectStackPolicy.IGNORE,
                true, false, false, "", java.util.List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CustomEffectDefinition(
                "effect_test_speed", "잘못된 효과", true, 0, 0, 0, 1,
                EffectTargetPolicy.PLAYER, EffectStackPolicy.IGNORE,
                true, false, false, "", java.util.List.of()));
    }

    @Test
    void handlerRegistrySupportsNormalizedReplacementAndSnapshotReads() {
        HandlerRegistry registry = new HandlerRegistry();
        Object handler = new Object();
        registry.register(" Test_Handler ", handler);

        assertTrue(registry.contains("test_handler"));
        assertSame(handler, registry.get("TEST_HANDLER").orElseThrow());
        assertEquals(1, registry.all().size());

        registry.unregister("TEST_HANDLER");
        assertFalse(registry.contains("test_handler"));
        assertThrows(IllegalArgumentException.class, () -> registry.register("", handler));
    }

    @Test
    void combatCoreIdsAreStableAndAdapterDoesNotInventDamageValues() {
        assertEquals("silence", CoreEffectIds.SILENCE);
        assertEquals("root", CoreEffectIds.ROOT);
        assertEquals("stun", CoreEffectIds.STUN);
        PaperAlchemyCombatAdapter adapter = new PaperAlchemyCombatAdapter(null);
        assertFalse(adapter.blocksAnyActiveSkill(UUID.randomUUID()));
        assertFalse(adapter.blocksAnyMovement(UUID.randomUUID()));
        assertFalse(adapter.blocksAnyAttack(UUID.randomUUID()));
    }

    @Test
    void attributionDeduplicatesBySourceTargetAndEffectAndPreservesDamageKind() {
        CombatEffectAttributionService attribution = new CombatEffectAttributionService();
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        attribution.recordApplication(source, target, " VULNERABILITY ", 10L);
        attribution.recordContribution(source, target, "vulnerability",
                CombatEffectHandler.DamageKind.DOT, 2.5D);
        assertEquals(1, attribution.contributions(source, target, "vulnerability").size());
        assertEquals(CombatEffectHandler.DamageKind.DOT,
                attribution.contributions(source, target, "vulnerability").get(0).kind());
        attribution.clear(source, target, "vulnerability");
        assertTrue(attribution.contributions(source, target, "vulnerability").isEmpty());
    }

    private YamlConfiguration load(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "missing resource: " + path);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
