package com.hyunseo.hyunseorpg.equipment;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/** Central mutable lore builder. Existing visible lines are retained byte-for-byte. */
public final class EquipmentLoreBuilder {
    private final List<Component> lines;

    private EquipmentLoreBuilder(ItemMeta meta) {
        this.lines = meta == null || meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
    }

    public static EquipmentLoreBuilder from(ItemMeta meta) {
        return new EquipmentLoreBuilder(meta);
    }

    public EquipmentLoreBuilder removePlainPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return this;
        lines.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).startsWith(prefix));
        return this;
    }

    public EquipmentLoreBuilder add(Component line) {
        if (line != null) lines.add(line);
        return this;
    }

    public EquipmentLoreBuilder addAll(Collection<Component> additions) {
        if (additions != null) additions.forEach(this::add);
        return this;
    }

    public EquipmentLoreBuilder removeIf(Predicate<Component> predicate) {
        if (predicate != null) lines.removeIf(predicate);
        return this;
    }

    public List<Component> build() {
        return List.copyOf(lines);
    }
}
