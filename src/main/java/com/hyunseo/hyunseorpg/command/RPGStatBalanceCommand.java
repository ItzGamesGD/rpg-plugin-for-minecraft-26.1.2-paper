package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.stat.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class RPGStatBalanceCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "hyunseorpg.admin";

    private final ConfigService configService;

    public RPGStatBalanceCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("관리자 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equals("?") || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> handleList(sender);
            case "max" -> handleMax(sender, label, args);
            case "skillmax" -> handleSkillMax(sender, label, args);
            case "bonus" -> handleBonus(sender, label, args);
            default -> {
                sender.sendMessage(Component.text("알 수 없는 하위 명령어입니다: " + args[0], NamedTextColor.RED));
                sendHelp(sender, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("list", "max", "skillmax", "bonus"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("bonus")) {
            return filter(Arrays.stream(StatType.values()).map(StatType::name).toList(), args[1]);
        }
        return List.of();
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("공통 스탯 밸런스", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("max-base-stat=" + configService.getStatsInt("max-base-stat", 8), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("max-skill-stat=" + configService.getStatsInt("max-skill-stat", 8), NamedTextColor.YELLOW));
        for (StatType statType : StatType.values()) {
            sender.sendMessage(Component.text(statType.name() + " bonus=" + configService.getStatsDouble("stat-point-bonuses." + statType.name(), 0.0D), NamedTextColor.AQUA));
        }
        return true;
    }

    private boolean handleMax(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("사용법: /" + label + " max <value>", NamedTextColor.YELLOW));
            return true;
        }
        Integer value = parseInt(sender, args[1]);
        if (value == null) {
            return true;
        }
        configService.setStatsValue("max-base-stat", Math.max(1, value));
        configService.saveStatsConfig();
        sender.sendMessage(Component.text("공통 스탯 최대치를 " + Math.max(1, value) + "로 저장했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSkillMax(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("사용법: /" + label + " skillmax <value>", NamedTextColor.YELLOW));
            return true;
        }
        Integer value = parseInt(sender, args[1]);
        if (value == null) {
            return true;
        }
        configService.setStatsValue("max-skill-stat", Math.max(1, value));
        configService.saveStatsConfig();
        sender.sendMessage(Component.text("스킬스탯 최대치를 " + Math.max(1, value) + "로 저장했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleBonus(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("사용법: /" + label + " bonus <stat> <value>", NamedTextColor.YELLOW));
            return true;
        }
        Optional<StatType> statType = parseStatType(args[1]);
        Double value = parseDouble(sender, args[2]);
        if (statType.isEmpty() || value == null) {
            sender.sendMessage(Component.text("스탯: " + String.join(", ", Arrays.stream(StatType.values()).map(StatType::name).toList()), NamedTextColor.GRAY));
            return true;
        }
        configService.setStatsValue("stat-point-bonuses." + statType.get().name(), value);
        configService.saveStatsConfig();
        sender.sendMessage(Component.text(statType.get().name() + " 레벨당 증가량을 " + value + "로 저장했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("공통 스탯 밸런스 명령어", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " max <value>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " skillmax <value>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " bonus <stat> <value>", NamedTextColor.YELLOW));
    }

    private Optional<StatType> parseStatType(String input) {
        String normalized = input.toUpperCase(Locale.ROOT).replace("-", "_");
        return Arrays.stream(StatType.values()).filter(statType -> statType.name().equals(normalized)).findFirst();
    }

    private Integer parseInt(CommandSender sender, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("정수를 입력해야 합니다: " + input, NamedTextColor.RED));
            return null;
        }
    }

    private Double parseDouble(CommandSender sender, String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("숫자를 입력해야 합니다: " + input, NamedTextColor.RED));
            return null;
        }
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }
}
