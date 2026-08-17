package com.hyunseo.hyunseorpg.equipment.trigger;

import com.hyunseo.hyunseorpg.enchant.EnchantData;
import com.hyunseo.hyunseorpg.enchant.EnchantRegistry;
import com.hyunseo.hyunseorpg.enchant.EnchantRuntimeStateService;
import com.hyunseo.hyunseorpg.enchant.EnchantService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.skill.SkillInputResult;
import com.hyunseo.hyunseorpg.skill.SkillInputType;
import com.hyunseo.hyunseorpg.skill.SkillService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.skill.SkillData;
import com.hyunseo.hyunseorpg.skill.SkillRegistry;
import com.hyunseo.hyunseorpg.skill.SkillCastContext;
import com.hyunseo.hyunseorpg.skill.effect.SkillEffectPhase;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared trigger executor for equipment enchantments and future reactive options. */
public final class EquipmentEffectTriggerEngine {
    private final JavaPlugin plugin;
    private final EnchantRegistry registry;
    private final EnchantService enchantService;
    private final CooldownService cooldownService;
    private final EquipmentInstanceService equipmentInstances;
    private final EnchantRuntimeStateService runtimeStates;
    private final Map<String, EquipmentEffectHandler> handlers = new ConcurrentHashMap<>();
    private final Set<String> activeChains = ConcurrentHashMap.newKeySet();
    private final Map<InputDispatchKey, Long> inputDispatches = new ConcurrentHashMap<>();

    public EquipmentEffectTriggerEngine(JavaPlugin plugin, EnchantRegistry registry, EnchantService enchantService,
                                        CooldownService cooldownService, EquipmentInstanceService equipmentInstances,
                                        EnchantRuntimeStateService runtimeStates) {
        this.plugin = plugin;
        this.registry = registry;
        this.enchantService = enchantService;
        this.cooldownService = cooldownService;
        this.equipmentInstances = equipmentInstances;
        this.runtimeStates = runtimeStates;
    }

    public void registerHandler(String handlerId, EquipmentEffectHandler handler) {
        if (handlerId == null || handlerId.isBlank() || handler == null) return;
        handlers.put(normalize(handlerId), handler);
    }

    public SkillInputResult triggerInput(Player player, SkillInputType input, ItemStack triggeringItem,
                                         SkillService skillService) {
        long tick = plugin.getServer().getCurrentTick();
        inputDispatches.entrySet().removeIf(entry -> entry.getKey().tick() < tick - 2L);
        InputDispatchKey dispatchKey = new InputDispatchKey(player.getUniqueId(),
                equipmentInstances.get(triggeringItem).orElse(new UUID(0L, 0L)), input, tick);
        // A duplicate event in the same tick is still a handled input. Returning ignored here
        // would allow SkillService to fall through into a second legacy execution path.
        if (inputDispatches.putIfAbsent(dispatchKey, tick) != null) return SkillInputResult.accepted(true);
        if (triggeringItem != null && enchantService.hasInputBinding(triggeringItem)) {
            equipmentInstances.ensure(triggeringItem);
        }
        TriggerContext context = new TriggerContext(
                TriggerType.INPUT, TriggerPhase.CONFIRMED, player, Instant.now(), null,
                UUID.randomUUID(), List.of(triggeringItem), triggeringItem, null, null, null, 0.0D, 0.0D);
        List<ResolvedEnchant> candidates = matching(context.withInput(input), input);
        for (ResolvedEnchant candidate : candidates) {
            EnchantData enchant = candidate.enchant();
            EquipmentEffectHandler handler = handlers.get(normalize(enchant.handlerId()));
            if (handler == null && "skill".equals(normalize(enchant.handlerId()))) {
                handler = (ignored, value) -> {
                    SkillInputResult result = skillService.executeEnchantment(player, value, input);
                    return result.accepted() ? EquipmentEffectResult.EXECUTED : EquipmentEffectResult.CONDITION_NOT_MET;
                };
            }
            if (handler == null) {
                plugin.getLogger().warning("No equipment effect handler for enchant " + enchant.enchantId()
                        + " (handler=" + enchant.handlerId() + ")");
                continue;
            }
            EquipmentEffectResult result = executeSafely(context.withInput(input).withTriggeringItem(candidate.source()), enchant, handler);
            if (result == EquipmentEffectResult.EXECUTED || result == EquipmentEffectResult.EXECUTED_DEFERRED_COOLDOWN
                    || result == EquipmentEffectResult.COOLDOWN) {
                return SkillInputResult.accepted(true);
            }
        }
        return SkillInputResult.ignored();
    }

    public List<EquipmentEffectResult> trigger(TriggerContext context) {
        List<EquipmentEffectResult> results = new ArrayList<>();
        for (ResolvedEnchant candidate : matching(context, null)) {
            EnchantData enchant = candidate.enchant();
            EquipmentEffectHandler handler = handlers.get(normalize(enchant.handlerId()));
            if (handler == null) continue;
            results.add(executeSafely(context.withTriggeringItem(candidate.source()), enchant, handler));
        }
        return results;
    }

    private List<ResolvedEnchant> matching(TriggerContext context, SkillInputType input) {
        Map<String, ResolvedEnchant> selected = new HashMap<>();
        for (EnchantData enchant : registry.getAll()) {
            boolean bound = enchant.triggers().stream().anyMatch(binding ->
                    binding.matches(context.triggerType(), context.phase(), input));
            if (!bound) continue;
            boolean eventItemIsAuthoritative = context.triggeringItem() != null
                    && (input == SkillInputType.DROP_KEY || enchant.sourceScope() == SourceScope.TRIGGERING_ITEM);
            List<ItemStack> sources = eventItemIsAuthoritative
                    ? List.of(context.triggeringItem())
                    : context.equipmentSlots().sources(enchant.sourceScope());
            for (ItemStack source : sources) {
                if (!enchantService.hasActiveEquipped(source, enchant.enchantId())) continue;
                if (!matchesSource(enchant.sourceScope(), context, source)) continue;
                String key = enchant.enchantId();
                ResolvedEnchant candidate = new ResolvedEnchant(enchant, source, inputSlotPriority(context, input, enchant.sourceScope()));
                if (enchant.duplicatePolicy() == DuplicatePolicy.ALL) {
                    selected.put(key + "@" + equipmentInstances.ensure(source), candidate);
                } else {
                    ResolvedEnchant previous = selected.get(key);
                    if (previous == null || candidate.slotPriority() < previous.slotPriority()
                            || (candidate.slotPriority() == previous.slotPriority()
                            && enchant.priority() > previous.enchant().priority())) {
                        selected.put(key, candidate);
                    }
                }
            }
        }
        return selected.values().stream()
                .sorted(Comparator.comparingInt(ResolvedEnchant::slotPriority)
                        .thenComparing(Comparator.comparingInt((ResolvedEnchant value) -> value.enchant().priority()).reversed())
                        .thenComparing(value -> value.enchant().enchantId()))
                .toList();
    }

    private boolean matchesSource(SourceScope scope, TriggerContext context, ItemStack source) {
        if (scope == SourceScope.TRIGGERING_ITEM) {
            return context.triggeringItem() != null
                    && equipmentInstances.get(context.triggeringItem()).equals(equipmentInstances.get(source));
        }
        if (context.inputType() == SkillInputType.DROP_KEY && context.triggeringItem() != null) {
            return equipmentInstances.get(context.triggeringItem()).equals(equipmentInstances.get(source));
        }
        return context.equipmentSlots().sources(scope).stream()
                .anyMatch(slotItem -> equipmentInstances.get(slotItem).equals(equipmentInstances.get(source)));
    }

    private int inputSlotPriority(TriggerContext context, SkillInputType input, SourceScope scope) {
        if (input == SkillInputType.DROP_KEY || input == SkillInputType.OFFHAND_QUICK) {
            return scope == SourceScope.MAIN_HAND ? 0 : scope == SourceScope.OFF_HAND ? 1 : 2;
        }
        return scope == SourceScope.TRIGGERING_ITEM ? 0 : 1;
    }

    private EquipmentEffectResult executeSafely(TriggerContext context, EnchantData enchant,
                                                 EquipmentEffectHandler handler) {
        String guard = context.chainId() + ":" + enchant.enchantId() + ":" + context.triggerType();
        if (!activeChains.add(guard)) return EquipmentEffectResult.CONDITION_NOT_MET;
        try {
            ItemStack sourceItem = sourceFor(context, enchant);
            String cooldownId = runtimeStates == null
                    ? "enchant-trigger:" + enchant.enchantId()
                    : runtimeStates.cooldownId(context.player(), sourceItem, enchant.enchantId());
            long remaining = cooldownService.getRemainingMillis(context.player().getUniqueId(), cooldownId);
            if (remaining > 0L) return EquipmentEffectResult.COOLDOWN;
            EquipmentEffectResult result = handler.handle(context, enchant);
            boolean cooldownStarted = false;
            if (result == EquipmentEffectResult.EXECUTED && enchant.cooldownSeconds() > 0.0D) {
                cooldownService.startCooldown(context.player().getUniqueId(), cooldownId,
                        Math.round(enchant.cooldownSeconds() * 1000.0D));
                cooldownStarted = true;
            }
            if (plugin.getConfig().getBoolean("enchant-runtime-debug.enabled", false)) {
                plugin.getLogger().info("[HyunseoRPG Enchant Input] player=" + context.player().getUniqueId()
                        + ", input=" + context.inputType()
                        + ", item=" + (sourceItem == null ? "AIR" : sourceItem.getType())
                        + ", equipment=" + equipmentInstances.get(sourceItem)
                        + ", enchant=" + enchant.enchantId()
                        + ", handler=" + enchant.handlerId()
                        + ", source-build=" + plugin.getClass().getProtectionDomain().getCodeSource().getLocation()
                        + ", result=" + result
                        + ", cooldown-started=" + cooldownStarted);
            }
            return result;
        } catch (Exception exception) {
            plugin.getLogger().warning("Equipment effect failed: enchant=" + enchant.enchantId()
                    + ", handler=" + enchant.handlerId() + ", trigger=" + context.triggerType()
                    + ", error=" + exception.getClass().getSimpleName());
            return EquipmentEffectResult.FAILED;
        } finally {
            activeChains.remove(guard);
        }
    }

    private record ResolvedEnchant(EnchantData enchant, ItemStack source, int slotPriority) { }

    private ItemStack sourceFor(TriggerContext context, EnchantData enchant) {
        if (context.triggeringItem() != null && enchantService.hasEquipped(context.triggeringItem(), enchant.enchantId())) {
            equipmentInstances.ensure(context.triggeringItem());
            return context.triggeringItem();
        }
        for (ItemStack source : context.sourceItems()) {
            if (enchantService.hasEquipped(source, enchant.enchantId())) {
                equipmentInstances.ensure(source);
                return source;
            }
        }
        if (context.triggeringItem() != null) {
            equipmentInstances.ensure(context.triggeringItem());
            return context.triggeringItem();
        }
        return new ItemStack(org.bukkit.Material.AIR);
    }

    private record InputDispatchKey(UUID playerId, UUID equipmentId, SkillInputType inputType, long tick) { }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
