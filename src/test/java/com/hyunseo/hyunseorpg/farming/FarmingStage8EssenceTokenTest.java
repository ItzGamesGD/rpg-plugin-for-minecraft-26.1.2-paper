package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the Stage 8 configuration boundary without requiring a server. */
class FarmingStage8EssenceTokenTest {
    @Test
    void essenceIsExpertOnlyAndPointBased() {
        YamlConfiguration essence = load("farming/essence.yml");
        assertTrue(essence.getBoolean("enabled"));
        assertEquals(2, essence.getInt("schema-version"));
        assertEquals("abundance_essence", essence.getString("result-item-id"));
        assertEquals("expert", essence.getString("unlock-stage"));
        assertTrue(essence.getLong("required-abundance-points") > 0L);
        assertEquals(1, essence.getInt("result-count"));
        assertEquals(64, essence.getInt("max-stack"));
        assertTrue(essence.getBoolean("tradable"));
        assertFalse(essence.getBoolean("sellable"));
        assertEquals(List.of("alchemy", "stat-token", "elemental-equipment", "endgame-equipment"),
                essence.getStringList("usage-tags"));
        assertFalse(essence.isSet("item-id"));
        assertFalse(essence.isSet("required-farming-stage"));
        assertFalse(essence.isSet("output-amount"));
        assertFalse(essence.isSet("tradeable"));
        assertFalse(essence.isSet("material-tag"));
        assertFalse(essence.isSet("required-material-amount"));

        YamlConfiguration crafting = load("crafting.yml");
        assertEquals("essence", crafting.getString("crafting-recipes.abundance_essence.farming-type"));
        assertEquals("expert", crafting.getString("crafting-recipes.abundance_essence.required-farming-stage"));
        assertEquals(essence.getLong("required-abundance-points"),
                crafting.getLong("crafting-recipes.abundance_essence.required-abundance-points"));
        assertFalse(crafting.isSet("crafting-recipes.abundance_essence.inputs"));
    }

    @Test
    void directHarvestHasAConfigurableNonZeroPlaceholder() {
        YamlConfiguration harvest = load("farming/harvest.yml");
        assertTrue(harvest.getLong("direct.abundance-points", 0L) > 0L);
    }

    @Test
    void allSupremeMaterialsShareTheSameTag() {
        YamlConfiguration items = load("items.yml");
        for (String id : List.of(
                "crop_corn_quality_supreme", "crop_onion_quality_supreme",
                "crop_chili_quality_supreme", "crop_garlic_quality_supreme",
                "processed_corn_starch_supreme", "processed_onion_concentrate_supreme",
                "processed_chili_extract_supreme", "processed_garlic_concentrate_supreme")) {
            assertTrue(items.getStringList("items." + id + ".tags").contains("farming-supreme-material"), id);
        }
    }

    @Test
    void statTokensHaveCanonicalItemsAndExplicitLimits() {
        YamlConfiguration tokens = load("farming/stat_tokens.yml");
        YamlConfiguration items = load("items.yml");
        for (String token : List.of("vitality", "satiety", "abundance")) {
            String itemId = tokens.getString("tokens." + token + ".item-id", "");
            assertFalse(itemId.isBlank(), token);
            assertTrue(items.isConfigurationSection("items." + itemId), itemId);
            assertTrue(tokens.getInt("tokens." + token + ".maximum-uses", 0) > 0, token);
            assertTrue(tokens.getBoolean("tokens." + token + ".account-bound", false), token);
        }
    }

    private YamlConfiguration load(String resource) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, resource);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
