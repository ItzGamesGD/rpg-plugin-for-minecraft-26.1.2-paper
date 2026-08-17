package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static migration evidence only. Paper-side mutations remain live-server evidence.
 */
class AlchemyScaffoldMigrationCompletenessTest {
    private static final Map<String, String> EFFECT_HANDLERS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("effect_vampire", "VampirismEffectHandler"),
            Map.entry("effect_berserk", "BerserkEffectHandler"),
            Map.entry("effect_corrosion", "CorrosionEffectHandler"),
            Map.entry("effect_frostbite", "FrostbiteEffectHandler"),
            Map.entry("effect_shock", "ShockEffectHandler"),
            Map.entry("effect_bleed", "BleedEffectHandler"),
            Map.entry("effect_vulnerability", "VulnerabilityEffectHandler"),
            Map.entry("effect_necrosis", "NecrosisEffectHandler")));

    private static final Map<String, String> HANDLER_IDS = Map.ofEntries(
            Map.entry("effect_vampire", "vampirism"),
            Map.entry("effect_berserk", "berserk"),
            Map.entry("effect_corrosion", "corrosion"),
            Map.entry("effect_frostbite", "frostbite"),
            Map.entry("effect_shock", "shock"),
            Map.entry("effect_bleed", "bleed"),
            Map.entry("effect_vulnerability", "vulnerability"),
            Map.entry("effect_necrosis", "necrosis"));

    private static final List<String> POTIONS = List.of(
            "potion_vampire", "potion_berserk", "potion_corrosion", "potion_frostbite",
            "potion_shock", "potion_bleed", "potion_vulnerability", "potion_necrosis");

    @Test
    void currentProductionDefinitionsHaveConcreteJavaAndCanonicalCraftingContracts() {
        YamlConfiguration effects = load("alchemy/effects.yml");
        YamlConfiguration potions = load("alchemy/potions.yml");
        YamlConfiguration crafting = load("crafting.yml");
        ConfigurationSection recipes = crafting.getConfigurationSection("crafting-recipes");
        assertNotNull(recipes);

        for (Map.Entry<String, String> entry : EFFECT_HANDLERS.entrySet()) {
            String effectId = entry.getKey();
            String path = "effects." + effectId;
            assertTrue(effects.getBoolean(path + ".enabled", false), effectId);
            assertEquals(HANDLER_IDS.get(effectId), effects.getString(path + ".handler-id", ""), effectId);
            assertConcreteHandler(entry.getValue());
        }

        for (String potionId : POTIONS) {
            String potionPath = "potions." + potionId;
            assertTrue(potions.getBoolean(potionPath + ".enabled", false), potionId);
            assertEquals(potionId, potions.getString(potionPath + ".output-item-id"), potionId);
            assertEquals("effect_" + potionId.substring("potion_".length()),
                    potions.getString(potionPath + ".effect-id"), potionId);

            String recipePath = potionId;
            assertEquals("alchemy_potion", recipes.getString(recipePath + ".farming-type"), potionId);
            assertEquals(potionId, recipes.getString(recipePath + ".output.item-id"), potionId);
            assertNotNull(recipes.getConfigurationSection(recipePath + ".inputs"), potionId);
        }
    }

    @Test
    void executableEntryPointClassesArePresent() {
        for (String name : List.of(
                "com.hyunseo.hyunseorpg.alchemy.EffectService",
                "com.hyunseo.hyunseorpg.alchemy.ProductionEffectListener",
                "com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionUseListener",
                "com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionUseService",
                "com.hyunseo.hyunseorpg.alchemy.potion.PotionFactory",
                "com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystApplicationService",
                "com.hyunseo.hyunseorpg.alchemy.catalyst.BoundedSpecialCatalystExecutionService")) {
            assertTrue(loadClass(name), name);
        }
    }

    private void assertConcreteHandler(String simpleName) {
        assertTrue(loadClass("com.hyunseo.hyunseorpg.alchemy." + simpleName), simpleName);
    }

    private boolean loadClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
