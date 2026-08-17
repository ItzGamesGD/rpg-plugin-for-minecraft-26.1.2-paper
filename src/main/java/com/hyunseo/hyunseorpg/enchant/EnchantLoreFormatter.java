package com.hyunseo.hyunseorpg.enchant;

import com.hyunseo.hyunseorpg.equipment.trigger.EnchantTriggerBinding;
import com.hyunseo.hyunseorpg.equipment.trigger.TriggerType;
import com.hyunseo.hyunseorpg.skill.SkillInputType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/** Single player-facing formatter for both enchant books and equipped enchant sections. */
public final class EnchantLoreFormatter {
    public static final String SECTION_TITLE = "[커스텀 인챈트]";

    private EnchantLoreFormatter() {
    }

    public static List<Component> equipmentLines(EnchantData data, int level) {
        List<Component> lines = new ArrayList<>();
        lines.add(line(data.displayName() + levelSuffix(level), NamedTextColor.AQUA));
        lines.addAll(descriptionLines(data));
        lines.addAll(inputLines(data));
        if (data.cooldownSeconds() > 0.0D) {
            lines.add(line("재사용 대기시간: " + formatSeconds(data.cooldownSeconds()), NamedTextColor.DARK_GRAY));
        }
        if (isReserved(data)) lines.add(line("현재 비활성화됨", NamedTextColor.RED));
        return List.copyOf(lines);
    }

    public static List<Component> bookLines(EnchantData data) {
        List<Component> lines = new ArrayList<>();
        lines.add(line(data.displayName() + " 인챈트 북", NamedTextColor.LIGHT_PURPLE));
        lines.add(line(isActive(data) ? "[액티브 인챈트]" : "[패시브 인챈트]", NamedTextColor.GRAY));
        lines.addAll(descriptionLines(data));
        lines.addAll(inputLines(data));
        lines.add(line("적용 가능 장비: " + equipmentName(data), NamedTextColor.GRAY));
        lines.add(line("최대 단계: " + roman(data.maxLevel()), NamedTextColor.GRAY));
        if (data.cooldownSeconds() > 0.0D) {
            lines.add(line("재사용 대기시간: " + formatSeconds(data.cooldownSeconds()), NamedTextColor.DARK_GRAY));
        }
        if (!data.conflictGroup().isBlank()) {
            lines.add(line("같은 입력 그룹의 인챈트와 함께 사용할 수 없습니다.", NamedTextColor.DARK_GRAY));
        }
        if (isReserved(data)) lines.add(line("현재 비활성화됨", NamedTextColor.RED));
        return List.copyOf(lines);
    }

    public static boolean isActive(EnchantData data) {
        return data.triggers().stream().anyMatch(binding -> binding.type() == TriggerType.INPUT);
    }

    public static boolean isReserved(EnchantData data) {
        return "RESERVED".equalsIgnoreCase(data.loreStatus()) || "DISABLED".equalsIgnoreCase(data.loreStatus());
    }

    private static List<Component> descriptionLines(EnchantData data) {
        List<String> text = data.loreDescription().isEmpty()
                ? List.of("이 인챈트는 " + data.displayName() + " 효과를 부여합니다.")
                : data.loreDescription();
        return text.stream().flatMap(value -> wrap(value, 28).stream())
                .map(value -> line(value, NamedTextColor.GRAY)).toList();
    }

    private static List<Component> inputLines(EnchantData data) {
        if (!data.loreInputDescription().isEmpty()) {
            return data.loreInputDescription().stream().flatMap(value -> wrap(value, 28).stream())
                    .map(value -> line(value, NamedTextColor.YELLOW)).toList();
        }
        return data.triggers().stream()
                .filter(binding -> binding.type() == TriggerType.INPUT && binding.input() != null)
                .map(EnchantTriggerBinding::input)
                .map(EnchantLoreFormatter::inputName)
                .map(value -> line(value, NamedTextColor.YELLOW))
                .toList();
    }

    private static String inputName(SkillInputType input) {
        return switch (input) {
            case DROP_KEY -> "Q키";
            case RIGHT_CLICK -> "우클릭";
            case LEFT_CLICK -> "좌클릭";
            case SHIFT_LEFT_CLICK -> "Shift + 좌클릭";
            case SHIFT_RIGHT_CLICK -> "Shift + 우클릭";
            case OFFHAND_QUICK -> "보조 손 전환키";
            case SHIFT_JUMP -> "Shift + 점프";
            case FIREWORK_USE -> "활공 중 폭죽 사용";
        };
    }

    private static String equipmentName(EnchantData data) {
        if (!data.equipmentCategory().isBlank()) {
            return switch (data.equipmentCategory().toUpperCase(java.util.Locale.ROOT)) {
                case "SPEAR" -> "창";
                case "SHIELD" -> "방패";
                case "ARMOR" -> "방어구";
                case "ELYTRA" -> "겉날개";
                case "FISHING_ROD" -> "낚싯대";
                case "TOOL" -> "도구";
                case "RANGED" -> "원거리 무기";
                case "WEAPON" -> "무기";
                default -> data.equipmentCategory();
            };
        }
        if (data.weaponType() != null) return switch (data.weaponType()) {
            case SWORD -> "검";
            case BOW -> "활";
            case SPEAR -> "창";
            case AXE -> "도끼";
            default -> "해당 장비";
        };
        return "해당 장비";
    }

    private static String levelSuffix(int level) {
        return level > 1 ? " " + roman(level) : "";
    }

    private static String roman(int value) {
        String[] values = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return value >= 1 && value <= values.length ? values[value - 1] : Integer.toString(Math.max(1, value));
    }

    private static String formatSeconds(double seconds) {
        return Math.abs(seconds - Math.rint(seconds)) < .0001D
                ? ((int) Math.rint(seconds)) + "초"
                : String.format(java.util.Locale.ROOT, "%.1f초", seconds);
    }

    private static List<String> wrap(String raw, int width) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> lines = new ArrayList<>();
        String value = raw.trim();
        while (value.length() > width) {
            int split = value.lastIndexOf(' ', width);
            if (split <= 0) split = width;
            lines.add(value.substring(0, split).trim());
            value = value.substring(split).trim();
        }
        if (!value.isBlank()) lines.add(value);
        return lines;
    }

    private static Component line(String value, NamedTextColor color) {
        return Component.text(value, color).decoration(TextDecoration.ITALIC, false);
    }
}
