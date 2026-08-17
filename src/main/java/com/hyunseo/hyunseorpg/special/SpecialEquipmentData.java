package com.hyunseo.hyunseorpg.special;

import java.util.List;
import java.util.Map;

public record SpecialEquipmentData(
        String id,
        String itemId,
        String displayName,
        String element,
        String equipmentType,
        boolean enabled,
        int requiredRpgLevel,
        List<String> requiredWorlds,
        Map<String, Long> requiredMobKills,
        Map<String, Long> requiredBossKills,
        Map<String, Integer> requiredItems,
        String requiredEquipmentId,
        int minimumEnhancementLevel,
        int minimumPromotionStage,
        boolean recipeEnabled,
        Map<String, Integer> recipeInputs,
        int outputAmount,
        int grade,
        boolean finalGearMaterialAllowed,
        boolean promotionEnabled,
        boolean allowUpgrade,
        boolean allowPromotion,
        boolean allowVanillaEnchants,
        boolean allowCustomEnchants,
        int customEnchantSlots,
        boolean unbreakable,
        Map<String, SpecialEquipmentAbilityDefinition> abilities
) {
    public SpecialEquipmentData {
        requiredWorlds = List.copyOf(requiredWorlds == null ? List.of() : requiredWorlds);
        requiredMobKills = Map.copyOf(requiredMobKills == null ? Map.of() : requiredMobKills);
        requiredBossKills = Map.copyOf(requiredBossKills == null ? Map.of() : requiredBossKills);
        requiredItems = Map.copyOf(requiredItems == null ? Map.of() : requiredItems);
        recipeInputs = Map.copyOf(recipeInputs == null ? Map.of() : recipeInputs);
        requiredRpgLevel = Math.max(1, requiredRpgLevel);
        minimumEnhancementLevel = Math.max(0, minimumEnhancementLevel);
        minimumPromotionStage = Math.max(0, minimumPromotionStage);
        outputAmount = Math.max(1, outputAmount);
        grade = Math.max(1, Math.min(5, grade));
        customEnchantSlots = Math.max(0, customEnchantSlots);
        abilities = Map.copyOf(abilities == null ? Map.of() : abilities);
    }
}
