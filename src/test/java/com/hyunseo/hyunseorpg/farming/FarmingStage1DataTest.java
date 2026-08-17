package com.hyunseo.hyunseorpg.farming;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage1DataTest {
    @Test
    void stageOneRegistersFourCanonicalCropsAndSeparateFiles() {
        YamlConfiguration crops = load("farming/crops.yml");
        YamlConfiguration growth = load("farming/growth.yml");
        YamlConfiguration items = load("items.yml");

        assertTrue(crops.getBoolean("enabled"));
        assertTrue(growth.getBoolean("enabled"));
        for (String id : new String[]{"corn", "onion", "chili", "garlic"}) {
            assertTrue(crops.isConfigurationSection("crops." + id), id);
            assertEquals(id.equals("corn"), crops.getBoolean("crops." + id + ".two-block"), id);
            assertNotNull(Material.matchMaterial(crops.getString("crops." + id + ".display-block")));
            assertTrue(growth.getLong("crops." + id + ".seconds-per-stage") > 0L, id);
            assertTrue(items.isConfigurationSection("items.seed_" + id), "seed_" + id);
            assertTrue(items.isConfigurationSection("items.crop_" + id), "crop_" + id);
        }
        assertEquals(1, crops.getInt("schema-version"));
        assertEquals(1, growth.getInt("schema-version"));
        assertFalse(growth.getBoolean("debug.events"));
    }

    @Test
    void cropIndexRejectsDuplicateCanonicalPositionAndUsesSameChunkKey() {
        UUID world = UUID.randomUUID();
        CropPosition position = new CropPosition(world, 31, 64, -1);
        CropInstance first = new CropInstance("corn", position, 0, 1L, 2L, 1);
        CropInstance duplicate = new CropInstance("onion", position, 0, 1L, 2L, 1);
        CropIndex index = new CropIndex();

        assertTrue(index.register(first));
        assertFalse(index.register(duplicate));
        assertEquals(1, index.getChunk(world, 1, -1).size());
        assertTrue(index.findByBlock(position).isPresent());
        assertEquals(1, index.remove(position).orElseThrow().position().x() / 31);
        assertEquals(0, index.cropCount());
    }

    @Test
    void upperRepresentationResolvesToCanonicalLowerPosition() {
        UUID world = UUID.randomUUID();
        CropPosition lower = new CropPosition(world, 8, 64, 8);
        CropPosition upper = new CropPosition(world, 8, 65, 8);
        CropIndex index = new CropIndex();
        CropInstance corn = new CropInstance("corn", lower, 1, 1L, 2L, 1);
        CropRepresentationResolver resolver = new CropRepresentationResolver(index,
                id -> Optional.of(new CropDefinition(id, "seed_" + id, "crop_" + id,
                        Material.WHEAT, 4, "corn".equals(id), Set.of(Material.FARMLAND), 60L, 1, true)));

        assertTrue(index.register(corn));
        assertEquals(lower, resolver.find(upper).orElseThrow().position());
    }

    @Test
    void singleBlockCropDoesNotResolveItsUpperBlockAsRepresentation() {
        UUID world = UUID.randomUUID();
        CropIndex index = new CropIndex();
        CropRepresentationResolver resolver = new CropRepresentationResolver(index,
                id -> Optional.of(new CropDefinition(id, "seed_" + id, "crop_" + id,
                        Material.CARROTS, 4, false, Set.of(Material.FARMLAND), 60L, 1, true)));

        for (String id : new String[]{"onion", "chili", "garlic"}) {
            int x = 8 + index.cropCount() * 2;
            CropPosition lower = new CropPosition(world, x, 64, 8);
            CropPosition upper = new CropPosition(world, x, 65, 8);
            assertTrue(index.register(new CropInstance(id, lower, 1, 1L, 2L, 1)));
            assertTrue(resolver.find(upper).isEmpty(), id);
            assertEquals(lower, resolver.find(lower).orElseThrow().position(), id);
        }
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
