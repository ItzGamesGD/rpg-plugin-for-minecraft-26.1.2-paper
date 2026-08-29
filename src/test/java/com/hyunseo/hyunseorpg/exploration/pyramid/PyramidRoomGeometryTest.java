package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import org.junit.jupiter.api.Test;
import org.bukkit.Material;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PyramidRoomGeometryTest {
    @Test
    void roomAndEveryActualShaftLayerUseOneGeometryContract() {
        PyramidRoomGeometry geometry = PyramidRoomGeometry.of(
                new PyramidBlockPosition(10, 40, 20), 4, 4,
                new StructureBounds(0, 58, 0, 20, 80, 40));

        assertTrue(geometry.protectedBlock(10, 39, 20)); // floor
        assertTrue(geometry.protectedBlock(6, 42, 20));  // wall
        assertTrue(geometry.protectedBlock(10, 44, 20)); // ceiling / shaft bottom
        for (int y = 44; y <= 57; y++) {
            assertTrue(geometry.containsShaft(9, y, 21), "shaft layer " + y);
        }
        assertFalse(geometry.containsShaft(8, 50, 20));
        assertFalse(geometry.containsShaft(10, 58, 20));
        assertFalse(geometry.protectedBlock(15, 42, 20));
    }

    @Test
    void roomMutationCannotResealTheThreeByThreeShaftOpening() {
        PyramidRoomGeometry geometry = PyramidRoomGeometry.of(
                new PyramidBlockPosition(10, 40, 20), 4, 4,
                new StructureBounds(0, 58, 0, 20, 80, 40));

        int openings = 0;
        for (int x = 6; x <= 14; x++) {
            for (int z = 16; z <= 24; z++) {
                Material material = PyramidRoomService.roomCeilingMaterial(geometry, x, z);
                if (material == Material.AIR) openings++;
            }
        }
        assertTrue(openings == 9, "the room ceiling must retain the complete 3x3 shaft");
        assertTrue(PyramidRoomService.roomCeilingMaterial(geometry, 6, 16)
                == Material.CHISELED_SANDSTONE);
    }
}
