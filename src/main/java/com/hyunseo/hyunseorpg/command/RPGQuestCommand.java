package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.quest.AutoQuestData;
import com.hyunseo.hyunseorpg.quest.AutoQuestService;
import com.hyunseo.hyunseorpg.quest.AutoQuestType;
import com.hyunseo.hyunseorpg.quest.QuestData;
import com.hyunseo.hyunseorpg.quest.QuestService;
import com.hyunseo.hyunseorpg.quest.availability.ContentAvailabilityService;
import com.hyunseo.hyunseorpg.quest.availability.ItemObtainabilityContext;
import com.hyunseo.hyunseorpg.quest.availability.ItemObtainabilityResult;
import com.hyunseo.hyunseorpg.quest.availability.MonsterEligibilityContext;
import com.hyunseo.hyunseorpg.quest.availability.MonsterEligibilityResult;
import com.hyunseo.hyunseorpg.quest.availability.PlayerDiscoveryService;
import com.hyunseo.hyunseorpg.quest.availability.QuestTargetCandidate;
import com.hyunseo.hyunseorpg.ui.RPGMenuService;
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
import java.util.Optional;

/** /quest is a GUI entry point; retained text subcommands serve accessibility and administration. */
public final class RPGQuestCommand implements CommandExecutor, TabCompleter {
    private final QuestService questService;
    private final AutoQuestService autoQuestService;
    private final RPGMenuService menu;
    private final ContentAvailabilityService availability;
    private final PlayerDiscoveryService discovery;

    public RPGQuestCommand(QuestService questService) {
        this(questService, null, null, null, null);
    }

    public RPGQuestCommand(QuestService questService, AutoQuestService autoQuestService) {
        this(questService, autoQuestService, null, null, null);
    }

    public RPGQuestCommand(QuestService questService, AutoQuestService autoQuestService, RPGMenuService menu,
                           ContentAvailabilityService availability, PlayerDiscoveryService discovery) {
        this.questService = questService;
        this.autoQuestService = autoQuestService;
        this.menu = menu;
        this.availability = availability;
        this.discovery = discovery;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) return handleAdmin(sender, args);
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command requires a player target.");
            return true;
        }
        if (!player.hasPermission("hyunseorpg.quest") && !player.hasPermission("hyunseorpg.admin")) {
            player.sendMessage(Component.text("퀘스트 명령어 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            openGui(player);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                openGui(player);
                yield true;
            }
            case "accept" -> autoAccept(player);
            case "complete" -> autoComplete(player, label, args);
            case "abandon" -> autoAbandon(player, label, args);
            case "start" -> staticStart(player, label, args);
            case "info" -> staticInfo(player, label, args);
            case "claim" -> staticClaim(player, label, args);
            case "cancel" -> staticCancel(player, label, args);
            default -> {
                sendHelp(player, label);
                yield true;
            }
        };
    }

    private boolean autoAccept(Player player) {
        if (autoQuestService == null || !autoQuestService.accept(player)) {
            player.sendMessage(Component.text("현재는 새 퀘스트를 받을 수 없습니다. 슬롯과 대기 시간을 확인하세요.", NamedTextColor.YELLOW));
        }
        openGui(player);
        return true;
    }

    private boolean autoComplete(Player player, String label, String[] args) {
        int slot = slot(player, label, args, "complete");
        if (slot > 0 && (autoQuestService == null || !autoQuestService.complete(player, slot))) {
            player.sendMessage(Component.text("아직 완료할 수 없는 퀘스트입니다.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean autoAbandon(Player player, String label, String[] args) {
        int slot = slot(player, label, args, "abandon");
        if (slot > 0 && (autoQuestService == null || !autoQuestService.abandon(player, slot))) {
            player.sendMessage(Component.text("해당 퀘스트를 찾을 수 없습니다.", NamedTextColor.RED));
        }
        return true;
    }

    private int slot(Player player, String label, String[] args, String action) {
        if (args.length < 2) {
            player.sendMessage(Component.text("사용법: /" + label + " " + action + " <슬롯>", NamedTextColor.YELLOW));
            return -1;
        }
        try {
            return Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("슬롯은 숫자여야 합니다.", NamedTextColor.RED));
            return -1;
        }
    }

    private boolean staticStart(Player player, String label, String[] args) {
        if (args.length < 2 || !questService.startQuest(player, args[1])) {
            player.sendMessage(Component.text("사용법 또는 퀘스트 조건을 확인하세요: /" + label + " start <questId>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean staticInfo(Player player, String label, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("사용법: /" + label + " info <questId>", NamedTextColor.YELLOW));
            return true;
        }
        Optional<QuestData> quest = questService.getQuest(args[1]);
        if (quest.isEmpty()) {
            player.sendMessage(Component.text("등록되지 않은 퀘스트입니다.", NamedTextColor.RED));
            return true;
        }
        player.sendMessage(Component.text(quest.get().displayName() + " / " + questService.getQuestState(player, quest.get().questId()), NamedTextColor.AQUA));
        return true;
    }

    private boolean staticClaim(Player player, String label, String[] args) {
        if (args.length < 2 || !questService.claimReward(player, args[1])) {
            player.sendMessage(Component.text("사용법 또는 완료 상태를 확인하세요: /" + label + " claim <questId>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean staticCancel(Player player, String label, String[] args) {
        if (args.length < 2 || !questService.cancelQuest(player, args[1])) {
            player.sendMessage(Component.text("사용법 또는 진행 상태를 확인하세요: /" + label + " cancel <questId>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hyunseorpg.admin")) {
            sender.sendMessage(Component.text("관리자 권한이 필요합니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("/quest admin <inspect|generate|clear|cooldown|discover-mob|eligible-mobs|eligible-items|check-mob|check-item>", NamedTextColor.YELLOW));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage(Component.text("대상 플레이어가 필요합니다.", NamedTextColor.RED));
            return true;
        }
        switch (action) {
            case "inspect" -> {
                List<AutoQuestData> quests = autoQuestService.getQuests(target);
                sender.sendMessage(Component.text("퀘스트 inspect: " + target.getName(), NamedTextColor.GOLD));
                for (AutoQuestData quest : quests) sender.sendMessage(Component.text("[" + quest.slot() + "] "
                        + quest.type() + " " + quest.targetSource() + " " + quest.target() + " "
                        + autoQuestService.displayedProgress(target, quest) + "/" + quest.amount()
                        + " status=" + quest.status(), NamedTextColor.GRAY));
            }
            case "generate" -> {
                AutoQuestType type = args.length >= 4 ? parseType(args[3]) : null;
                if (autoQuestService.accept(target, type)) sender.sendMessage(Component.text("퀘스트 생성 완료", NamedTextColor.GREEN));
                else sender.sendMessage(Component.text("생성 가능한 퀘스트가 없습니다.", NamedTextColor.YELLOW));
            }
            case "clear" -> {
                autoQuestService.clear(target);
                sender.sendMessage(Component.text("활성 퀘스트를 제거했습니다.", NamedTextColor.GREEN));
            }
            case "cooldown" -> {
                autoQuestService.resetCooldown(target);
                sender.sendMessage(Component.text("퀘스트 대기 시간을 초기화했습니다.", NamedTextColor.GREEN));
            }
            case "discover-mob" -> {
                if (args.length < 4 || discovery == null) sender.sendMessage(Component.text("/quest admin discover-mob <player> <mobId>", NamedTextColor.YELLOW));
                else {
                    discovery.discover(target, args[3]);
                    sender.sendMessage(Component.text("몬스터 발견 기록을 추가했습니다.", NamedTextColor.GREEN));
                }
            }
            case "eligible-mobs" -> sendCandidates(sender, availability == null ? List.of() : availability.eligibleHuntTargets(target));
            case "eligible-items" -> sendCandidates(sender, availability == null ? List.of() : availability.eligibleItemTargets(target));
            case "check-mob" -> checkMob(sender, target, args);
            case "check-item" -> checkItem(sender, target, args);
            default -> sender.sendMessage(Component.text("알 수 없는 quest admin 작업입니다.", NamedTextColor.RED));
        }
        return true;
    }

    private void sendCandidates(CommandSender sender, List<QuestTargetCandidate> candidates) {
        sender.sendMessage(Component.text("생성 가능 후보: " + candidates.size(), NamedTextColor.GOLD));
        for (QuestTargetCandidate candidate : candidates) {
            sender.sendMessage(Component.text(candidate.source() + " " + candidate.id() + " / " + candidate.displayName(), NamedTextColor.GRAY));
        }
    }

    private void checkMob(CommandSender sender, Player target, String[] args) {
        if (availability == null || args.length < 4) {
            sender.sendMessage(Component.text("/quest admin check-mob <player> <mobId>", NamedTextColor.YELLOW));
            return;
        }
        MonsterEligibilityResult result = availability.checkMonster(target, args[3], MonsterEligibilityContext.QUEST_TARGET);
        sender.sendMessage(Component.text(result.monsterId() + ": " + result.reason() + " / " + result.detail(),
                result.eligible() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
    }

    private void checkItem(CommandSender sender, Player target, String[] args) {
        if (availability == null || args.length < 4) {
            sender.sendMessage(Component.text("/quest admin check-item <player> <itemId>", NamedTextColor.YELLOW));
            return;
        }
        ItemObtainabilityResult result = availability.checkItem(target, args[3], ItemObtainabilityContext.QUEST_TARGET);
        sender.sendMessage(Component.text(result.itemId() + ": " + result.reason() + " / " + result.detail()
                + " / " + result.acquisitionSources(), result.obtainable() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
    }

    private AutoQuestType parseType(String raw) {
        try { return AutoQuestType.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private void openGui(Player player) {
        if (menu != null) menu.openQuests(player);
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(Component.text("/" + label + " - 퀘스트 메뉴 열기", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " accept | complete <slot> | abandon <slot>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " start|info|claim|cancel <questId>", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("list", "accept", "complete", "abandon", "start", "info", "claim", "cancel", "admin"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) return filter(List.of(
                "inspect", "generate", "clear", "cooldown", "discover-mob", "eligible-mobs", "eligible-items", "check-mob", "check-item"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("generate")) return List.of("HUNT", "ITEM_DELIVERY");
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("discover-mob")) return List.of();
        if (args.length == 2 && List.of("start", "info", "claim", "cancel").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(questService.getQuests().stream().map(QuestData::questId).toList(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();
    }
}
