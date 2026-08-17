package com.hyunseo.hyunseorpg.enchant;

import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary enchant runtime data. Persistent equipment data stays on the
 * ItemStack; charge, channel, mark and spawned visual state are removed on
 * state cleanup and are deliberately not serialized.
 */
public final class EnchantRuntimeStateService {
    private final EquipmentInstanceService equipmentInstances;
    private final Map<StateKey, RuntimeState> states = new ConcurrentHashMap<>();

    public EnchantRuntimeStateService(EquipmentInstanceService equipmentInstances) {
        this.equipmentInstances = equipmentInstances;
    }

    public RuntimeState state(Player player, ItemStack equipment, String enchantId) {
        StateKey key = key(player, equipment, enchantId);
        return states.computeIfAbsent(key, ignored -> new RuntimeState());
    }

    public RuntimeState find(Player player, ItemStack equipment, String enchantId) {
        if (player == null || equipment == null || enchantId == null) return null;
        return equipmentInstances.get(equipment)
                .map(id -> states.get(new StateKey(player.getUniqueId(), id, normalize(enchantId))))
                .orElse(null);
    }

    public boolean has(Player player, ItemStack equipment, String enchantId) {
        RuntimeState state = find(player, equipment, enchantId);
        return state != null && !state.isExpired();
    }

    public String cooldownId(Player player, ItemStack equipment, String enchantId) {
        StateKey key = key(player, equipment, enchantId);
        return "enchant-instance:" + key.equipmentId() + ":" + key.enchantId();
    }

    public void clear(Player player, ItemStack equipment) {
        if (player == null || equipment == null) return;
        equipmentInstances.get(equipment).ifPresent(id -> clearEquipment(player, id));
    }

    public void clear(Player player, ItemStack equipment, String enchantId) {
        if (player == null || equipment == null) return;
        equipmentInstances.get(equipment).ifPresent(id -> clearEquipment(player, id, enchantId));
    }

    public void clearEquipment(Player player, UUID equipmentId) {
        if (player == null || equipmentId == null) return;
        clear(player.getUniqueId(), equipmentId);
    }

    public void clearEquipment(Player player, UUID equipmentId, String enchantId) {
        if (player == null || equipmentId == null) return;
        clear(new StateKey(player.getUniqueId(), equipmentId, normalize(enchantId)));
    }

    public void clearPlayer(Player player) {
        if (player == null) return;
        clearPlayer(player.getUniqueId());
    }

    public void clearPlayer(UUID playerId) {
        new ArrayList<>(states.keySet()).stream()
                .filter(key -> key.ownerId().equals(playerId))
                .forEach(this::clear);
    }

    public void clearExpired() {
        new ArrayList<>(states.entrySet()).stream()
                .filter(entry -> entry.getValue().isExpired())
                .map(Map.Entry::getKey)
                .forEach(this::clear);
    }

    /** Clears every transient task, entity and effect when the plugin stops. */
    public void clearAll() {
        new ArrayList<>(states.keySet()).forEach(this::clear);
    }

    private StateKey key(Player player, ItemStack equipment, String enchantId) {
        UUID ownerId = player == null ? new UUID(0L, 0L) : player.getUniqueId();
        return new StateKey(ownerId, equipmentInstances.ensure(equipment), normalize(enchantId));
    }

    private void clear(UUID ownerId, UUID equipmentId) {
        new ArrayList<>(states.keySet()).stream()
                .filter(key -> key.ownerId().equals(ownerId) && key.equipmentId().equals(equipmentId))
                .forEach(this::clear);
    }

    private void clear(StateKey key) {
        RuntimeState state = states.remove(key);
        if (state != null) state.cleanup();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record StateKey(UUID ownerId, UUID equipmentId, String enchantId) { }

    public static final class RuntimeState {
        private final Map<String, Long> longs = new HashMap<>();
        private final Map<String, Double> doubles = new HashMap<>();
        private final Map<String, UUID> entityIds = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();
        private final Set<BukkitTask> tasks = new HashSet<>();
        private final Set<Runnable> cleanupActions = new HashSet<>();
        private long expiresAtMillis;

        public long expiresAtMillis() { return expiresAtMillis; }
        public void expiresAtMillis(long value) { expiresAtMillis = Math.max(0L, value); }
        public boolean isExpired() { return expiresAtMillis > 0L && System.currentTimeMillis() >= expiresAtMillis; }
        public long getLong(String key, long fallback) { return longs.getOrDefault(key, fallback); }
        public void putLong(String key, long value) { longs.put(key, value); }
        public double getDouble(String key, double fallback) { return doubles.getOrDefault(key, fallback); }
        public void putDouble(String key, double value) { doubles.put(key, value); }
        public UUID getEntityId(String key) { return entityIds.get(key); }
        public void putEntityId(String key, UUID value) { if (value == null) entityIds.remove(key); else entityIds.put(key, value); }
        @SuppressWarnings("unchecked")
        public <T> T getObject(String key, Class<T> type) {
            Object value = objects.get(key);
            return type.isInstance(value) ? (T) value : null;
        }
        public void putObject(String key, Object value) { if (value == null) objects.remove(key); else objects.put(key, value); }
        public void addTask(BukkitTask task) { if (task != null) tasks.add(task); }
        public void addCleanup(Runnable action) { if (action != null) cleanupActions.add(action); }
        public void cleanup() {
            for (BukkitTask task : new ArrayList<>(tasks)) task.cancel();
            tasks.clear();
            for (UUID entityId : new ArrayList<>(entityIds.values())) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null && entity.isValid()) entity.remove();
            }
            entityIds.clear();
            for (Runnable action : new ArrayList<>(cleanupActions)) {
                try { action.run(); } catch (RuntimeException ignored) { }
            }
            cleanupActions.clear();
            objects.clear();
            longs.clear();
            doubles.clear();
            expiresAtMillis = 0L;
        }
    }
}
