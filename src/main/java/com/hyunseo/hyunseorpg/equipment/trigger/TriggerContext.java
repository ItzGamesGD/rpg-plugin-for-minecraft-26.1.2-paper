package com.hyunseo.hyunseorpg.equipment.trigger;

import com.hyunseo.hyunseorpg.combat.DamageContext;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TriggerContext(
        TriggerType triggerType,
        TriggerPhase phase,
        Player player,
        Instant occurredAt,
        Event originalEvent,
        UUID chainId,
        List<ItemStack> sourceItems,
        ItemStack triggeringItem,
        Entity target,
        LivingEntity livingTarget,
        Block block,
        double originalDamage,
        double finalDamage,
        DamageContext damageContext,
        EquipmentSlotSnapshot equipmentSlots,
        com.hyunseo.hyunseorpg.skill.SkillInputType inputType
) {
    public TriggerContext {
        sourceItems = sourceItems == null ? List.of() : sourceItems.stream()
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .toList();
        triggeringItem = triggeringItem == null ? null : triggeringItem.clone();
        equipmentSlots = equipmentSlots == null ? EquipmentSlotSnapshot.from(player) : equipmentSlots;
    }

    public TriggerContext(TriggerType triggerType, TriggerPhase phase, Player player, Instant occurredAt,
                          Event originalEvent, UUID chainId, List<ItemStack> sourceItems,
                          ItemStack triggeringItem, Entity target, LivingEntity livingTarget,
                          Block block, double originalDamage, double finalDamage) {
        this(triggerType, phase, player, occurredAt, originalEvent, chainId, sourceItems, triggeringItem,
                target, livingTarget, block, originalDamage, finalDamage, null,
                EquipmentSlotSnapshot.from(player), null);
    }

    public TriggerContext(TriggerType triggerType, TriggerPhase phase, Player player, Instant occurredAt,
                          Event originalEvent, UUID chainId, List<ItemStack> sourceItems,
                          ItemStack triggeringItem, Entity target, LivingEntity livingTarget,
                          Block block, double originalDamage, double finalDamage, DamageContext damageContext) {
        this(triggerType, phase, player, occurredAt, originalEvent, chainId, sourceItems, triggeringItem,
                target, livingTarget, block, originalDamage, finalDamage, damageContext,
                EquipmentSlotSnapshot.from(player), null);
    }

    public TriggerContext withTriggeringItem(ItemStack item) {
        return new TriggerContext(triggerType, phase, player, occurredAt, originalEvent, chainId, sourceItems,
                item, target, livingTarget, block, originalDamage, finalDamage, damageContext,
                equipmentSlots, inputType);
    }

    public TriggerContext withInput(com.hyunseo.hyunseorpg.skill.SkillInputType input) {
        return new TriggerContext(triggerType, phase, player, occurredAt, originalEvent, chainId, sourceItems,
                triggeringItem, target, livingTarget, block, originalDamage, finalDamage, damageContext,
                equipmentSlots, input);
    }
}
