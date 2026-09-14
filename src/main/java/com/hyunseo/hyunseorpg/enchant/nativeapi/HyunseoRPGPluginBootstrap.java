package com.hyunseo.hyunseorpg.enchant.nativeapi;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

/** Registers native enchantments during Paper's writable-registry bootstrap phase. */
@SuppressWarnings("UnstableApiUsage")
public final class HyunseoRPGPluginBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
            for (NativeEnchantDefinition definition : NativeEnchantDefinitions.ALL) {
                TypedKey<org.bukkit.enchantments.Enchantment> key = TypedKey.create(
                        RegistryKey.ENCHANTMENT, Key.key(NativeEnchantDefinitions.NAMESPACE, definition.id()));
                event.registry().register(key, builder -> builder
                        .description(Component.text(definition.displayName()))
                        .supportedItems(event.getOrCreateTag(TagKey.create(RegistryKey.ITEM,
                                Key.key("minecraft", definition.supportedItemTag()))))
                        .weight(definition.weight())
                        .maxLevel(definition.maxLevel())
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                definition.minimumBaseCost(), definition.minimumPerLevelCost()))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                definition.maximumBaseCost(), definition.maximumPerLevelCost()))
                        .anvilCost(definition.anvilCost())
                        .activeSlots(EquipmentSlotGroup.ANY));
            }
        }));
    }
}
