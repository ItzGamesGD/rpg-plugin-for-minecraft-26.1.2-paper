package com.hyunseo.hyunseorpg.farming;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the Prompt 10 farming-to-brewing read contract without a server. */
class FarmingStage10BridgeContractTest {
    @Test
    void bridgeExposesOnlyReadMetadataAndNoRecipeOrEffectApi() throws Exception {
        List<String> methods = List.of(
                "isFarmingIngredient", "getCropId", "getProcessingItemId", "getQuality",
                "getQualityScore", "hasFarmingTag", "isAbundanceEssence",
                "isAbundanceEssenceUsableFor", "getFarmingDataVersion");
        for (String name : methods) {
            Method method = switch (name) {
                case "isFarmingIngredient", "isAbundanceEssence" ->
                        FarmingItemBridge.class.getMethod(name, ItemStack.class);
                case "getCropId", "getProcessingItemId", "getQuality", "getQualityScore", "getFarmingDataVersion" ->
                        FarmingItemBridge.class.getMethod(name, ItemStack.class);
                case "hasFarmingTag" -> FarmingItemBridge.class.getMethod(name, ItemStack.class, String.class);
                case "isAbundanceEssenceUsableFor" ->
                        FarmingItemBridge.class.getMethod(name, ItemStack.class, String.class);
                default -> throw new IllegalStateException(name);
            };
            assertNotNull(method, name);
        }
        assertTrue(List.of(FarmingItemBridge.class.getMethods()).stream()
                .noneMatch(method -> method.getName().equals("craft") || method.getName().equals("consume")));
    }

    @Test
    void essenceHasStableTagAndOnlyDeclaredUsageTargets() {
        var items = load("items.yml");
        var essence = load("farming/essence.yml");
        List<String> itemTags = items.getStringList("items.abundance_essence.tags");
        assertTrue(itemTags.contains("farming:abundance_essence"));
        assertTrue(itemTags.contains("alchemy-catalyst"));
        assertFalse(itemTags.contains("alchemy"));
        assertEquals(List.of("alchemy", "stat-token", "elemental-equipment", "endgame-equipment"),
                essence.getStringList("usage-tags"));
        assertEquals("abundance_essence", essence.getString("result-item-id"));
        assertTrue(essence.getBoolean("tradable"));
        assertTrue(!essence.getBoolean("sellable"));
    }

    private org.bukkit.configuration.file.YamlConfiguration load(String resource) {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
