package com.hyunseo.hyunseorpg.equipment.trigger;

import com.hyunseo.hyunseorpg.enchant.EnchantData;

@FunctionalInterface
public interface EquipmentEffectHandler {
    EquipmentEffectResult handle(TriggerContext context, EnchantData enchant);
}
