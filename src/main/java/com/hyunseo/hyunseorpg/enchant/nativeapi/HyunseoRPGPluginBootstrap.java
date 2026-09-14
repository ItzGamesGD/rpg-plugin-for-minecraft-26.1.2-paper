package com.hyunseo.hyunseorpg.enchant.nativeapi;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

/** Registers native enchantments during Paper's writable-registry bootstrap phase. */
@SuppressWarnings("UnstableApiUsage")
public final class HyunseoRPGPluginBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        // DATAPACK_DISCOVERY runs while Paper builds the server data pack repository. Auto-enabling
        // the in-JAR root makes its item/enchantment tags available to the later registry compose pass.
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                var root = Objects.requireNonNull(HyunseoRPGPluginBootstrap.class.getResource("/datapack"),
                        "Missing bundled /datapack resource root");
                var discovered = event.registrar().discoverPack(root.toURI(), "native-enchantments",
                        configurer -> configurer.autoEnableOnServerStart(true));
                if (discovered == null) throw new IllegalStateException("Paper rejected bundled HyunseoRPG datapack");
            } catch (URISyntaxException | IOException exception) {
                throw new IllegalStateException("Unable to discover bundled HyunseoRPG datapack", exception);
            }
        });
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
            for (NativeEnchantDefinition definition : NativeEnchantDefinitions.ALL) {
                TypedKey<org.bukkit.enchantments.Enchantment> key = TypedKey.create(
                        RegistryKey.ENCHANTMENT, Key.key(NativeEnchantDefinitions.NAMESPACE, definition.id()));
                String[] supported = definition.supportedItemTag().split(":", 2);
                event.registry().register(key, builder -> {
                    builder.description(Component.text(definition.displayName()))
                        .supportedItems(event.getOrCreateTag(TagKey.create(RegistryKey.ITEM,
                                Key.key(supported[0], supported[1]))))
                        .weight(definition.weight())
                        .maxLevel(definition.maxLevel())
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                definition.minimumBaseCost(), definition.minimumPerLevelCost()))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                definition.maximumBaseCost(), definition.maximumPerLevelCost()))
                        .anvilCost(definition.anvilCost())
                        .activeSlots(EquipmentSlotGroup.ANY);
                    if (!definition.exclusiveSetTag().isBlank()) {
                        builder.exclusiveWith(event.getOrCreateTag(TagKey.create(RegistryKey.ENCHANTMENT,
                                Key.key(NativeEnchantDefinitions.NAMESPACE, definition.exclusiveSetTag()))));
                    }
                });
            }
        }));
    }
}
