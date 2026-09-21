package com.hyunseo.hyunseorpg.alchemy.brewing;

import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionPdcContract;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Resolves visible Brewing Stand stacks to canonical registry identifiers. */
public final class BrewingInputResolver {
    private final PotionPdcContract<ItemStack> potionPdc;
    private final RPGItemService items;
    private final CatalystRegistry catalysts;
    private final SpecialCatalystRegistry specialCatalysts;

    public BrewingInputResolver(PotionPdcContract<ItemStack> potionPdc, RPGItemService items,
                                CatalystRegistry catalysts, SpecialCatalystRegistry specialCatalysts) {
        this.potionPdc = potionPdc;
        this.items = items;
        this.catalysts = catalysts;
        this.specialCatalysts = specialCatalysts;
    }

    public String resolveBase(ItemStack item) {
        if (empty(item)) return "";
        String potionId = normalize(potionPdc.readPotionId(item));
        return potionId.isBlank() ? resolveItem(item) : potionId;
    }

    public String resolveIngredient(ItemStack item) {
        if (empty(item)) return "";
        String itemId = items.getItemId(item).orElse("");
        if (!itemId.isBlank()) {
            return catalysts.all().values().stream()
                    .filter(definition -> definition.enabled() && definition.itemId().equals(itemId))
                    .map(definition -> "catalyst:" + definition.catalystId()).findFirst().orElse(itemId);
        }
        String material = item.getType().name();
        String catalyst = catalysts.all().values().stream()
                .filter(definition -> definition.enabled() && definition.materialId().equalsIgnoreCase(material))
                .map(definition -> definition.catalystId()).findFirst()
                .or(() -> specialCatalysts.findByItem(item).filter(definition -> definition.enabled())
                        .map(definition -> definition.catalystId()))
                .orElse("");
        return catalyst.isBlank() ? "vanilla:" + material.toLowerCase(Locale.ROOT) : "catalyst:" + catalyst;
    }

    private String resolveItem(ItemStack item) {
        return items.getItemId(item).orElseGet(() ->
                "vanilla:" + item.getType().name().toLowerCase(Locale.ROOT));
    }

    private static boolean empty(ItemStack item) { return item == null || item.getType().isAir(); }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
