package com.hyunseo.hyunseorpg.enchant.nativeapi;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Definitions needed before the Bukkit plugin is enabled. Runtime-only trigger settings remain in enchants.yml.
 * Vanilla acquisition is intentionally controlled by Minecraft enchantment tags, not plugin loot listeners.
 */
public final class NativeEnchantDefinitions {
    public static final String NAMESPACE = "hyunseorpg";
    public static final List<NativeEnchantDefinition> ALL = List.of(
        d("blade_chain", "칼날 연쇄", 1, "enchantable/sword"),
        d("light_greatsword", "빛의 대검", 1, "enchantable/sword"),
        d("laser_arrow", "레이저 화살", 1, "enchantable/bow"),
        d("axe_heavy_strike", "강타", 1, "enchantable/mining"),
        d("titans_wrath", "타이탄의 분노", 1, "enchantable/mining"),
        d("protection", "보호", 4, "enchantable/armor"),
        d("fire_protection", "화염 보호", 4, "enchantable/armor"),
        d("blast_protection", "폭발 보호", 4, "enchantable/armor"),
        d("projectile_protection", "투사체 보호", 4, "enchantable/armor"),
        d("skill_protection", "스킬 보호", 4, "enchantable/armor"),
        d("thorns", "가시", 3, "enchantable/armor"),
        d("rolling_landing", "구르기 착지", 1, "enchantable/foot_armor"),
        d("respiration", "호흡", 3, "enchantable/head_armor"),
        d("aqua_affinity", "친수성", 1, "enchantable/head_armor"),
        d("swift_sneak", "신속한 잠행", 3, "enchantable/leg_armor"),
        d("depth_strider", "물갈퀴", 3, "enchantable/foot_armor"),
        d("soul_speed", "영혼 가속", 3, "enchantable/foot_armor"),
        d("frost_walker", "얼음 걸음", 2, "enchantable/foot_armor"),
        d("wind_arrow", "바람 화살", 1, "enchantable/bow"),
        d("fire_arrow_rain", "불화살 비", 1, "enchantable/bow"),
        d("crossbow_barrage", "쇠뇌 연발", 1, "enchantable/crossbow"),
        d("unbreaking", "내구도 보존", 1, "enchantable/durability"),
        d("mining_bonus_drop", "광맥 추가 채굴", 1, "enchantable/mining"),
        d("area_excavation", "광역 채굴", 1, "enchantable/mining"),
        d("auto_replant", "자동 심기", 1, "enchantable/mining"),
        d("auto_smelt", "자동 제련", 1, "enchantable/mining"),
        d("chain_logging", "연쇄 벌목", 1, "enchantable/mining"),
        d("treasure_finder", "보물 찾기", 1, "enchantable/fishing"),
        d("multi_catch", "다중 낚시", 1, "enchantable/fishing"),
        d("elytra_launch", "겉날개 발사", 1, "enchantable/equippable"),
        d("precision_flight", "정밀 비행", 3, "enchantable/equippable"),
        d("explosive_mace", "폭발 철퇴", 1, "enchantable/mace")
    );
    public static final Map<String, NativeEnchantDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(NativeEnchantDefinition::id, Function.identity()));

    private NativeEnchantDefinitions() { }

    private static NativeEnchantDefinition d(String id, String name, int maxLevel, String itemTag) {
        return new NativeEnchantDefinition(id, name, maxLevel, 5, 10, 8, 55, 8, 4, itemTag, false);
    }
}
