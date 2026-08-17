package com.hyunseo.hyunseorpg.enchant;

import com.hyunseo.hyunseorpg.skill.SkillInputType;
import com.hyunseo.hyunseorpg.equipment.trigger.DuplicatePolicy;
import com.hyunseo.hyunseorpg.equipment.trigger.EnchantTriggerBinding;
import com.hyunseo.hyunseorpg.equipment.trigger.SourceScope;
import com.hyunseo.hyunseorpg.weapon.WeaponType;

import java.util.List;

public record EnchantData(
        String enchantId,
        String displayName,
        String bookItemId,
        String executorId,
        WeaponType weaponType,
        SkillInputType inputType,
        String handlerId,
        SourceScope sourceScope,
        DuplicatePolicy duplicatePolicy,
        int priority,
        double cooldownSeconds,
        boolean cooldownOnSuccessOnly,
        boolean showCooldownMessage,
        String equipmentCategory,
        List<String> materialPatterns,
        int maxLevel,
        String conflictGroup,
        List<EnchantTriggerBinding> triggers,
        List<String> loreDescription,
        List<String> loreInputDescription,
        String loreStatus
) {
    public EnchantData(String enchantId, String displayName, String bookItemId, String executorId,
                       WeaponType weaponType, SkillInputType inputType) {
        this(enchantId, displayName, bookItemId, executorId, weaponType, inputType,
                "skill", SourceScope.MAIN_HAND, DuplicatePolicy.HIGHEST_LEVEL, 0,
                0.0D, true, true, "", List.of(), 1, "", List.of(), List.of(), List.of(), "ACTIVE");
    }
}
