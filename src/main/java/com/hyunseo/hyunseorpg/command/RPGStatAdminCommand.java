package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.mana.ManaService;
import com.hyunseo.hyunseorpg.stat.StatModifier;
import com.hyunseo.hyunseorpg.stat.StatModifierOperation;
import com.hyunseo.hyunseorpg.stat.StatService;
import com.hyunseo.hyunseorpg.stat.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class RPGStatAdminCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "hyunseorpg.admin";

    private final StatService statService;
    private final ManaService manaService;

    public RPGStatAdminCommand(StatService statService, ManaService manaService) {
        this.statService = statService;
        this.manaService = manaService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(Component.text("이 명령어는 인게임 관리자 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }

        if (!admin.isOp() && !admin.hasPermission(PERMISSION)) {
            admin.sendMessage(Component.text("관리자 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || isHelp(args[0])) {
            sendHelp(admin, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "get" -> handleGet(admin, label, args);
            case "set" -> handleSet(admin, label, args);
            case "add" -> handleAdd(admin, label, args);
            case "reset" -> handleReset(admin, label, args);
            case "mana" -> handleMana(admin, label, args);
            case "modifier", "mod" -> handleModifier(admin, label, args);
            default -> {
                admin.sendMessage(Component.text("알 수 없는 하위 명령어입니다: " + args[0], NamedTextColor.RED));
                sendHelp(admin, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("get", "set", "add", "reset", "mana", "modifier"), args[0]);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("get") || subCommand.equals("set") || subCommand.equals("add") || subCommand.equals("reset")) {
            if (args.length == 2) {
                return filter(onlinePlayerNames(), args[1]);
            }
            if ((subCommand.equals("get") && args.length == 3)
                    || (subCommand.equals("set") && args.length == 3)
                    || (subCommand.equals("add") && args.length == 3)) {
                return filter(statNames(), args[2]);
            }
        }

        if (subCommand.equals("mana")) {
            if (args.length == 2) {
                return filter(onlinePlayerNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(List.of("get", "set", "add", "fill", "clear"), args[2]);
            }
        }

        if (subCommand.equals("modifier") || subCommand.equals("mod")) {
            if (args.length == 2) {
                return filter(List.of("add", "remove", "clear", "list"), args[1]);
            }
            if (args.length == 3) {
                return filter(onlinePlayerNames(), args[2]);
            }
            if (args.length == 5 && args[1].equalsIgnoreCase("add")) {
                return filter(statNames(), args[4]);
            }
            if (args.length == 6 && args[1].equalsIgnoreCase("add")) {
                return filter(List.of("add", "multiply"), args[5]);
            }
        }

        return List.of();
    }

    private boolean handleGet(Player admin, String label, String[] args) {
        if (args.length < 2) {
            admin.sendMessage(Component.text("사용법: /" + label + " get <player> [stat]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = findOnlinePlayer(admin, args[1]);
        if (target == null) {
            return true;
        }

        if (args.length >= 3) {
            Optional<StatType> statType = parseStatType(args[2]);
            if (statType.isEmpty()) {
                sendUnknownStat(admin, args[2]);
                return true;
            }
            sendOneStat(admin, target, statType.get());
            return true;
        }

        admin.sendMessage(Component.text(target.getName() + " 스탯", NamedTextColor.GOLD));
        for (StatType statType : StatType.values()) {
            sendOneStat(admin, target, statType);
        }
        sendMana(admin, target);
        return true;
    }

    private boolean handleSet(Player admin, String label, String[] args) {
        if (args.length < 4) {
            admin.sendMessage(Component.text("사용법: /" + label + " set <player> <stat> <value>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = findOnlinePlayer(admin, args[1]);
        Optional<StatType> statType = parseStatType(args[2]);
        Double value = parseDouble(admin, args[3]);
        if (target == null || statType.isEmpty() || value == null) {
            if (statType.isEmpty()) {
                sendUnknownStat(admin, args[2]);
            }
            return true;
        }

        statService.adminSetBaseStat(target, statType.get(), value);
        admin.sendMessage(Component.text(target.getName() + " " + statType.get().name() + " 기본값 = " + value, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAdd(Player admin, String label, String[] args) {
        if (args.length < 4) {
            admin.sendMessage(Component.text("사용법: /" + label + " add <player> <stat> <amount>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = findOnlinePlayer(admin, args[1]);
        Optional<StatType> statType = parseStatType(args[2]);
        Double amount = parseDouble(admin, args[3]);
        if (target == null || statType.isEmpty() || amount == null) {
            if (statType.isEmpty()) {
                sendUnknownStat(admin, args[2]);
            }
            return true;
        }

        statService.adminAddBaseStat(target, statType.get(), amount);
        admin.sendMessage(Component.text(target.getName() + " " + statType.get().name() + " 기본값을 " + amount + "만큼 변경했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleReset(Player admin, String label, String[] args) {
        if (args.length < 2) {
            admin.sendMessage(Component.text("사용법: /" + label + " reset <player>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = findOnlinePlayer(admin, args[1]);
        if (target == null) {
            return true;
        }

        statService.resetBaseStats(target);
        admin.sendMessage(Component.text(target.getName() + " 기본 스탯을 모두 0으로 초기화했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleMana(Player admin, String label, String[] args) {
        if (args.length < 3) {
            admin.sendMessage(Component.text("사용법: /" + label + " mana <player> <get|set|add|fill|clear> [amount]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = findOnlinePlayer(admin, args[1]);
        if (target == null) {
            return true;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("get")) {
            sendMana(admin, target);
            return true;
        }
        if (action.equals("fill")) {
            manaService.fillToMax(target);
            admin.sendMessage(Component.text(target.getName() + " 마나를 최대치로 채웠습니다.", NamedTextColor.GREEN));
            return true;
        }
        if (action.equals("clear")) {
            manaService.clearMana(target);
            admin.sendMessage(Component.text(target.getName() + " 마나를 0으로 설정했습니다.", NamedTextColor.GREEN));
            return true;
        }

        if (args.length < 4) {
            admin.sendMessage(Component.text("마나 값을 입력해야 합니다.", NamedTextColor.RED));
            return true;
        }

        Double amount = parseDouble(admin, args[3]);
        if (amount == null) {
            return true;
        }

        if (action.equals("set")) {
            manaService.setCurrentMana(target, amount);
            admin.sendMessage(Component.text(target.getName() + " 현재 마나를 " + amount + "로 설정했습니다.", NamedTextColor.GREEN));
            return true;
        }
        if (action.equals("add")) {
            manaService.addCurrentMana(target, amount);
            admin.sendMessage(Component.text(target.getName() + " 현재 마나를 " + amount + "만큼 변경했습니다.", NamedTextColor.GREEN));
            return true;
        }

        admin.sendMessage(Component.text("알 수 없는 마나 작업입니다: " + action, NamedTextColor.RED));
        return true;
    }

    private boolean handleModifier(Player admin, String label, String[] args) {
        if (args.length < 3) {
            admin.sendMessage(Component.text("사용법: /" + label + " modifier <add|remove|clear|list> <player> ...", NamedTextColor.YELLOW));
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = findOnlinePlayer(admin, args[2]);
        if (target == null) {
            return true;
        }

        if (action.equals("list")) {
            sendModifiers(admin, target);
            return true;
        }
        if (action.equals("clear")) {
            statService.clearRuntimeModifiers(target);
            admin.sendMessage(Component.text(target.getName() + " 임시 스탯 수정자를 모두 제거했습니다.", NamedTextColor.GREEN));
            return true;
        }
        if (action.equals("remove")) {
            if (args.length < 4) {
                admin.sendMessage(Component.text("사용법: /" + label + " modifier remove <player> <sourceId>", NamedTextColor.YELLOW));
                return true;
            }
            boolean removed = statService.removeModifier(target, args[3]);
            admin.sendMessage(Component.text(removed ? "수정자를 제거했습니다." : "해당 sourceId의 수정자가 없습니다.", removed ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            return true;
        }
        if (action.equals("add")) {
            if (args.length < 7) {
                admin.sendMessage(Component.text("사용법: /" + label + " modifier add <player> <sourceId> <stat> <add|multiply> <amount>", NamedTextColor.YELLOW));
                return true;
            }

            Optional<StatType> statType = parseStatType(args[4]);
            Optional<StatModifierOperation> operation = parseOperation(args[5]);
            Double amount = parseDouble(admin, args[6]);
            if (statType.isEmpty() || operation.isEmpty() || amount == null) {
                if (statType.isEmpty()) {
                    sendUnknownStat(admin, args[4]);
                }
                if (operation.isEmpty()) {
                    admin.sendMessage(Component.text("operation은 add 또는 multiply만 가능합니다.", NamedTextColor.RED));
                }
                return true;
            }

            statService.putModifier(target, args[3], statType.get(), operation.get(), amount);
            admin.sendMessage(Component.text("수정자를 적용했습니다: " + args[3], NamedTextColor.GREEN));
            return true;
        }

        admin.sendMessage(Component.text("알 수 없는 modifier 작업입니다: " + action, NamedTextColor.RED));
        return true;
    }

    private void sendOneStat(Player admin, Player target, StatType statType) {
        double base = statService.getBaseStat(target, statType);
        double effective = statService.getEffectiveStat(target, statType);
        admin.sendMessage(Component.text(statType.name() + " base=" + format(base) + ", effective=" + format(effective), NamedTextColor.GRAY));
    }

    private void sendMana(Player admin, Player target) {
        admin.sendMessage(Component.text("MANA current=" + format(manaService.getCurrentMana(target)) + ", max=" + format(manaService.getMaxMana(target)), NamedTextColor.AQUA));
    }

    private void sendModifiers(Player admin, Player target) {
        List<StatModifier> modifiers = new ArrayList<>(statService.getModifiers(target));
        if (modifiers.isEmpty()) {
            admin.sendMessage(Component.text("적용 중인 임시 스탯 수정자가 없습니다.", NamedTextColor.YELLOW));
            return;
        }

        admin.sendMessage(Component.text(target.getName() + " 임시 스탯 수정자", NamedTextColor.GOLD));
        for (StatModifier modifier : modifiers) {
            admin.sendMessage(Component.text(
                    modifier.sourceId() + ": " + modifier.statType().name() + " " + modifier.operation().name() + " " + modifier.amount(),
                    NamedTextColor.GRAY
            ));
        }
    }

    private Player findOnlinePlayer(Player admin, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            admin.sendMessage(Component.text("온라인 플레이어를 찾을 수 없습니다: " + name, NamedTextColor.RED));
        }
        return target;
    }

    private Optional<StatType> parseStatType(String input) {
        String normalized = input.toUpperCase(Locale.ROOT).replace("-", "_");
        return Arrays.stream(StatType.values())
                .filter(statType -> statType.name().equals(normalized))
                .findFirst();
    }

    private Optional<StatModifierOperation> parseOperation(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "add", "plus" -> Optional.of(StatModifierOperation.ADD);
            case "multiply", "mul", "mult" -> Optional.of(StatModifierOperation.MULTIPLY);
            default -> Optional.empty();
        };
    }

    private Double parseDouble(Player admin, String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            admin.sendMessage(Component.text("숫자를 입력해야 합니다: " + input, NamedTextColor.RED));
            return null;
        }
    }

    private void sendUnknownStat(Player admin, String input) {
        admin.sendMessage(Component.text("알 수 없는 스탯입니다: " + input, NamedTextColor.RED));
        admin.sendMessage(Component.text("스탯: " + String.join(", ", statNames()), NamedTextColor.GRAY));
    }

    private void sendHelp(Player admin, String label) {
        admin.sendMessage(Component.text("HyunseoRPG 관리자 스탯 명령어", NamedTextColor.GOLD));
        admin.sendMessage(Component.text("/" + label + " get <player> [stat]", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " set <player> <stat> <value>", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " add <player> <stat> <amount>", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " reset <player>", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " mana <player> <get|set|add|fill|clear> [amount]", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " modifier add <player> <sourceId> <stat> <add|multiply> <amount>", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " modifier remove <player> <sourceId>", NamedTextColor.YELLOW));
    }

    private boolean isHelp(String value) {
        return value.equals("?") || value.equalsIgnoreCase("help");
    }

    private List<String> statNames() {
        return Arrays.stream(StatType.values())
                .map(StatType::name)
                .toList();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
