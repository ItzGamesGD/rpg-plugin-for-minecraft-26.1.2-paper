package com.hyunseo.hyunseorpg.crafting;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.ui.KoreanDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single renderer for every recipe shown in the player GUI and layout editor.
 * It receives only the canonical recipe object; recipe IDs are never special-cased here.
 */
public final class CraftingRecipeRenderer {
    private final RPGItemService items;
    private final CraftingTransactionService transactions;
    private final NamespacedKey recipeKey;

    public CraftingRecipeRenderer(JavaPlugin plugin, RPGItemService items,
                                  CraftingTransactionService transactions) {
        this.items = items;
        this.transactions = transactions;
        this.recipeKey = new NamespacedKey(plugin, "crafting_recipe_id");
    }

    public ItemStack render(Player player, CraftingRecipeData recipe) {
        ItemStack result = createOutput(player, recipe);
        if (result == null) result = new ItemStack(Material.PAPER);
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return result;

        meta.getPersistentDataContainer().set(recipeKey, PersistentDataType.STRING, recipe.id());
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.text("\uACB0\uACFC: " + KoreanDisplay.itemId(recipe.outputId(), items)
                + " x" + recipe.outputAmount(), NamedTextColor.GRAY));
        lore.add(Component.text("\uC7AC\uB8CC:", NamedTextColor.GRAY));
        for (CraftingRecipeData.Ingredient ingredient : recipe.ingredients()) {
            int owned = transactions.ingredientCount(player, ingredient);
            NamedTextColor color = owned >= ingredient.amount() ? NamedTextColor.GREEN : NamedTextColor.RED;
            String label = ingredient.tag()
                    ? "\uD0DC\uADF8: " + KoreanDisplay.tag(ingredient.tagId())
                    : KoreanDisplay.itemId(ingredient.id(), items);
            lore.add(Component.text("- " + label + ": " + owned + "/" + ingredient.amount(), color));
        }
        lore.add(Component.text("\uC88C\uD074\uB9AD: 1\uD68C \uC81C\uC791", NamedTextColor.YELLOW));
        lore.add(Component.text("Shift + \uC88C\uD074\uB9AD: \uAC00\uB2A5\uD55C \uCD5C\uB300 \uC81C\uC791", NamedTextColor.YELLOW));
        meta.lore(lore);
        result.setItemMeta(meta);
        return result;
    }

    public String recipeId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(recipeKey, PersistentDataType.STRING);
    }

    private ItemStack createOutput(Player player, CraftingRecipeData recipe) {
        if (recipe.outputId().startsWith("vanilla:")) {
            Material material = Material.matchMaterial(recipe.outputId().substring("vanilla:".length())
                    .toUpperCase(Locale.ROOT));
            return material == null ? null : new ItemStack(material, recipe.outputAmount());
        }
        return items.create(recipe.outputId(), recipe.outputAmount()).orElse(null);
    }
}
