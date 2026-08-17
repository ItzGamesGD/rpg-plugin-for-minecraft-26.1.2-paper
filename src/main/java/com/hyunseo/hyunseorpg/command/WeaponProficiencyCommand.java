package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.weapon.WeaponProficiencyService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class WeaponProficiencyCommand implements CommandExecutor, TabCompleter {
    private final WeaponProficiencyService proficiencyService;

    public WeaponProficiencyCommand(WeaponProficiencyService proficiencyService) {
        this.proficiencyService = proficiencyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 1) {
            WeaponType type = WeaponType.fromInput(args[0]).orElse(null);
            if (type == null) {
                player.sendMessage(Component.text("알 수 없는 무기 종류입니다.", NamedTextColor.RED));
                return true;
            }
            sendInfo(player, type);
            return true;
        }
        player.sendMessage(Component.text("무기 숙련도", NamedTextColor.GOLD));
        for (WeaponType type : WeaponType.values()) {
            sendInfo(player, type);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Arrays.stream(WeaponType.values()).map(WeaponType::id).filter(id -> id.startsWith(prefix)).toList();
    }

    private void sendInfo(Player player, WeaponType type) {
        int level = proficiencyService.getLevel(player, type);
        long current = proficiencyService.getExperience(player, type);
        long required = proficiencyService.getRequiredExperience(level);
        player.sendMessage(Component.text(type.displayName() + ": Lv." + level + " (" + current + "/" + required + " 경험치)", NamedTextColor.AQUA));
    }
}
