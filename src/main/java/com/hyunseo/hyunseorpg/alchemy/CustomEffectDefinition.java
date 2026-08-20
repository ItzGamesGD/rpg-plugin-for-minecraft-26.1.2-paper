package com.hyunseo.hyunseorpg.alchemy;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record CustomEffectDefinition(String id, String displayName, String description, boolean enabled, int priority,
                                     int durationTicks, int amplifier, int maxStacks,
                                     EffectTargetPolicy targetPolicy, EffectStackPolicy stackPolicy,
                                     boolean removeOnDeath, boolean persistOnLogout,
                                     boolean persistOnWorldChange, String handlerId,
                                     List<EffectComponentDefinition> components) {
    public CustomEffectDefinition(String id, String displayName, boolean enabled, int priority,
                                  int durationTicks, int amplifier, int maxStacks,
                                  EffectTargetPolicy targetPolicy, EffectStackPolicy stackPolicy,
                                  boolean removeOnDeath, boolean persistOnLogout,
                                  boolean persistOnWorldChange, String handlerId,
                                  List<EffectComponentDefinition> components) {
        this(id, displayName, defaultDescription(id), enabled, priority, durationTicks, amplifier,
                maxStacks, targetPolicy, stackPolicy, removeOnDeath, persistOnLogout,
                persistOnWorldChange, handlerId, components);
    }

    public CustomEffectDefinition {
        id = normalizeId(id);
        displayName = displayName == null || displayName.isBlank() ? id : displayName.trim();
        description = description == null || description.isBlank() ? defaultDescription(id) : description.trim();
        if (!id.matches("[a-z0-9]+(_[a-z0-9]+)*")) {
            throw new IllegalArgumentException("invalid effect id: " + id);
        }
        if (durationTicks < 1) throw new IllegalArgumentException("durationTicks must be positive");
        if (amplifier < 0) throw new IllegalArgumentException("amplifier must be non-negative");
        if (maxStacks < 1) throw new IllegalArgumentException("maxStacks must be positive");
        targetPolicy = Objects.requireNonNull(targetPolicy, "targetPolicy");
        stackPolicy = Objects.requireNonNull(stackPolicy, "stackPolicy");
        handlerId = handlerId == null ? "" : handlerId.trim().toLowerCase(Locale.ROOT);
        components = List.copyOf(components == null ? List.of() : components);
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String defaultDescription(String effectId) {
        return switch (normalizeId(effectId)) {
            case "effect_vampire" -> "가한 피해의 일정 비율만큼 자신의 체력을 회복합니다.";
            case "effect_berserk" -> "가하는 피해가 증가하지만 받는 피해도 증가합니다.";
            case "effect_corrosion" -> "대상의 방어를 약화해 받는 피해가 증가합니다.";
            case "effect_frostbite" -> "대상의 이동 속도를 낮추고 몸을 얼어붙게 만듭니다.";
            case "effect_shock" -> "주기적으로 피해를 주며 피해 직후 잠시 이동을 구속합니다.";
            case "effect_bleed" -> "일정 간격으로 지속적인 출혈 피해를 줍니다.";
            case "effect_vulnerability" -> "대상이 받는 피해를 증가시킵니다.";
            case "effect_necrosis" -> "대상의 최대 체력을 감소시킵니다.";
            case "effect_test_speed" -> "이동 속도를 증가시키는 테스트 효과입니다.";
            default -> "등록된 양조 상태 효과입니다.";
        };
    }
}
