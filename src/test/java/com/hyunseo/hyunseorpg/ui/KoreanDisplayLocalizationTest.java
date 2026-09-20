package com.hyunseo.hyunseorpg.ui;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KoreanDisplayLocalizationTest {
    private static final List<String> FARMING_IDS = List.of(
            "seed_corn", "seed_onion", "seed_chili", "seed_garlic",
            "crop_corn", "crop_onion", "crop_chili", "crop_garlic",
            "crop_corn_quality_proficient", "processed_corn_starch_normal",
            "processed_onion_concentrate_supreme", "processed_chili_extract_advanced",
            "processed_garlic_concentrate_basic", "abundance_essence",
            "vitality_stat_token", "satiety_stat_token", "abundance_stat_token");

    @Test
    void canonicalFarmingNamesAndLoreDoNotExposeEnglish() {
        for (String id : FARMING_IDS) {
            String name = KoreanDisplay.canonicalItemName(id).orElseThrow(() -> new AssertionError(id));
            assertFalse(containsEnglish(name), id + " name=" + name);
            KoreanDisplay.canonicalItemLore(id).ifPresent(lore ->
                    lore.forEach(line -> assertFalse(containsEnglish(line), id + " lore=" + line)));
        }
        assertEquals("\uC625\uC218\uC218 \uC804\uBD84 - \uC77C\uBC18", KoreanDisplay.itemId("processed_corn_starch_normal", null));
        assertEquals("\uCD5C\uACE0\uAE09", KoreanDisplay.quality("supreme"));
        assertEquals("\uB18D\uC0AC", KoreanDisplay.craftingCategory("farming"));
        assertEquals("\uC0BC\uC9C0\uCC3D", KoreanDisplay.itemId("vanilla:TRIDENT", null));
        assertEquals("\uD654\uC5FC\uAD6C", KoreanDisplay.itemId("vanilla:FIRE_CHARGE", null));
    }

    @Test
    void bundledFarmingPresentationAndMenuTitleAreKorean() {
        YamlConfiguration items = load("items.yml");
        for (String id : FARMING_IDS) {
            assertFalse(containsEnglish(items.getString("items." + id + ".display-name", "")), id);
            for (String line : items.getStringList("items." + id + ".lore")) {
                assertFalse(containsEnglish(line), id + " lore=" + line);
            }
        }
    }

    private boolean containsEnglish(String value) {
        return value != null && value.matches(".*[A-Za-z].*");
    }

    private YamlConfiguration load(String name) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            fail(exception);
            return new YamlConfiguration();
        }
    }
}
