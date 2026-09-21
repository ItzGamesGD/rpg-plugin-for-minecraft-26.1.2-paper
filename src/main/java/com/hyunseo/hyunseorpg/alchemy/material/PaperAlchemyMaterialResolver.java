package com.hyunseo.hyunseorpg.alchemy.material;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** Matches explicit vanilla identities or canonical RPG item PDC identities. */
public final class PaperAlchemyMaterialResolver implements AlchemyMaterialResolver {
    private final RPGItemService items;

    public PaperAlchemyMaterialResolver(RPGItemService items) { this.items = items; }

    @Override public boolean matches(ItemStack item, String rawId) {
        if (item == null || item.getType().isAir() || rawId == null) return false;
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        if (id.startsWith("vanilla:")) {
            Material material = Material.matchMaterial(id.substring("vanilla:".length()));
            return material != null && item.getType() == material && items.getItemId(item).isEmpty();
        }
        return items.isItem(item, id);
    }

    @Override public int count(List<ItemStack> inputs, String ingredientId) {
        return inputs == null ? 0 : inputs.stream().filter(item -> matches(item, ingredientId))
                .mapToInt(ItemStack::getAmount).sum();
    }
}
