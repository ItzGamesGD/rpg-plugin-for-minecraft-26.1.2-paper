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
        boolean recipeEnabled,
        Map<String, Integer> recipeInputs,
        int outputAmount,
        boolean finalGearMaterialAllowed,
        boolean allowUpgrade,
        boolean allowVanillaEnchants,
        boolean allowCustomEnchants,
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
        outputAmount = Math.max(1, outputAmount);
        abilities = Map.copyOf(abilities == null ? Map.of() : abilities);
    }
}
