package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.classsystem.ClassWeaponService;
import com.hyunseo.hyunseorpg.classsystem.RPGClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ClassWeaponCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "hyunseorpg.weapon.give";

    private final ClassWeaponService classWeaponService;

    public ClassWeaponCommand(ClassWeaponService classWeaponService) {
        this.classWeaponService = classWeaponService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("이 명령어는 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("직업 무기 지급 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || isHelp(args[0])) {
            sendHelp(player, label);
            return true;
        }

        String classInput = args[0];
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) {
                sendHelp(player, label);
                return true;
            }
            classInput = args[1];
        }

        if (classInput.equalsIgnoreCase("all")) {
            classWeaponService.giveAllWeapons(player);
            return true;
        }

        Optional<RPGClass> rpgClass = RPGClass.fromInput(classInput);
        if (rpgClass.isEmpty()) {
            player.sendMessage(Component.text("알 수 없는 직업입니다: " + classInput, NamedTextColor.RED));
            sendClassList(player);
            return true;
        }

        classWeaponService.giveWeapon(player, rpgClass.get());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> candidates = new ArrayList<>();
            candidates.add("all");
            candidates.add("give");
            candidates.addAll(RPGClass.commandIds());
            return filter(candidates, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> candidates = new ArrayList<>();
            candidates.add("all");
            candidates.addAll(RPGClass.commandIds());
            return filter(candidates, args[1]);
        }

        return List.of();
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(Component.text("HyunseoRPG 직업 무기 명령어", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/" + label + " <직업|all>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("예: /" + label + " swordmaster", NamedTextColor.GRAY));
        player.sendMessage(Component.text("예: /" + label + " 소드마스터", NamedTextColor.GRAY));
        player.sendMessage(Component.text("예: /" + label + " all", NamedTextColor.GRAY));
        sendClassList(player);
    }

    private void sendClassList(Player player) {
        player.sendMessage(Component.text("직업: " + String.join(", ", RPGClass.commandIds()), NamedTextColor.AQUA));
    }

    private boolean isHelp(String value) {
        return value.equals("?") || value.equalsIgnoreCase("help");
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }
}
