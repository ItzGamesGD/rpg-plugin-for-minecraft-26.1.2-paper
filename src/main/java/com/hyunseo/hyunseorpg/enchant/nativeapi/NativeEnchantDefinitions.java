package com.hyunseo.hyunseorpg.enchant.nativeapi;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable bootstrap metadata, separate because registries freeze before runtime YAML is available.
 * NativeEnchantDefinitionParityTest prevents this table from drifting from enchants.yml.
 */
public final class NativeEnchantDefinitions {
    public static final String NAMESPACE = "hyunseorpg";
    private static final String BOW_SHIFT_EXCLUSIVE = "exclusive_set/bow_shift_left";
    public static final List<NativeEnchantDefinition> ALL = List.of(
        t("blade_chain", "칼날 연쇄", 1, "swords"),
        t("light_greatsword", "빛의 대검", 1, "swords"),
        t("laser_arrow", "레이저 화살", 1, "bows"),
        t("axe_heavy_strike", "강타", 1, "axes"),
        t("titans_wrath", "타이탄의 분노", 1, "axes"),
        c("skill_protection", "스킬 보호", 4, "minecraft:enchantable/armor"),
        c("rolling_landing", "구르기 착지", 1, "minecraft:enchantable/foot_armor"),
        x("wind_arrow", "바람 화살", 1, "bows", BOW_SHIFT_EXCLUSIVE, AcquisitionPolicy.common()),
        x("fire_arrow_rain", "불화살 비", 1, "bows", BOW_SHIFT_EXCLUSIVE, AcquisitionPolicy.treasure()),
        t("crossbow_barrage", "쇠뇌 연발", 1, "crossbows"),
        c("mining_bonus_drop", "광맥 추가 채굴", 1, "pickaxes"),
        t("area_excavation", "광역 채굴", 1, "excavation_tools"),
        c("auto_replant", "자동 심기", 1, "hoes"),
        c("auto_smelt", "자동 제련", 1, "pickaxes"),
        c("chain_logging", "연쇄 벌목", 1, "axes"),
        c("treasure_finder", "보물 찾기", 1, "fishing_rods"),
        c("multi_catch", "다중 낚시", 1, "fishing_rods"),
        t("elytra_launch", "겉날개 발사", 1, "elytra"),
        t("precision_flight", "정밀 비행", 3, "elytra"),
        t("explosive_mace", "폭발 철퇴", 1, "maces")
    );
    public static final Map<String, NativeEnchantDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(NativeEnchantDefinition::id, Function.identity()));


    public static boolean conflicts(String firstId, String secondId) {
        NativeEnchantDefinition first = BY_ID.get(firstId);
        NativeEnchantDefinition second = BY_ID.get(secondId);
        return first != null && second != null && !firstId.equals(secondId)
                && !first.exclusiveSetTag().isBlank()
                && first.exclusiveSetTag().equals(second.exclusiveSetTag());
    }

    private NativeEnchantDefinitions() { }
    private static NativeEnchantDefinition c(String id, String name, int level, String tag) {
        return x(id, name, level, tag, "", AcquisitionPolicy.common());
    }
    private static NativeEnchantDefinition t(String id, String name, int level, String tag) {
        return x(id, name, level, tag, "", AcquisitionPolicy.treasure());
    }
    private static NativeEnchantDefinition x(String id, String name, int level, String tag, String exclusive,
                                             AcquisitionPolicy acquisition) {
        String qualifiedTag = tag.contains(":") ? tag : NAMESPACE + ":" + tag;
        return new NativeEnchantDefinition(id, name, level, 5, 10, 8, 55, 8, 4,
                qualifiedTag, exclusive, acquisition);
    }
}
