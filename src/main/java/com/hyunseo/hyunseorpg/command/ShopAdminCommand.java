package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.shop.ShopGuiService;
import com.hyunseo.hyunseorpg.shop.ShopRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class ShopAdminCommand implements CommandExecutor, TabCompleter {
    private final ShopGuiService guiService;
    private final ShopRegistry shopRegistry;

    public ShopAdminCommand(ShopGuiService guiService, ShopRegistry shopRegistry) {
        this.guiService = guiService;
        this.shopRegistry = shopRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("이 명령어는 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("hyunseorpg.shop.admin")) {
            player.sendMessage(Component.text("상점 관리자 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            guiService.reload();
            player.sendMessage(Component.text("shops.yml을 다시 불러왔습니다.", NamedTextColor.GREEN));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            if (!guiService.openAdmin(player, args[1])) player.sendMessage(Component.text("등록되지 않은 상점입니다: " + args[1], NamedTextColor.RED));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("save")) {
            boolean saved = guiService.saveSession(player, args[1]);
            player.sendMessage(Component.text(saved ? "상점을 저장했습니다." : "열려 있는 해당 상점 편집 세션이 없습니다.",
                    saved ? NamedTextColor.GREEN : NamedTextColor.RED));
            return true;
        }
        player.sendMessage(Component.text("사용법: /" + label + " <edit [상점 ID]|save [상점 ID]|reload>", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("edit", "save", "reload"), args[0]);
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return filter(shopRegistry.getAll().stream().map(shop -> shop.shopId()).toList(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();
    }
}
