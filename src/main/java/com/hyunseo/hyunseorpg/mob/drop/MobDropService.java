package com.hyunseo.hyunseorpg.mob.drop;

import com.hyunseo.hyunseorpg.mob.MobData;
import com.hyunseo.hyunseorpg.mob.MobService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.mob.ElementalFragmentPolicy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class MobDropService {
    private final NamespacedKey itemIdKey;
    private final MobService mobService;
    private final MobDropRegistry dropRegistry;
    private final RPGItemService itemService;
    private final InventoryDeliveryService inventoryDeliveryService;
    private final ElementalFragmentPolicy elementalPolicy;

    public MobDropService(JavaPlugin plugin, MobService mobService, MobDropRegistry dropRegistry,
                          RPGItemService itemService, InventoryDeliveryService inventoryDeliveryService,
                          ElementalFragmentPolicy elementalPolicy) {
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.mobService = mobService;
        this.dropRegistry = dropRegistry;
        this.itemService = itemService;
        this.inventoryDeliveryService = inventoryDeliveryService;
        this.elementalPolicy = elementalPolicy;
    }

    public void dropRewards(Player player, LivingEntity defeated) {
        Optional<MobData> mobData = mobService.getMobData(defeated);
        if (mobData.isEmpty() || mobData.get().dropTableId().isBlank()) {
            return;
        }

        Optional<MobDropTable> dropTable = dropRegistry.get(mobData.get().dropTableId());
        if (dropTable.isEmpty()) {
            return;
        }

        for (MobDropEntry entry : dropTable.get().entries()) {
            if (entry.bossOnly() && !mobService.isBossMob(defeated)) {
                continue;
            }
            if (ElementalFragmentPolicy.isElementalFragment(entry.itemId())
                    && !elementalPolicy.allows(player, defeated, entry.itemId())) continue;
            if (ThreadLocalRandom.current().nextDouble() > entry.chance()) {
                continue;
            }

            int amount = ThreadLocalRandom.current().nextInt(entry.minAmount(), entry.maxAmount() + 1);
            inventoryDeliveryService.giveOrDiscard(player, createDropItem(entry, amount));
        }
    }

    private ItemStack createDropItem(MobDropEntry entry, int amount) {
        Optional<ItemStack> registeredItem = itemService.create(entry.itemId(), amount);
        if (registeredItem.isPresent()) {
            return registeredItem.get();
        }
        ItemStack itemStack = new ItemStack(entry.material(), amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(entry.displayName(), NamedTextColor.AQUA));
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, entry.itemId());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
