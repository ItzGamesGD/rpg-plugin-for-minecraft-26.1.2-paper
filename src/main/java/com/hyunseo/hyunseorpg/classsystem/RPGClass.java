package com.hyunseo.hyunseorpg.classsystem;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum RPGClass {
    SWORDMASTER(
            "swordmaster",
            "소드마스터",
            "swordmaster_sword",
            Material.DIAMOND_SWORD,
            0,
            "소드마스터의 검",
            List.of(
                    "좌클릭: 기본 공격",
                    "왼손 들기: 검 투척",
                    "우클릭: 칼날 사출",
                    "버리기 키: 빛의 대검"
            ),
            List.of("sword", "swordsman", "검사", "소드")
    ),
    BOWMASTER(
            "bowmaster",
            "보우마스터",
            "bowmaster_bow",
            Material.BOW,
            0,
            "보우마스터의 활",
            List.of(
                    "우클릭: 기본 활 공격",
                    "왼손 들기: 불화살",
                    "좌클릭: 레이저 화살",
                    "쉬프트+좌클릭: 불화살비"
            ),
            List.of("bow", "archer", "궁수", "보우")
    ),
    LANCER(
            "lancer",
            "랜서",
            "lancer_spear",
            Material.TRIDENT,
            0,
            "랜서의 창",
            List.of(
                    "좌클릭: 기본 공격",
                    "오른쪽 클릭 삼지창 투척은 차단됨",
                    "왼손 들기: 투창",
                    "우클릭: 돌진",
                    "버리기 키: 창격 돌파"
            ),
            List.of("spear", "창")
    ),
    ASSASSIN(
            "assassin",
            "어쌔신",
            "assassin_dagger",
            Material.IRON_SWORD,
            0,
            "어쌔신의 단검",
            List.of(
                    "좌클릭: 기본 공격",
                    "왼손 들기: 은신",
                    "우클릭: 암살"
            ),
            List.of("rogue", "dagger", "암살자", "단검")
    ),
    WIZARD(
            "wizard",
            "위자드",
            "wizard_staff",
            Material.BLAZE_ROD,
            1005,
            "위자드의 지팡이",
            List.of(
                    "좌클릭: 불쏘시개 화염",
                    "왼손 들기: 라이트닝",
                    "우클릭: 메테오",
                    "쉬프트+좌클릭: 골렘 소환"
            ),
            List.of("mage", "staff", "마법사", "지팡이")
    );

    private final String id;
    private final String koreanName;
    private final String weaponId;
    private final Material weaponMaterial;
    private final int customModelData;
    private final String weaponDisplayName;
    private final List<String> weaponLore;
    private final List<String> aliases;

    RPGClass(
            String id,
            String koreanName,
            String weaponId,
            Material weaponMaterial,
            int customModelData,
            String weaponDisplayName,
            List<String> weaponLore,
            List<String> aliases
    ) {
        this.id = id;
        this.koreanName = koreanName;
        this.weaponId = weaponId;
        this.weaponMaterial = weaponMaterial;
        this.customModelData = customModelData;
        this.weaponDisplayName = weaponDisplayName;
        this.weaponLore = List.copyOf(weaponLore);
        this.aliases = List.copyOf(aliases);
    }

    public String id() {
        return id;
    }

    public String koreanName() {
        return koreanName;
    }

    public String weaponId() {
        return weaponId;
    }

    public Material weaponMaterial() {
        return weaponMaterial;
    }

    public int customModelData() {
        return customModelData;
    }

    public String weaponDisplayName() {
        return weaponDisplayName;
    }

    public List<String> weaponLore() {
        return weaponLore;
    }

    public static Optional<RPGClass> fromInput(String input) {
        String normalizedInput = normalize(input);
        return Arrays.stream(values())
                .filter(rpgClass -> rpgClass.matches(normalizedInput))
                .findFirst();
    }

    public static List<String> commandIds() {
        return Arrays.stream(values())
                .map(RPGClass::id)
                .toList();
    }

    private boolean matches(String normalizedInput) {
        if (normalize(name()).equals(normalizedInput)) {
            return true;
        }
        if (normalize(id).equals(normalizedInput)) {
            return true;
        }
        if (normalize(koreanName).equals(normalizedInput)) {
            return true;
        }
        return aliases.stream().anyMatch(alias -> normalize(alias).equals(normalizedInput));
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}
