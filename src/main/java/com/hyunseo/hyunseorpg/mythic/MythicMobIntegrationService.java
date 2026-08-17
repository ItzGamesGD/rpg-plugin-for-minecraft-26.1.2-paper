package com.hyunseo.hyunseorpg.mythic;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.mob.drop.MobDropEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/** Optional reflection bridge. HyunseoRPG still compiles and runs without MythicMobs. */
public final class MythicMobIntegrationService {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final MythicMobRegistry registry;
    private final RPGItemService itemService;
    private final InventoryDeliveryService inventoryDeliveryService;
    private final NamespacedKey mythicMobIdKey;
    private boolean unavailableLogged;

    public MythicMobIntegrationService(JavaPlugin plugin, ConfigService configService,
                                       MythicMobRegistry registry, RPGItemService itemService,
                                       InventoryDeliveryService inventoryDeliveryService) {
        this.plugin = plugin;
        this.configService = configService;
        this.registry = registry;
        this.itemService = itemService;
        this.inventoryDeliveryService = inventoryDeliveryService;
        this.mythicMobIdKey = new NamespacedKey(plugin, "mythic_mob_id");
    }

    public boolean isEnabled() {
        return configService.getMythicMobsBoolean("settings.enabled", true)
                && plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null;
    }

    public Optional<String> findMobId(LivingEntity entity) {
        String stored = entity.getPersistentDataContainer().get(mythicMobIdKey, PersistentDataType.STRING);
        if (stored != null && !stored.isBlank()) return Optional.of(stored);
        if (!isEnabled()) return Optional.empty();
        Object instance = mythicMobInstance(entity);
        if (instance == null) return Optional.empty();
        String id = extractId(instance);
        if (id.isBlank()) return Optional.empty();
        entity.getPersistentDataContainer().set(mythicMobIdKey, PersistentDataType.STRING, id);
        return Optional.of(id);
    }

    public Optional<LivingEntity> spawn(String mobId, Location location) {
        if (!isEnabled() || mobId == null || mobId.isBlank()) return Optional.empty();
        Object manager = mythicManager();
        if (manager == null) return Optional.empty();
        try {
            Method method = findMethod(manager.getClass(), "spawnMob", String.class, Location.class).orElse(null);
            if (method == null) return Optional.empty();
            Object activeMob = invoke(method, manager, mobId, location);
            Object unwrapped = unwrap(activeMob);
            LivingEntity entity = toLivingEntity(unwrapped);
            if (entity == null) return Optional.empty();
            entity.getPersistentDataContainer().set(mythicMobIdKey, PersistentDataType.STRING,
                    mobId.trim().toLowerCase());
            return Optional.of(entity);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("MythicMobs spawn failed for " + mobId + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    public void dropRewards(Player player, MythicMobData data) {
        for (MobDropEntry drop : data.drops()) {
            if (ThreadLocalRandom.current().nextDouble() > drop.chance()) continue;
            int amount = ThreadLocalRandom.current().nextInt(drop.minAmount(), drop.maxAmount() + 1);
            Optional<ItemStack> registered = itemService.create(drop.itemId(), amount);
            inventoryDeliveryService.giveOrDiscard(player, registered.orElseGet(() -> fallbackItem(drop, amount)));
        }
    }

    public MythicMobRegistry registry() { return registry; }

    public boolean nativeDropsAllowed() {
        return configService.getMythicMobsBoolean("settings.allow-mythic-native-drops", false);
    }

    private ItemStack fallbackItem(MobDropEntry drop, int amount) {
        ItemStack item = new ItemStack(drop.material() == null ? Material.STONE : drop.material(), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(drop.displayName(), NamedTextColor.AQUA));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"),
                    PersistentDataType.STRING, drop.itemId());
            item.setItemMeta(meta);
        }
        return item;
    }

    private Object mythicMobInstance(Entity entity) {
        Object manager = mythicManager();
        if (manager == null) return null;
        try {
            Method method = findMethod(manager.getClass(), "getMythicMobInstance", Entity.class).orElse(null);
            return method == null ? null : unwrap(invoke(method, manager, entity));
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private Object mythicManager() {
        if (!isEnabled()) {
            if (!unavailableLogged && plugin.getServer().getPluginManager().getPlugin("MythicMobs") == null) {
                unavailableLogged = true;
                plugin.getLogger().info("MythicMobs not installed; optional integration is disabled.");
            }
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object api = apiClass.getMethod("inst").invoke(null);
            return apiClass.getMethod("getMobManager").invoke(api);
        } catch (ReflectiveOperationException exception) {
            if (!unavailableLogged) {
                unavailableLogged = true;
                plugin.getLogger().warning("MythicMobs detected, but its supported API bridge was not found.");
            }
            return null;
        }
    }

    private String extractId(Object instance) {
        for (String methodName : new String[]{"getMobType", "getInternalName", "getName"}) {
            try {
                Object value = instance.getClass().getMethod(methodName).invoke(instance);
                if (value instanceof String string && !string.isBlank()) return string.trim().toLowerCase();
                if (value != null && value != instance) {
                    String nested = extractId(value);
                    if (!nested.isBlank()) return nested;
                }
            } catch (ReflectiveOperationException ignored) { }
        }
        return "";
    }

    private LivingEntity toLivingEntity(Object value) {
        if (value instanceof LivingEntity entity) return entity;
        if (value == null) return null;
        for (String methodName : new String[]{"getBukkitEntity", "getEntity"}) {
            try {
                Object nested = value.getClass().getMethod(methodName).invoke(value);
                if (nested instanceof LivingEntity entity) return entity;
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private Object unwrap(Object value) {
        if (value instanceof Optional<?> optional) return optional.orElse(null);
        return value;
    }

    private Optional<Method> findMethod(Class<?> type, String name, Class<?>... parameters) {
        try { return Optional.of(type.getMethod(name, parameters)); }
        catch (NoSuchMethodException ignored) { return Optional.empty(); }
    }

    private Object invoke(Method method, Object target, Object... arguments) throws ReflectiveOperationException {
        return method.invoke(target, arguments);
    }
}
