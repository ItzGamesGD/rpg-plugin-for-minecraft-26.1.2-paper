package com.hyunseo.hyunseorpg.equipment;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

/** Assigns one immutable runtime identity to each RPG equipment item. */
public final class EquipmentInstanceService {
    private final NamespacedKey instanceKey;

    public EquipmentInstanceService(JavaPlugin plugin) {
        this.instanceKey = new NamespacedKey(plugin, "equipment_instance_uuid");
    }

    public Optional<UUID> get(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
        String raw = item.getItemMeta().getPersistentDataContainer().get(instanceKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public UUID ensure(ItemStack item) {
        UUID existing = get(item).orElse(null);
        if (existing != null) return existing;
        if (item == null || item.getType().isAir()) return new UUID(0L, 0L);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new UUID(0L, 0L);
        UUID created = UUID.randomUUID();
        meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, created.toString());
        item.setItemMeta(meta);
        return created;
    }

    public boolean is(ItemStack item, UUID instanceId) {
        return instanceId != null && get(item).map(instanceId::equals).orElse(false);
    }
}
