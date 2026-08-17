package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.exp.ClassLevelUpResult;
import com.hyunseo.hyunseorpg.exp.ExpService;
import com.hyunseo.hyunseorpg.exp.LevelService;
import com.hyunseo.hyunseorpg.exp.LevelUpResult;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Administrator controls for base and combat specialization progression. */
public final class RPGLevelAdminCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "hyunseorpg.admin";

    private final PlayerDataService playerDataService;
    private final ExpService expService;
    private final LevelService levelService;

    public RPGLevelAdminCommand(PlayerDataService playerDataService, ExpService expService, LevelService levelService) {
        this.playerDataService = playerDataService;
        this.expService = expService;
        this.levelService = levelService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(Component.text("이 명령어는 게임 안의 관리자만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        if (!admin.isOp() && !admin.hasPermission(PERMISSION)) {
            admin.sendMessage(Component.text("관리자 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equals("?") || args[0].equalsIgnoreCase("help")) {
            sendHelp(admin, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "get" -> get(admin, label, args);
            case "addexp" -> addBaseExp(admin, label, args);
            case "addclassexp" -> addSpecializationExp(admin, label, args);
            case "setlevel" -> setBaseLevel(admin, label, args);
            case "setexp" -> setBaseExp(admin, label, args);
            case "setclasslevel" -> setSpecializationLevel(admin, label, args);
            case "setclassexp" -> setSpecializationExp(admin, label, args);
            case "points" -> points(admin, label, args);
            case "refresh" -> refresh(admin, label, args);
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
            return filter(List.of("get", "addexp", "addclassexp", "setlevel", "setexp", "setclasslevel", "setclassexp", "points", "refresh"), args[0]);
        }
        if (args.length == 2) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("points")) {
            return filter(List.of("get", "set", "add"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("points")) {
            return filter(List.of("stat", "skill", "weaponstat"), args[3]);
        }
        return List.of();
    }

    private boolean get(Player admin, String label, String[] args) {
        Player target = requireTarget(admin, label, args, "get [플레이어]");
        if (target != null) {
            sendInfo(admin, target);
        }
        return true;
    }

    private boolean addBaseExp(Player admin, String label, String[] args) {
        Player target = requireTargetAndAmount(admin, label, args, "addexp [플레이어] [경험치]");
        if (target == null) return true;
        long amount = Long.parseLong(args[2]);
        LevelUpResult result = expService.giveBaseExp(target, amount);
        admin.sendMessage(Component.text(target.getName() + "에게 기본 경험치 " + amount + "을 지급했습니다. Lv."
                + result.oldLevel() + " -> Lv." + result.newLevel(), NamedTextColor.GREEN));
        return true;
    }

    private boolean addSpecializationExp(Player admin, String label, String[] args) {
        Player target = requireTargetAndAmount(admin, label, args, "addclassexp [플레이어] [경험치]");
        if (target == null) return true;
        long amount = Long.parseLong(args[2]);
        ClassLevelUpResult result = expService.giveClassExp(target, amount);
        admin.sendMessage(Component.text(target.getName() + "에게 전문화 경험치 " + amount + "을 지급했습니다. Lv."
                + result.oldLevel() + " -> Lv." + result.newLevel()
                + ", 스킬 포인트 +" + result.skillPointsGained()
                + ", 무기 전문화 포인트 +" + result.classStatPointsGained(), NamedTextColor.GREEN));
        return true;
    }

    private boolean setBaseLevel(Player admin, String label, String[] args) {
        Player target = requireTargetAndAmount(admin, label, args, "setlevel [플레이어] [레벨]");
        if (target == null) return true;
        levelService.setBaseLevel(target, Integer.parseInt(args[2]));
        admin.sendMessage(Component.text(target.getName() + "의 기본 레벨을 설정했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean setBaseExp(Player admin, String label, String[] args) {
        Player target = requireTargetAndAmount(admin, label, args, "setexp [플레이어] [경험치]");
        if (target == null) return true;
        levelService.setBaseExp(target, Long.parseLong(args[2]));
        admin.sendMessage(Component.text(target.getName() + "의 기본 경험치를 설정했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean setSpecializationLevel(Player admin, String label, String[] args) {
        Player target = requireTargetAndAmount(admin, label, args, "setclasslevel [플레이어] [레벨]");
        if (target == null) return true;
        levelService.setClassLevel(target, Integer.parseInt(args[2]));
        admin.sendMessage(Component.text(target.getName() + "의 전문화 레벨을 설정했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean setSpecializationExp(Player admin, String label, String[] args) {
        Player target = requireTargetAndAmount(admin, label, args, "setclassexp [플레이어] [경험치]");
        if (target == null) return true;
        levelService.setClassExp(target, Long.parseLong(args[2]));
        admin.sendMessage(Component.text(target.getName() + "의 전문화 경험치를 설정했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean points(Player admin, String label, String[] args) {
        if (args.length < 4) {
            admin.sendMessage(Component.text("사용법: /" + label + " points [플레이어] <get|set|add> <stat|skill|weaponstat> [수량]", NamedTextColor.YELLOW));
            return true;
        }
        Player target = findTarget(admin, args[1]);
        if (target == null) return true;
        PlayerRPGData data = playerDataService.getOrLoad(target);
        String action = args[2].toLowerCase(Locale.ROOT);
        String type = args[3].toLowerCase(Locale.ROOT);
        if (action.equals("get")) {
            sendPoints(admin, target, data);
            return true;
        }
        if (!action.equals("set") && !action.equals("add") || args.length < 5) {
            admin.sendMessage(Component.text("사용법: /" + label + " points [플레이어] <set|add> <stat|skill|weaponstat> [수량]", NamedTextColor.YELLOW));
            return true;
        }
        Integer amount = parseInt(admin, args[4]);
        if (amount == null) return true;
        boolean add = action.equals("add");
        switch (type) {
            case "stat" -> data.setStatPoints(add ? data.getStatPoints() + amount : Math.max(0, amount));
            case "skill" -> data.setSkillPoints(add ? data.getSkillPoints() + amount : Math.max(0, amount));
            case "weaponstat", "classstat" -> data.setClassStatPoints(add ? data.getClassStatPoints() + amount : Math.max(0, amount));
            default -> {
                admin.sendMessage(Component.text("포인트 종류는 stat, skill, weaponstat 중 하나여야 합니다.", NamedTextColor.RED));
                return true;
            }
        }
        playerDataService.savePlayer(target);
        sendPoints(admin, target, data);
        return true;
    }

    private boolean refresh(Player admin, String label, String[] args) {
        Player target = requireTarget(admin, label, args, "refresh [플레이어]");
        if (target != null) {
            levelService.refreshVanillaExpBar(target);
            admin.sendMessage(Component.text(target.getName() + "의 경험치 표시줄을 갱신했습니다.", NamedTextColor.GREEN));
        }
        return true;
    }

    private Player requireTargetAndAmount(Player admin, String label, String[] args, String syntax) {
        if (args.length < 3) {
            admin.sendMessage(Component.text("사용법: /" + label + " " + syntax, NamedTextColor.YELLOW));
            return null;
        }
        Player target = findTarget(admin, args[1]);
        if (target == null) return null;
        return parseLong(admin, args[2]) == null ? null : target;
    }

    private Player requireTarget(Player admin, String label, String[] args, String syntax) {
        if (args.length < 2) {
            admin.sendMessage(Component.text("사용법: /" + label + " " + syntax, NamedTextColor.YELLOW));
            return null;
        }
        return findTarget(admin, args[1]);
    }

    private Player findTarget(Player admin, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) admin.sendMessage(Component.text("온라인 플레이어를 찾을 수 없습니다: " + name, NamedTextColor.RED));
        return target;
    }

    private Integer parseInt(Player admin, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            admin.sendMessage(Component.text("정수를 입력해야 합니다: " + value, NamedTextColor.RED));
            return null;
        }
    }

    private Long parseLong(Player admin, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            admin.sendMessage(Component.text("정수를 입력해야 합니다: " + value, NamedTextColor.RED));
            return null;
        }
    }

    private void sendInfo(Player admin, Player target) {
        PlayerRPGData data = playerDataService.getOrLoad(target);
        admin.sendMessage(Component.text(target.getName() + " 성장 정보", NamedTextColor.GOLD));
        admin.sendMessage(Component.text("기본 레벨: " + data.getBaseLevel() + " | 경험치: " + data.getBaseExp()
                + "/" + levelService.getRequiredExpForNextLevel(data) + " | 스탯 포인트: " + data.getStatPoints(), NamedTextColor.GRAY));
        admin.sendMessage(Component.text("전문화 레벨: " + data.getClassLevel() + " | 전문화 경험치: " + data.getClassExp()
                + "/" + levelService.getRequiredClassExpForNextLevel(data) + " | 스킬 포인트: " + data.getSkillPoints()
                + " | 무기 전문화 포인트: " + data.getClassStatPoints(), NamedTextColor.GRAY));
    }

    private void sendPoints(Player admin, Player target, PlayerRPGData data) {
        admin.sendMessage(Component.text(target.getName() + " 포인트 - 스탯: " + data.getStatPoints()
                + ", 스킬: " + data.getSkillPoints() + ", 무기 전문화: " + data.getClassStatPoints(), NamedTextColor.AQUA));
    }

    private void sendHelp(Player admin, String label) {
        admin.sendMessage(Component.text("HyunseoRPG 성장 관리자 명령어", NamedTextColor.GOLD));
        admin.sendMessage(Component.text("/" + label + " get [플레이어]", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " addexp [플레이어] [경험치]", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " addclassexp [플레이어] [경험치] - 전문화 경험치", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " setlevel|setexp|setclasslevel|setclassexp [플레이어] [값]", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " points [플레이어] <get|set|add> <stat|skill|weaponstat> [수량]", NamedTextColor.YELLOW));
        admin.sendMessage(Component.text("/" + label + " refresh [플레이어]", NamedTextColor.YELLOW));
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();
    }
}
