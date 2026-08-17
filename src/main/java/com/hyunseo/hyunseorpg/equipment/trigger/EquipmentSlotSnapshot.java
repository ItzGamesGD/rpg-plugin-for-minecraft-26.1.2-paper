package com.hyunseo.hyunseorpg.equipment.trigger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable view of the equipment slots that were relevant when a trigger was
 * created. Slot scope is therefore never inferred from ItemStack similarity.
 */
public record EquipmentSlotSnapshot(
        ItemStack mainHand,
        ItemStack offHand,
        ItemStack head,
        ItemStack chest,
        ItemStack legs,
        ItemStack feet
) {
    public static EquipmentSlotSnapshot from(Player player) {
        if (player == null) return new EquipmentSlotSnapshot(null, null, null, null, null, null);
        var inventory = player.getInventory();
        return new EquipmentSlotSnapshot(
                copy(inventory.getItemInMainHand()),
                copy(inventory.getItemInOffHand()),
                copy(inventory.getHelmet()),
                copy(inventory.getChestplate()),
                copy(inventory.getLeggings()),
                copy(inventory.getBoots())
        );
    }

    public List<ItemStack> sources(SourceScope scope) {
        return switch (scope) {
            case MAIN_HAND -> items(mainHand);
            case OFF_HAND -> items(offHand);
            case HEAD -> items(head);
            case CHEST -> items(chest);
            case LEGS -> items(legs);
            case FEET -> items(feet);
            case ARMOR -> items(head, chest, legs, feet);
            case HELD_ITEMS -> items(mainHand, offHand);
            case ALL_EQUIPPED -> items(mainHand, offHand, head, chest, legs, feet);
            case TRIGGERING_ITEM -> List.of();
        };
    }

    private static List<ItemStack> items(ItemStack... candidates) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : candidates) {
            if (item != null && !item.getType().isAir()) result.add(item);
        }
        return List.copyOf(result);
    }

    private static ItemStack copy(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }
}
