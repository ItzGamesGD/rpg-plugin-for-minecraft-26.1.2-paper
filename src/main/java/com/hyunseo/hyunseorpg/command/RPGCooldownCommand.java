package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.classsystem.RPGClass;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.skill.SkillInputType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RPGCooldownCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "hyunseorpg.admin";

    private final ConfigService configService;
    private final CooldownService cooldownService;

    public RPGCooldownCommand(ConfigService configService, CooldownService cooldownService) {
        this.configService = configService;
        this.cooldownService = cooldownService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("관리자 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || isHelp(args[0])) {
            sendHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "clear" -> handleClear(sender, label, args);
            case "get" -> handleGet(sender, label, args);
            case "set" -> handleSet(sender, label, args);
            case "list" -> handleList(sender);
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
            return filter(List.of("clear", "get", "set", "list"), args[0]);
        }
        if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("get")) {
            if (args.length == 2) {
                return filter(onlinePlayerNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(cooldownIds(), args[2]);
            }
        }
        if (args[0].equalsIgnoreCase("set") && args.length == 2) {
            return filter(cooldownIds(), args[1]);
        }
        if (args[0].equalsIgnoreCase("set") && args.length == 3) {
            return filter(List.of("0", "0.5", "1", "3", "6", "10", "15", "20", "30", "60"), args[2]);
        }
        if (!args[0].equalsIgnoreCase("set") && args.length == 2) {
            return filter(onlinePlayerNames(), args[1]);
        }
        return List.of();
    }

    private boolean handleClear(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("사용법: /" + label + " clear <player> [cooldownId]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = findOnlinePlayer(sender, args[1]);
        if (target == null) {
            return true;
        }

        if (args.length >= 3) {
            cooldownService.clearCooldown(target.getUniqueId(), args[2]);
            sender.sendMessage(Component.text(target.getName() + " 쿨타임 제거: " + args[2], NamedTextColor.GREEN));
            return true;
        }

        cooldownService.clearPlayer(target.getUniqueId());
        sender.sendMessage(Component.text(target.getName() + " 모든 쿨타임을 제거했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleGet(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("사용법: /" + label + " get <player> [cooldownId]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = findOnlinePlayer(sender, args[1]);
        if (target == null) {
            return true;
        }

        if (args.length < 3) {
            Map<String, Long> remainingCooldowns = cooldownService.getRemainingCooldowns(target.getUniqueId());
            if (remainingCooldowns.isEmpty()) {
                sender.sendMessage(Component.text(target.getName() + " 활성 쿨타임이 없습니다.", NamedTextColor.YELLOW));
                return true;
            }

            sender.sendMessage(Component.text(target.getName() + " 활성 쿨타임", NamedTextColor.GOLD));
            remainingCooldowns.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sender.sendMessage(Component.text(
                            entry.getKey() + ": " + formatSeconds(entry.getValue()) + "초",
                            NamedTextColor.AQUA
                    )));
            return true;
        }

        long remainingMillis = cooldownService.getRemainingMillis(target.getUniqueId(), args[2]);
        sender.sendMessage(Component.text(target.getName() + " " + args[2] + " 남은 쿨타임: " + formatSeconds(remainingMillis) + "초", NamedTextColor.AQUA));
        return true;
    }

    private boolean handleSet(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("사용법: /" + label + " set <cooldownId> <seconds>", NamedTextColor.YELLOW));
            return true;
        }

        CooldownKey cooldownKey = parseCooldownKey(args[1]);
        if (cooldownKey == null) {
            sender.sendMessage(Component.text("알 수 없는 쿨타임 ID입니다: " + args[1], NamedTextColor.RED));
            sender.sendMessage(Component.text("예시: swordmaster:drop-key, bowmaster:left-click", NamedTextColor.GRAY));
            return true;
        }

        Double seconds = parseSeconds(sender, args[2]);
        if (seconds == null) {
            return true;
        }

        configService.setValue(cooldownPath(cooldownKey), seconds);
        configService.saveConfig();
        sender.sendMessage(Component.text(cooldownKey.id() + " 쿨타임을 " + formatSeconds(Math.round(seconds * 1000.0D)) + "초로 저장했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("HyunseoRPG 스킬 쿨타임 설정값", NamedTextColor.GOLD));
        for (CooldownKey cooldownKey : cooldownKeys()) {
            double seconds = configService.getDouble(cooldownPath(cooldownKey), cooldownKey.defaultSeconds());
            sender.sendMessage(Component.text(cooldownKey.id() + ": " + String.format(Locale.ROOT, "%.1f", seconds) + "초", NamedTextColor.AQUA));
        }
        return true;
    }

    private Player findOnlinePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(Component.text("온라인 플레이어를 찾을 수 없습니다: " + name, NamedTextColor.RED));
        }
        return target;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("HyunseoRPG 쿨타임 관리 명령어", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " clear <player> [cooldownId]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " get <player> [cooldownId]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " set <cooldownId> <seconds>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("cooldownId 예시: swordmaster:drop-key, bowmaster:left-click", NamedTextColor.GRAY));
    }

    private boolean isHelp(String value) {
        return value.equals("?") || value.equalsIgnoreCase("help");
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }

    private List<String> cooldownIds() {
        return cooldownKeys().stream()
                .map(CooldownKey::id)
                .toList();
    }

    private List<CooldownKey> cooldownKeys() {
        return Arrays.stream(RPGClass.values())
                .flatMap(rpgClass -> Arrays.stream(SkillInputType.values())
                        .map(inputType -> new CooldownKey(rpgClass, inputType, defaultCooldownSeconds(rpgClass, inputType))))
                .filter(cooldownKey -> cooldownKey.defaultSeconds() > 0.0D)
                .toList();
    }

    private CooldownKey parseCooldownKey(String input) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        return cooldownKeys().stream()
                .filter(cooldownKey -> cooldownKey.id().equals(normalizedInput))
                .findFirst()
                .orElse(null);
    }

    private String cooldownPath(CooldownKey cooldownKey) {
        return "skill-input.cooldowns." + cooldownKey.rpgClass().id() + "." + cooldownKey.inputType().configKey();
    }

    private double defaultCooldownSeconds(RPGClass rpgClass, SkillInputType inputType) {
        return switch (rpgClass) {
            case SWORDMASTER -> switch (inputType) {
                case OFFHAND_QUICK -> 1.0D;
                case RIGHT_CLICK -> 20.0D;
                case DROP_KEY -> 60.0D;
                default -> 0.0D;
            };
            case BOWMASTER -> switch (inputType) {
                case OFFHAND_QUICK -> 10.0D;
                case LEFT_CLICK -> 20.0D;
                case SHIFT_LEFT_CLICK -> 60.0D;
                default -> 0.0D;
            };
            case LANCER -> switch (inputType) {
                case OFFHAND_QUICK -> 3.0D;
                case RIGHT_CLICK -> 6.0D;
                case DROP_KEY -> 60.0D;
                default -> 0.0D;
            };
            case ASSASSIN -> switch (inputType) {
                case OFFHAND_QUICK -> 10.0D;
                case RIGHT_CLICK -> 30.0D;
                default -> 0.0D;
            };
            case WIZARD -> switch (inputType) {
                case LEFT_CLICK -> 0.5D;
                case SHIFT_LEFT_CLICK -> 15.0D;
                case OFFHAND_QUICK -> 3.0D;
                case RIGHT_CLICK -> 8.0D;
                default -> 0.0D;
            };
        };
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }

    private String formatSeconds(long millis) {
        return String.format(Locale.ROOT, "%.1f", millis / 1000.0D);
    }

    private Double parseSeconds(CommandSender sender, String input) {
        try {
            double seconds = Double.parseDouble(input);
            if (seconds < 0.0D || seconds > 3600.0D) {
                sender.sendMessage(Component.text("쿨타임은 0초 이상 3600초 이하만 가능합니다.", NamedTextColor.RED));
                return null;
            }
            return seconds;
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("초 단위 숫자를 입력해야 합니다: " + input, NamedTextColor.RED));
            return null;
        }
    }

    private record CooldownKey(RPGClass rpgClass, SkillInputType inputType, double defaultSeconds) {
        private String id() {
            return rpgClass.id() + ":" + inputType.configKey();
        }
    }
}
