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

public final class ShopCommand implements CommandExecutor, TabCompleter {
    private final ShopGuiService guiService;
    private final ShopRegistry shopRegistry;

    public ShopCommand(ShopGuiService guiService, ShopRegistry shopRegistry) {
        this.guiService = guiService;
        this.shopRegistry = shopRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("이 명령어는 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("hyunseorpg.shop.use")) {
            player.sendMessage(Component.text("상점 이용 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Component.text("사용법: /" + label + " [상점 ID]", NamedTextColor.YELLOW));
            return true;
        }
        if (!guiService.openShop(player, args[0])) {
            player.sendMessage(Component.text("등록되지 않은 상점입니다: " + args[0], NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return shopRegistry.getAll().stream().map(shop -> shop.shopId())
                .filter(id -> id.startsWith(prefix)).toList();
    }
}
