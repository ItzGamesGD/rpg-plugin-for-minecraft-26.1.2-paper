package com.hyunseo.hyunseorpg.alchemy.potion;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Creates only registry-backed production potion items with the canonical PDC contract. */
public final class PotionFactory {
    private final PotionRegistry potions;
    private final RPGItemService items;
    private final PotionPdcContract<ItemStack> pdc;
    private final int dataVersion;

    public PotionFactory(PotionRegistry potions, RPGItemService items,
                         PotionPdcContract<ItemStack> pdc, int dataVersion) {
        this.potions = potions;
        this.items = items;
        this.pdc = pdc;
        this.dataVersion = dataVersion;
    }

    public Optional<ItemStack> create(String rawPotionId, int amount) {
        PotionDefinition definition = potions.find(rawPotionId).orElse(null);
        if (definition == null || !definition.enabled()) return Optional.empty();
        return create(definition, amount);
    }

    public Optional<ItemStack> create(PotionDefinition definition, int amount) {
        if (definition == null || !definition.enabled() || amount < 1) return Optional.empty();
        ItemStack item = items.create(definition.outputItemId(), amount).orElse(null);
        if (item == null) return Optional.empty();
        pdc.write(item, definition, "", dataVersion);
        return Optional.of(item);
    }
}
