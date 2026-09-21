package com.hyunseo.hyunseorpg.crafting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CraftingRecipeDataTest {
    @Test
    void canonicalRecipeNormalizesIdsAndPreservesCategory() {
        CraftingRecipeData data = new CraftingRecipeData(" Fire_Core ", true, CraftingCategory.MATERIALS,
                List.of(new CraftingRecipeData.Ingredient("vanilla:iron_ingot", 2)), " Basic_Upgrade_Stone ", 1);

        assertEquals("fire_core", data.id());
        assertEquals("basic_upgrade_stone", data.outputId());
        assertEquals("materials", data.categoryId());
        assertTrue(data.ingredients().getFirst().vanilla());
    }

    @Test
    void recipeRejectsInvalidAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new CraftingRecipeData.Ingredient("basic_upgrade_stone", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CraftingRecipeData("invalid", true, CraftingCategory.MATERIALS, List.of(), "output", 0));
    }

    @Test
    void categoryAliasesAreStable() {
        assertEquals(CraftingCategory.EQUIPMENT, CraftingCategory.fromConfig("weapon", null));
        assertEquals(CraftingCategory.SPECIAL, CraftingCategory.fromConfig("special-equipment", null));
        assertEquals(CraftingCategory.CONSUMABLES, CraftingCategory.fromConfig("consumable", null));
        assertFalse(CraftingCategory.fromConfig("missing", null) != null);
    }

}
