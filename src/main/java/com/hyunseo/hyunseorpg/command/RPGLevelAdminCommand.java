package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.exp.ExpService;
import com.hyunseo.hyunseorpg.exp.LevelService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Locale;

/** Operator controls for the retained RPG level only. */
public final class RPGLevelAdminCommand implements CommandExecutor, TabCompleter {
    private final PlayerDataService dataService; private final ExpService expService; private final LevelService levelService;
    public RPGLevelAdminCommand(PlayerDataService d, ExpService e, LevelService l) { dataService=d; expService=e; levelService=l; }
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player admin) || (!admin.isOp() && !admin.hasPermission("hyunseorpg.admin"))) { sender.sendMessage(Component.text("관리자 권한이 없습니다.", NamedTextColor.RED)); return true; }
        if (args.length<1 || args[0].equalsIgnoreCase("help")) { help(admin,label); return true; }
        if (args.length<2) { help(admin,label); return true; }
        Player target=Bukkit.getPlayerExact(args[1]); if(target==null){admin.sendMessage(Component.text("온라인 플레이어를 찾을 수 없습니다.",NamedTextColor.RED));return true;}
        try {
            switch(args[0].toLowerCase(Locale.ROOT)) {
                case "get" -> info(admin,target);
                case "addexp" -> { if(args.length<3){help(admin,label);return true;} expService.giveBaseExp(target,Long.parseLong(args[2])); info(admin,target); }
                case "setlevel" -> { if(args.length<3){help(admin,label);return true;} levelService.setBaseLevel(target,Integer.parseInt(args[2])); info(admin,target); }
                case "setexp" -> { if(args.length<3){help(admin,label);return true;} levelService.setBaseExp(target,Long.parseLong(args[2])); info(admin,target); }
                case "refresh" -> levelService.refreshVanillaExpBar(target);
                default -> help(admin,label);
            }
        } catch(NumberFormatException ex) { admin.sendMessage(Component.text("숫자를 입력해야 합니다.",NamedTextColor.RED)); }
        return true;
    }
    private void info(Player admin, Player target){ PlayerRPGData d=dataService.getOrLoad(target); admin.sendMessage(Component.text(target.getName()+" RPG Lv."+d.getBaseLevel()+" EXP "+d.getBaseExp()+"/"+levelService.getRequiredExpForNextLevel(d),NamedTextColor.GOLD)); }
    private void help(Player p,String l){p.sendMessage(Component.text("/"+l+" <get|addexp|setlevel|setexp|refresh> [플레이어] [값]",NamedTextColor.YELLOW));}
    public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){ if(args.length==1)return filter(List.of("get","addexp","setlevel","setexp","refresh"),args[0]); if(args.length==2)return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]); return List.of(); }
    private List<String> filter(List<String> v,String p){String n=p.toLowerCase(Locale.ROOT);return v.stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(n)).toList();}
}
