package com.hyunseo.hyunseorpg.alchemy.material;

import org.bukkit.inventory.ItemStack;
import java.util.List;

/** U6 matching boundary; it never falls back from a custom item to Material-only identity. */
public interface AlchemyMaterialResolver {
    boolean matches(ItemStack item, String ingredientId);
    int count(List<ItemStack> inputs, String ingredientId);
}
