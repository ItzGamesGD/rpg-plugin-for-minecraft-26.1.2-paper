package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.classsystem.ClassSelectionResult;
import com.hyunseo.hyunseorpg.classsystem.ClassService;
import com.hyunseo.hyunseorpg.classsystem.RPGClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ClassSelectCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "hyunseorpg.class.select";

    private final ClassService classService;

    public ClassSelectCommand(ClassService classService) {
        this.classService = classService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("이 명령어는 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("직업 선택 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || isHelp(args[0])) {
            sendHelp(player, label);
            return true;
        }

        Optional<RPGClass> rpgClass = RPGClass.fromInput(args[0]);
        if (rpgClass.isEmpty()) {
            player.sendMessage(Component.text("알 수 없는 직업입니다: " + args[0], NamedTextColor.RED));
            sendClassList(player);
            return true;
        }

        ClassSelectionResult result = classService.selectClass(player, rpgClass.get());
        if (result.status() == ClassSelectionResult.Status.ALREADY_SELECTED) {
            player.sendMessage(Component.text("이미 직업을 선택했습니다: " + result.selectedClass().koreanName(), NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("직업을 선택했습니다: " + result.selectedClass().koreanName(), NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String normalizedPrefix = args[0].toLowerCase(Locale.ROOT);
        return RPGClass.commandIds().stream()
                .filter(classId -> classId.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(Component.text("HyunseoRPG 직업 선택", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/" + label + " <직업>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("예: /" + label + " swordmaster", NamedTextColor.GRAY));
        player.sendMessage(Component.text("예: /" + label + " 소드마스터", NamedTextColor.GRAY));
        sendClassList(player);
    }

    private void sendClassList(Player player) {
        player.sendMessage(Component.text("직업: " + String.join(", ", RPGClass.commandIds()), NamedTextColor.AQUA));
    }

    private boolean isHelp(String value) {
        return value.equals("?") || value.equalsIgnoreCase("help");
    }
}
