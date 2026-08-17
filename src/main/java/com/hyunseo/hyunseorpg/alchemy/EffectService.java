package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owns custom effect instances. No potion, farming, combat, or recipe code is called here. */
public final class EffectService implements Listener {
    private final JavaPlugin plugin;
    private final CustomEffectRegistry registry;
    private final HandlerRegistry handlers = new HandlerRegistry();
    private final EffectConflictResolver conflicts = new EffectConflictResolver();
    private final TickManager tickManager;
    private final NamespacedKey legacyModifierCleanupKey;
    private final ActiveEffectStore active = new ActiveEffectStore();
    private final Map<UUID, List<AttributeModifier>> modifiers = new HashMap<>();

    public EffectService(JavaPlugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.registry = new CustomEffectRegistry(config);
        this.tickManager = new TickManager(plugin);
        this.legacyModifierCleanupKey = new NamespacedKey(plugin, "alchemy_legacy_modifier_cleanup_v1");
    }

    public boolean load() { return registry.load(); }

    /** Reloads definitions transactionally; a failed candidate keeps the old registry and active state. */
    public synchronized boolean reload() {
        if (!validateReload()) return false;
        // Effects are non-persistent by default. Clearing after a successful commit
        // prevents active instances from retaining definitions from the old snapshot.
        commitReload();
        return true;
    }
    /** Loads and validates the candidate registry without clearing active instances. */
    public synchronized boolean validateReload() { return registry.load(); }
    /** Commits lifecycle cleanup after all dependent registries have validated. */
    public synchronized void commitReload() { clearAllTargets(); }
    public CustomEffectRegistry registry() { return registry; }
    public HandlerRegistry handlers() { return handlers; }
    public void start() { tickManager.start(this::tick); }
    public void shutdown() {
        tickManager.stop();
        clearAllTargets();
    }

    public synchronized boolean apply(UUID targetId, String effectId, EffectContext context) {
        return applyInternal(targetId, effectId, context, null, null);
    }

    /** Applies a registry effect with a transient catalyst transformation. */
    public synchronized boolean applyWithOverrides(UUID targetId, String effectId, EffectContext context,
                                                    int durationTicks, int amplifier) {
        if (durationTicks < 1 || durationTicks > 72000 || amplifier < 0 || amplifier > 10) return false;
        return applyInternal(targetId, effectId, context, durationTicks, amplifier);
    }

    /** Test-only overrides; values are transient and never written to configuration or items. */
    public synchronized boolean applyDebug(UUID targetId, String effectId, EffectContext context,
                                            int durationTicks, int amplifier) {
        return applyWithOverrides(targetId, effectId, context, durationTicks, amplifier);
    }

    private boolean applyInternal(UUID targetId, String effectId, EffectContext context,
                                  Integer durationOverride, Integer amplifierOverride) {
        if (targetId == null || context == null || context.targetId() == null || !targetId.equals(context.targetId())) return false;
        CustomEffectDefinition definition = registry.get(effectId).orElse(null);
        Entity raw = Bukkit.getEntity(targetId);
        if (definition == null || !(raw instanceof LivingEntity target) || !allowed(definition, target, context)) return false;
        CombatEffectHandler handler = handlerFor(definition);
        if (!definition.handlerId().isBlank() && handler == null) return false;
        if (durationOverride != null || amplifierOverride != null) {
            definition = new CustomEffectDefinition(definition.id(), definition.displayName(), definition.enabled(),
                    definition.priority(), durationOverride == null ? definition.durationTicks() : durationOverride,
                    amplifierOverride == null ? definition.amplifier() : amplifierOverride,
                    definition.maxStacks(), definition.targetPolicy(), definition.stackPolicy(),
                    definition.removeOnDeath(), definition.persistOnLogout(), definition.persistOnWorldChange(),
                    definition.handlerId(), definition.components());
        }
        ActiveEffectInstance current = active.get(targetId, definition.id());
        if (!conflicts.canApply(current, definition)) return false;
        long now = currentTick();
        if (current != null) {
            switch (definition.stackPolicy()) {
                case IGNORE -> { return false; }
                case ADD_DURATION -> current.refresh(Math.min(now + 72000L, current.expiresAtTick() + definition.durationTicks()));
                case ADD_STACK -> { current.addStack(); current.refresh(now + definition.durationTicks()); }
                case REPLACE_IF_STRONGER -> { if (definition.amplifier() <= current.definition().amplifier()) return false; removeInternal(target, current); current = null; }
                case REPLACE_ALWAYS -> { removeInternal(target, current); current = null; }
                case REFRESH_DURATION -> current.refresh(now + definition.durationTicks());
            }
            if (current != null) return true;
        }
        ActiveEffectInstance instance = new ActiveEffectInstance(UUID.randomUUID(), targetId, definition,
                context.source(), now + definition.durationTicks(), 1);
        active.put(targetId, instance);
        applyComponents(target, instance);
        if (handler != null) handler.onApply(targetId, instance);
        return true;
    }

    public synchronized boolean remove(UUID targetId, String effectId) {
        ActiveEffectInstance instance = active.remove(targetId, effectId);
        if (instance == null) return false;
        Entity raw = Bukkit.getEntity(targetId);
        if (raw instanceof LivingEntity target) removeInternal(target, instance);
        return true;
    }

    public synchronized void clearTarget(UUID targetId) {
        List<ActiveEffectInstance> instances = active.removeAll(targetId);
        Entity raw = Bukkit.getEntity(targetId);
        if (!(raw instanceof LivingEntity target)) return;
        instances.forEach(instance -> removeInternal(target, instance));
    }

    public synchronized int clearAndReport(UUID targetId) {
        int count = getActive(targetId).size();
        clearTarget(targetId);
        return count;
    }

    public synchronized List<ActiveEffectInstance> getActive(UUID targetId) {
        return active.getActive(targetId);
    }

    public synchronized boolean isActive(UUID targetId, String effectId) {
        return active.contains(targetId, effectId);
    }

    /** Applies registered combat modifiers without exposing the active map to listeners. */
    public synchronized double modifyDamage(UUID sourceId, UUID targetId, double damage) {
        if (!Double.isFinite(damage) || damage < 0.0D) return damage;
        double modified = damage;
        if (sourceId != null) {
            for (ActiveEffectInstance instance : getActive(sourceId)) {
                CombatEffectHandler handler = handlerFor(instance.definition());
                if (handler instanceof CombatEffectModifier modifier) {
                    modified = modifier.modifyOutgoing(sourceId, targetId, modified);
                }
            }
        }
        if (targetId != null) {
            for (ActiveEffectInstance instance : getActive(targetId)) {
                CombatEffectHandler handler = handlerFor(instance.definition());
                if (handler instanceof CombatEffectModifier modifier) {
                    modified = modifier.modifyIncoming(sourceId, targetId, modified);
                }
            }
        }
        return Math.max(0.0D, Double.isFinite(modified) ? modified : damage);
    }

    /** Sends the final event amount through the normal registered handler callback. */
    public synchronized void notifyDamage(UUID sourceId, UUID targetId, double amount,
                                           CombatEffectHandler.DamageKind kind) {
        if (amount <= 0.0D || !Double.isFinite(amount) || kind == null) return;
        if (sourceId == null) return;
        for (ActiveEffectInstance instance : getActive(sourceId)) {
            CombatEffectHandler handler = handlerFor(instance.definition());
            if (handler != null) handler.onDamage(sourceId, targetId, amount, kind);
        }
    }

    public synchronized boolean isTicking() { return tickManager.isRunning(); }

    public synchronized void onDeath(UUID targetId) {
        if (targetId == null) return;
        Entity raw = Bukkit.getEntity(targetId);
        if (raw instanceof Player player) clearByDeathPolicy(player);
        else clearTarget(targetId);
    }

    public synchronized void onLogout(UUID targetId) {
        clearByLifecycle(targetId, false);
    }

    public synchronized void onKick(UUID targetId) {
        clearByLifecycle(targetId, false);
    }

    public synchronized void onWorldChange(UUID targetId) {
        clearByLifecycle(targetId, true);
    }

    public synchronized void onRespawn(UUID targetId) {
        onDeath(targetId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        onDeath(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { clearByLifecycle(event.getPlayer(), false); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) { clearByLifecycle(event.getPlayer(), false); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) { onRespawn(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) { clearByLifecycle(event.getPlayer(), true); }

    /** Removes only modifiers previously owned by this effect service after a stale logout. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean cleanupRecorded = player.getPersistentDataContainer().has(legacyModifierCleanupKey, PersistentDataType.BYTE);
        int removed = removeOwnedModifiers(player);
        if (!cleanupRecorded) {
            player.getPersistentDataContainer().set(legacyModifierCleanupKey, PersistentDataType.BYTE, (byte) 1);
            if (removed > 0) {
                plugin.getLogger().info("Removed " + removed + " stale alchemy attribute modifier(s) from " + player.getUniqueId());
            }
        }
        // Some server versions restore serialized player attributes after the join
        // event, and other listeners may refresh equipment state during the same tick.
        // Re-check after those lifecycle callbacks without touching foreign modifiers.
        Bukkit.getScheduler().runTask(plugin, () -> removeOwnedModifiers(player));
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeOwnedModifiers(player), 20L);
    }

    private synchronized void tick() {
        long now = currentTick();
        for (UUID targetId : active.targetIds()) {
            Entity raw = Bukkit.getEntity(targetId);
            if (!(raw instanceof LivingEntity target) || !target.isValid() || target.isDead()) { clearTarget(targetId); continue; }
            for (ActiveEffectInstance instance : getActive(targetId)) {
                if (instance.expiresAtTick() <= now) {
                    remove(targetId, instance.definition().id());
                    continue;
                }
                CombatEffectHandler handler = handlerFor(instance.definition());
                if (handler != null) handler.onTick(targetId, instance, now);
            }
        }
    }

    private void clearByDeathPolicy(Player player) {
        for (ActiveEffectInstance instance : getActive(player.getUniqueId())) {
            if (instance.definition().removeOnDeath()) remove(player.getUniqueId(), instance.definition().id());
        }
    }

    private void clearByLifecycle(UUID targetId, boolean worldChange) {
        Entity raw = Bukkit.getEntity(targetId);
        if (!(raw instanceof Player player)) {
            clearTarget(targetId);
            return;
        }
        clearByLifecycle(player, worldChange);
    }

    private void clearByLifecycle(Player player, boolean worldChange) {
        UUID targetId = player.getUniqueId();
        for (ActiveEffectInstance instance : getActive(targetId)) {
            boolean clear = worldChange ? !instance.definition().persistOnWorldChange()
                    : !instance.definition().persistOnLogout();
            if (!clear) continue;
            active.remove(targetId, instance.definition().id());
            removeInternal(player, instance);
        }
    }

    private synchronized void clearAllTargets() {
        new ArrayList<>(active.targetIds()).forEach(this::clearTarget);
    }

    private boolean allowed(CustomEffectDefinition definition, LivingEntity target, EffectContext context) {
        return switch (definition.targetPolicy()) {
            case ANY, LIVING_ENTITY -> true;
            case PLAYER -> target instanceof Player;
            case SELF -> context.applicatorId() != null && context.applicatorId().equals(target.getUniqueId());
        };
    }

    private void applyComponents(LivingEntity target, ActiveEffectInstance instance) {
        for (EffectComponentDefinition component : instance.definition().components()) {
            if (!component.enabled() || !component.id().equals("attribute")) continue;
            Attribute attribute;
            AttributeModifier.Operation operation;
            try {
                attribute = Attribute.valueOf(component.attribute().toUpperCase());
                operation = AttributeModifier.Operation.valueOf(component.operation().toUpperCase());
            } catch (IllegalArgumentException ignored) { continue; }
            AttributeInstance attributeInstance = target.getAttribute(attribute);
            if (attributeInstance == null) continue;
            UUID modifierId = UUID.nameUUIDFromBytes(("alchemy:" + instance.instanceId() + ":" + attribute.name()).getBytes(StandardCharsets.UTF_8));
            attributeInstance.getModifiers().stream().filter(modifier -> modifier.getUniqueId().equals(modifierId)).toList()
                    .forEach(attributeInstance::removeModifier);
            AttributeModifier modifier = new AttributeModifier(modifierId, "hyunseorpg_alchemy_" + instance.definition().id(), component.amount(), operation);
            attributeInstance.addModifier(modifier);
            modifiers.computeIfAbsent(instance.instanceId(), ignored -> new ArrayList<>()).add(modifier);
        }
    }

    private void removeInternal(LivingEntity target, ActiveEffectInstance instance) {
        for (EffectComponentDefinition component : instance.definition().components()) {
            try {
                Attribute attribute = Attribute.valueOf(component.attribute().toUpperCase());
                AttributeInstance attributeInstance = target.getAttribute(attribute);
                if (attributeInstance == null) continue;
                UUID modifierId = UUID.nameUUIDFromBytes(("alchemy:" + instance.instanceId() + ":" + attribute.name()).getBytes(StandardCharsets.UTF_8));
                attributeInstance.getModifiers().stream().filter(modifier -> modifier.getUniqueId().equals(modifierId)).toList()
                        .forEach(attributeInstance::removeModifier);
            } catch (IllegalArgumentException ignored) { }
        }
        CombatEffectHandler handler = handlerFor(instance.definition());
        if (handler != null) handler.onRemove(target.getUniqueId(), instance);
        modifiers.remove(instance.instanceId());
    }

    private CombatEffectHandler handlerFor(CustomEffectDefinition definition) {
        if (definition == null || definition.handlerId().isBlank()) return null;
        Object handler = handlers.get(definition.handlerId()).orElse(null);
        return handler instanceof CombatEffectHandler typed ? typed : null;
    }

    private int removeOwnedModifiers(LivingEntity target) {
        int removed = 0;
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = target.getAttribute(attribute);
            if (attributeInstance == null) continue;
            List<AttributeModifier> owned = attributeInstance.getModifiers().stream()
                    .filter(modifier -> modifier.getName().startsWith("hyunseorpg_alchemy_"))
                    .toList();
            owned.forEach(attributeInstance::removeModifier);
            removed += owned.size();
        }
        return removed;
    }

    private long currentTick() { return Bukkit.getCurrentTick(); }
}
