package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.detection.DeterministicStructureSelector;
import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureCandidate;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.StructureVariantDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class DeterministicStructureSelectorTest {
    @Test
    void idAndVariantDoNotReroll() {
        UUID world = UUID.randomUUID();
        StructureCandidate candidate = new StructureCandidate(world, "minecraft:swamp_hut",
                new StructureBounds(1, 2, 3, 20, 15, 22), new StructureAnchor(world, 10, 8, 12));
        DeterministicStructureSelector selector = new DeterministicStructureSelector();
        UUID first = selector.stableId(candidate);
        UUID second = selector.stableId(candidate);
        assertEquals(first, second);
        ExplorationStructureDefinition definition = new ExplorationStructureDefinition("swamp_hut", "minecraft:swamp_hut",
                true, 1.0, 32, 64, 100,
                List.of(new StructureVariantDefinition("a", 1, true, List.of()),
                        new StructureVariantDefinition("b", 2, true, List.of())));
        assertEquals(selector.chooseVariant(first, definition), selector.chooseVariant(first, definition));
        assertTrue(selector.selected(first, 1.0));
        assertFalse(selector.selected(first, 0.0));
    }
}
