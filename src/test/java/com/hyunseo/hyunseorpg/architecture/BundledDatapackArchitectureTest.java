package com.hyunseo.hyunseorpg.architecture;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BundledDatapackArchitectureTest {
    private static final Path ROOT = Path.of("src/main/resources/datapack");

    @Test
    void packRootAndRequiredNativeTagResourcesAreComplete() throws Exception {
        assertTrue(Files.isDirectory(ROOT));
        Path metadata = ROOT.resolve("pack.mcmeta");
        assertTrue(Files.isRegularFile(metadata));
        var pack = JsonParser.parseReader(Files.newBufferedReader(metadata, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("pack");
        assertEquals(101, pack.get("max_format").getAsInt());
        assertNotNull(pack.get("min_format"));
        for (String name : List.of("in_enchanting_table", "tradeable", "on_random_loot", "treasure", "non_treasure")) {
            assertTrue(Files.isRegularFile(ROOT.resolve("data/minecraft/tags/enchantment/" + name + ".json")), name);
        }
        for (String name : List.of("swords", "axes", "pickaxes", "hoes", "excavation_tools", "bows",
                "crossbows", "fishing_rods", "elytra", "maces")) {
            assertTrue(Files.isRegularFile(ROOT.resolve("data/hyunseorpg/tags/item/" + name + ".json")), name);
        }
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "data/hyunseorpg/tags/enchantment/exclusive_set/bow_shift_left.json")));
    }

    @Test
    void bootstrapRegistersTheInJarRootWithPaperDiscoveryLifecycle() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/hyunseo/hyunseorpg/enchant/nativeapi/HyunseoRPGPluginBootstrap.java"));
        assertTrue(source.contains("LifecycleEvents.DATAPACK_DISCOVERY"));
        assertTrue(source.contains("getResource(\"/datapack\")"));
        assertTrue(source.contains("discoverPack(root.toURI()"));
        assertTrue(source.contains("autoEnableOnServerStart(true)"));
        assertTrue(source.indexOf("DATAPACK_DISCOVERY") < source.indexOf("RegistryEvents.ENCHANTMENT.compose()"));
    }
}
