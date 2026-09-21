package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.item.RPGItemRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.item.PendingRewardService;
import com.hyunseo.hyunseorpg.core.config.RPGReloadService;
import com.hyunseo.hyunseorpg.core.config.ConfigDoctor;
import com.hyunseo.hyunseorpg.core.config.ConfigMigrationService;
import com.hyunseo.hyunseorpg.crafting.SoulboundItemService;
import com.hyunseo.hyunseorpg.weapon.WeaponItemService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import com.hyunseo.hyunseorpg.mob.variant.ZombieVariant;
import com.hyunseo.hyunseorpg.mob.variant.ZombieVariantService;
import com.hyunseo.hyunseorpg.alchemy.ActiveEffectInstance;
import com.hyunseo.hyunseorpg.alchemy.EffectContext;
import com.hyunseo.hyunseorpg.alchemy.EffectService;
import com.hyunseo.hyunseorpg.alchemy.EffectSourceType;
import com.hyunseo.hyunseorpg.alchemy.AlchemyAuditLog;
import com.hyunseo.hyunseorpg.alchemy.catalyst.BoundedSpecialCatalystExecutionService;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionPdcContract;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionFactory;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionDefinition;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentData;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import com.hyunseo.hyunseorpg.ui.KoreanDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Administrative convenience entry point for YAML-defined custom items. */
public final class RPGGiveCommand implements CommandExecutor, TabCompleter {
    private final RPGItemRegistry itemRegistry;
    private final RPGItemService itemService;
    private final SoulboundItemService soulboundItemService;
    private final RPGReloadService reloadService;
    private final WeaponItemService weaponItemService;
    private final ZombieVariantService zombieVariantService;
    private ConfigDoctor configDoctor;
    private ConfigMigrationService configMigrationService;
    private PendingRewardService pendingRewards;
    private Consumer<Inventory> inventoryNormalizer = inventory -> { };
    private EffectService effectService;
    private PotionRegistry potionRegistry;
    private PaperPotionPdcContract potionPdc;
    private PotionFactory potionFactory;
    private SpecialEquipmentService specialEquipmentService;
    private BoundedSpecialCatalystExecutionService specialCatalystExecutions;
    private AlchemyAuditLog alchemyAuditLog;

    public RPGGiveCommand(RPGItemRegistry itemRegistry, RPGItemService itemService,
                          SoulboundItemService soulboundItemService, RPGReloadService reloadService,
                          WeaponItemService weaponItemService, ZombieVariantService zombieVariantService) {
        this.itemRegistry = itemRegistry;
        this.itemService = itemService;
        this.soulboundItemService = soulboundItemService;
        this.reloadService = reloadService;
        this.weaponItemService = weaponItemService;
        this.zombieVariantService = zombieVariantService;
    }

    public void setMaintenanceServices(ConfigDoctor configDoctor, ConfigMigrationService configMigrationService) {
        this.configDoctor = configDoctor;
        this.configMigrationService = configMigrationService;
    }

    public void setPendingRewardService(PendingRewardService pendingRewards) {
        this.pendingRewards = pendingRewards;
    }

    public void setEffectService(EffectService effectService) {
        this.effectService = effectService;
    }

    public void setAlchemyServices(PotionRegistry potionRegistry, PaperPotionPdcContract potionPdc,
                                   BoundedSpecialCatalystExecutionService specialCatalystExecutions,
                                   AlchemyAuditLog alchemyAuditLog) {
        this.potionRegistry = potionRegistry;
        this.potionPdc = potionPdc;
        this.specialCatalystExecutions = specialCatalystExecutions;
        this.alchemyAuditLog = alchemyAuditLog;
    }

    public void setPotionFactory(PotionFactory potionFactory) {
        this.potionFactory = potionFactory;
    }

    public void setSpecialEquipmentService(SpecialEquipmentService specialEquipmentService) {
        this.specialEquipmentService = specialEquipmentService;
    }

    public void setInventoryNormalizer(Consumer<Inventory> inventoryNormalizer) {
        this.inventoryNormalizer = inventoryNormalizer == null ? inventory -> { } : inventoryNormalizer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("사용법: /" + label
                    + " <give <itemId> [amount]|pending [claim]|reload [항목]|alchemy>", NamedTextColor.YELLOW));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("doctor")) {
            if (!sender.hasPermission("hyunseorpg.admin")) {
                sender.sendMessage("관리자 권한이 필요합니다.");
                return true;
            }
            if (configDoctor == null) {
                sender.sendMessage("Doctor 서비스가 준비되지 않았습니다.");
                return true;
            }
            String section = args.length >= 2 ? args[1] : "all";
            ConfigDoctor.DoctorReport report = configDoctor.run(section);
            File reportFile = configDoctor.writeReport(report);
            report.lines().forEach(sender::sendMessage);
            if (reportFile != null) sender.sendMessage("진단 보고서 저장: " + reportFile.getPath());
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("migrate")) {
            if (!sender.hasPermission("hyunseorpg.admin")) {
                sender.sendMessage("관리자 권한이 필요합니다.");
                return true;
            }
            if (configMigrationService == null) {
                sender.sendMessage("Migration 서비스가 준비되지 않았습니다.");
                return true;
            }
            String target = args.length >= 2 ? args[1] : "configs";
            boolean apply = args.length >= 3 && args[2].equalsIgnoreCase("--apply");
            ConfigMigrationService.MigrationReport report = configMigrationService.migrate(target, !apply);
            report.lines().forEach(sender::sendMessage);
            if (report.success() && apply && reloadService != null) {
                RPGReloadService.ReloadResult reload = reloadService.reload("all");
                sender.sendMessage(reload.message());
            }
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("hyunseorpg.admin")) {
                sender.sendMessage(Component.text("관리자 권한이 없습니다.", NamedTextColor.RED));
                return true;
            }
            if (reloadService == null) {
                sender.sendMessage(Component.text("리로드 서비스가 준비되지 않았습니다.", NamedTextColor.RED));
                return true;
            }
            String target = args.length >= 2 ? args[1] : "all";
            RPGReloadService.ReloadResult result = reloadService.reload(target);
            sender.sendMessage(Component.text(result.message(),
                    result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This debug command can only be used by a player.");
                return true;
            }
            if (!player.hasPermission("hyunseorpg.admin") || zombieVariantService == null) {
                player.sendMessage(Component.text("관리자 권한 또는 변종 서비스가 없습니다.", NamedTextColor.RED));
                return true;
            }
            handleDebug(player, args);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("pending")) {
            if (!(sender instanceof Player player)) return true;
            if (pendingRewards == null) {
                player.sendMessage(Component.text("미수령 보상 서비스가 준비되지 않았습니다.", NamedTextColor.RED));
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("claim")) {
                pendingRewards.claim(player);
            } else {
                player.sendMessage(Component.text("미수령 보상: " + pendingRewards.count(player.getUniqueId())
                        + "개. /rpg pending claim", NamedTextColor.YELLOW));
            }
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("effect")) {
            handleEffect(sender, args);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("alchemy")) {
            handleAlchemy(sender, args);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("hyunseorpg.item.give")) {
            player.sendMessage(Component.text("커스텀 아이템 지급 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            player.sendMessage(Component.text("사용법: /" + label + " <give <itemId> [amount]|pending [claim]|reload [항목]>", NamedTextColor.YELLOW));
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[2])));
            } catch (NumberFormatException exception) {
                player.sendMessage(Component.text("수량은 1부터 2304 사이의 정수여야 합니다.", NamedTextColor.RED));
                return true;
            }
        }
        String requestedId = args[1].trim().toLowerCase(Locale.ROOT);
        PotionDefinition potion = potionRegistry == null ? null : potionRegistry.find(requestedId).orElse(null);
        SpecialEquipmentData specialEquipment = findSpecialEquipment(requestedId);
        ItemStack prototype;
        if (potion != null || requiresCanonicalPotion(requestedId)) {
            // Potion IDs must never fall back to the generic item factory: it cannot write potion PDC.
            if (potion == null || !potion.enabled() || potionFactory == null) {
                player.sendMessage(Component.text("사용할 수 없는 포션 ID입니다: " + args[1], NamedTextColor.RED));
                return true;
            }
            prototype = potionFactory.create(potion, 1).orElse(null);
        } else if (specialEquipment != null) {
            if (!specialEquipment.enabled() || specialEquipmentService == null) {
                player.sendMessage(Component.text("사용할 수 없는 특수 장비 ID입니다: " + args[1], NamedTextColor.RED));
                return true;
            }
            // Special equipment must use its service so PDC, Loyalty, and lore stay canonical.
            prototype = specialEquipmentService.create(specialEquipment.id(), 1);
        } else {
            prototype = itemService.create(args[1], 1).orElse(null);
        }
        if (itemRegistry.get(args[1]).filter(data -> data.legacyProfessionItem()).isPresent()) {
            player.sendMessage(Component.text("이 직업 아이템은 레거시 호환용이라 새로 지급할 수 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (prototype == null && weaponItemService != null && args[1].toLowerCase(Locale.ROOT).startsWith("basic_")) {
            prototype = WeaponType.fromInput(args[1].substring("basic_".length()))
                    .map(weaponItemService::create).orElse(null);
        }
        if (prototype == null) {
            player.sendMessage(Component.text("등록되지 않은 커스텀 아이템입니다: " + args[1], NamedTextColor.RED));
            return true;
        }
        // Normalize existing inventory stacks before Bukkit attempts to merge the new item.
        inventoryNormalizer.accept(player.getInventory());
        while (amount > 0) {
            ItemStack stack = prototype.clone();
            int stackAmount = Math.min(amount, stack.getMaxStackSize());
            stack.setAmount(stackAmount);
            if (soulboundItemService.requiresBinding(stack)) {
                soulboundItemService.bind(stack, player.getUniqueId());
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            amount -= stackAmount;
        }
        player.sendMessage(Component.text("커스텀 아이템을 지급했습니다: " + args[1], NamedTextColor.GREEN));
        return true;
    }

    private SpecialEquipmentData findSpecialEquipment(String requestedId) {
        if (specialEquipmentService == null) return null;
        return specialEquipmentService.registry().get(requestedId)
                .orElseGet(() -> specialEquipmentService.registry().getAll().stream()
                        .filter(data -> data.itemId().equalsIgnoreCase(requestedId))
                        .findFirst().orElse(null));
    }

    private int parseAmount(String[] args, int index) {
        if (args.length <= index) return 1;
        Integer value = parseNonNegativeInt(args[index]);
        return value == null ? 0 : Math.min(2304, value);
    }

    private Integer parseNonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long parseNonNegativeLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed < 0L ? null : parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { return null; }
    }

    private void handleDebug(Player player, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("mob")) {
            player.sendMessage(Component.text("/rpg debug mob <variant|inspect|roll>", NamedTextColor.YELLOW));
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("inspect")) {
            Entity target = player.getTargetEntity(20);
            if (!(target instanceof LivingEntity living)) {
                player.sendMessage(Component.text("바라보는 생명체가 없습니다.", NamedTextColor.YELLOW));
                return;
            }
            player.sendMessage(Component.text("Zombie Variant Inspect", NamedTextColor.GOLD));
            player.sendMessage(Component.text("type=" + living.getType(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("rpg=" + zombieVariantService.isInitialized(living), NamedTextColor.GRAY));
            player.sendMessage(Component.text("variant=" + zombieVariantService.getVariant(living).orElse(ZombieVariant.NORMAL), NamedTextColor.GRAY));
            player.sendMessage(Component.text("spawnReason=" + zombieVariantService.getSpawnReason(living), NamedTextColor.GRAY));
            player.sendMessage(Component.text("level=" + zombieVariantService.getMobLevel(living), NamedTextColor.GRAY));
            player.sendMessage(Component.text("bombProcessed=" + zombieVariantService.isBombDeathProcessed(living), NamedTextColor.GRAY));
            player.sendMessage(Component.text("leapCooldownMs=" + zombieVariantService.getLeapCooldownRemainingMillis(living), NamedTextColor.GRAY));
            player.sendMessage(Component.text("health=" + living.getHealth(), NamedTextColor.GRAY));
            return;
        }
        if (action.equals("roll")) {
            int count = 1;
            if (args.length >= 5) {
                try {
                    count = Math.max(1, Math.min(10000, Integer.parseInt(args[4])));
                } catch (NumberFormatException exception) {
                    player.sendMessage(Component.text("count는 숫자여야 합니다.", NamedTextColor.RED));
                    return;
                }
            }
            Map<ZombieVariant, Integer> result = new java.util.EnumMap<>(ZombieVariant.class);
            for (int index = 0; index < count; index++) {
                ZombieVariant variant = zombieVariantService.rollForDebug();
                result.merge(variant, 1, Integer::sum);
            }
            player.sendMessage(Component.text("roll=" + result, NamedTextColor.AQUA));
            return;
        }
        ZombieVariant variant;
        try {
            variant = ZombieVariant.valueOf(action.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("variant는 normal, bomb, leap 중 하나여야 합니다.", NamedTextColor.RED));
            return;
        }
        Entity target = player.getTargetEntity(20);
        if (!(target instanceof LivingEntity living) || !zombieVariantService.forceVariant(living, variant)) {
            player.sendMessage(Component.text("바라보는 대상은 좀비여야 합니다.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("좀비 변종 적용: " + variant, NamedTextColor.GREEN));
    }

    /*
    private void handleEffectDebugHarness(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hyunseorpg.admin")) {
            sender.sendMessage("\uAD00\uB9AC\uC790 \uAD8C\uD55C\uC774 \uD544\uC694\uD569\uB2C8\uB2E4.");
            return;
        }
        if (effectService == null) {
            sender.sendMessage("\uC0C1\uD0DC\uD6A8\uACFC \uC11C\uBE44\uC2A4\uAC00 \uC900\uBE44\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4.");
            return;
        }
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "list";
        Player self = sender instanceof Player current ? current : null;
        if (action.equals("reload")) {
            boolean loaded = effectService.reload();
            sender.sendMessage(loaded ? "\uC0C1\uD0DC\uD6A8\uACFC \uC124\uC815\uC744 \uC7AC\uB85C\uB4DC\uD588\uC2B5\uB2C8\uB2E4." : "\uC0C1\uD0DC\uD6A8\uACFC \uC124\uC815 \uC801\uC6A9\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
            return;
        }
        if (action.equals("apply")) {
            if (args.length < 6) { sender.sendMessage("\uC0AC\uC6A9\uBC95: /rpg effect apply <player> <effect_id> <duration_ticks> <amplifier>"); return; }
            Player target = Bukkit.getPlayerExact(args[2]);
            Integer duration = parseEffectInteger(args[4]);
            Integer amplifier = parseEffectInteger(args[5]);
            if (target == null || duration == null || amplifier == null) { sender.sendMessage("\uB300\uC0C1 \uD50C\uB808\uC774\uC5B4\uC640 \uC815\uC218 \uC785\uB825\uC744 \uD655\uC778\uD558\uC138\uC694."); return; }
            if (effectService.registry().get(args[3]).isEmpty()) { sender.sendMessage("\uB4F1\uB85D\uB418\uC9C0 \uC54A\uC740 \ub610\ub294 \ube44\ud65c\uc131 \uc0c1\ud0DC\uD6A8\uACFC\uC785\uB2C8\uB2E4."); return; }
            UUID source = self == null ? target.getUniqueId() : self.getUniqueId();
            boolean applied = effectService.applyDebug(target.getUniqueId(), args[3],
                    new EffectContext(source, EffectSourceType.COMMAND, "rpg_effect_debug", target.getUniqueId(), null), duration, amplifier);
            sender.sendMessage(applied ? "\uB514\uBC84\uADF8 \uC0C1\uD0DC\uD6A8\uACFC\uB97C \uC801\uC6A9\uD588\uC2B5\uB2C8\uB2E4." : "\uC0C1\uD0DC\uD6A8\uACFC \uC801\uC6A9\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
            return;
        }
        if (action.equals("remove") || action.equals("clear")) {
            if (args.length < 3) { sender.sendMessage("\uC0AC\uC6A9\uBC95: /rpg effect " + action + " <player>" + (action.equals("remove") ? " <effect_id>" : "")); return; }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage("\uB300\uC0C1 \uD50C\uB808\uC774\uC5B4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."); return; }
            boolean changed = action.equals("clear") ? (effectService.clearAndReport(target.getUniqueId()) > 0)
                    : args.length >= 4 && effectService.remove(target.getUniqueId(), args[3]);
            sender.sendMessage(changed ? "\uC0C1\uD0DC\uD6A8\uACFC \uCC98\uB9AC\uB97C \uC644\uB8CC\uD588\uC2B5\uB2C8\uB2E4." : "\uBCC0\uACBD\uB41C \uC0C1\uD0DC\uD6A8\uACFC가 \uC5C6\uC2B5\uB2C8\uB2E4.");
            return;
        }
        if (action.equals("list") || action.equals("debug")) {
            if (self == null) { sender.sendMessage("\uD50C\uB808\uC774\uC5B4 \uB300\uC0C1\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."); return; }
            sender.sendMessage("\uD65C\uC131 \uC0C1\uD0DC\uD6A8\uACFC: " + effectService.getActive(self.getUniqueId()).size() + "\uAC1C");
            return;
        }
        sender.sendMessage("\uC0AC\uC6A9\uBC95: /rpg effect <list|apply|remove|clear|debug|reload>");
    }

    }
    */
    private void handleAlchemy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hyunseorpg.admin")) {
            sender.sendMessage("관리자 권한이 필요합니다.");
            return;
        }
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "inspect";
        if (action.equals("reload")) {
            if (reloadService == null) { sender.sendMessage("Reload 서비스가 준비되지 않았습니다."); return; }
            RPGReloadService.ReloadResult result = reloadService.reload("alchemy");
            sender.sendMessage(result.message());
            return;
        }
        if (action.equals("clearjobs")) {
            if (specialCatalystExecutions != null) specialCatalystExecutions.cancelAll(
                    com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystExecution.CancelReason.ADMIN_CANCEL);
            if (alchemyAuditLog != null) alchemyAuditLog.admin("clearjobs", sender instanceof Player p ? p.getUniqueId() : null, "all");
            sender.sendMessage("진행 중인 양조 작업을 정리했습니다.");
            return;
        }
        if (action.equals("give")) {
            if (args.length < 4) { sender.sendMessage("사용법: /rpg alchemy give <player> <potion_id> [amount]"); return; }
            Player target = Bukkit.getPlayerExact(args[2]);
            PotionDefinition definition = potionRegistry == null ? null : potionRegistry.find(args[3]).orElse(null);
            if (target == null || definition == null || !definition.enabled() || potionPdc == null) {
                sender.sendMessage("등록되지 않았거나 비활성화된 물약입니다.");
                return;
            }
            int amount = 1;
            if (args.length >= 5) {
                try { amount = Math.max(1, Math.min(64, Integer.parseInt(args[4]))); }
                catch (NumberFormatException ignored) { sender.sendMessage("수량은 1-64 정수여야 합니다."); return; }
            }
            ItemStack item = potionFactory == null ? null : potionFactory.create(definition, amount).orElse(null);
            if (item == null) { sender.sendMessage("물약 출력 아이템이 ItemRegistry에 없습니다."); return; }
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
            leftover.values().forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            if (alchemyAuditLog != null) alchemyAuditLog.admin("give", sender instanceof Player p ? p.getUniqueId() : null,
                    "target=" + target.getUniqueId() + " potion=" + definition.id() + " amount=" + amount);
            sender.sendMessage("양조 물약을 지급했습니다: " + definition.id());
            return;
        }
        if (action.equals("inspect")) {
            if (args.length >= 3 && args[2].equalsIgnoreCase("catalysts")) {
                if (specialCatalystExecutions == null) {
                    sender.sendMessage("특수 촉매 레지스트리가 준비되지 않았습니다.");
                } else {
                    sender.sendMessage("[촉매 진단] " + specialCatalystExecutions.catalystDiagnostic("sculk"));
                }
                return;
            }
            if (args.length < 3) { sender.sendMessage("사용법: /rpg alchemy inspect <player|catalysts>"); return; }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null || potionPdc == null) { sender.sendMessage("온라인 플레이어를 찾을 수 없습니다."); return; }
            ItemStack item = target.getInventory().getItemInMainHand();
            String potionId = potionPdc.readPotionId(item);
            String catalystId = potionPdc.readCatalystId(item);
            int version = potionPdc.readDataVersion(item);
            PotionDefinition definition = potionRegistry == null ? null : potionRegistry.find(potionId).orElse(null);
            sender.sendMessage("[양조 검사] potion=" + (potionId.isBlank() ? "없음" : potionId)
                    + ", registry=" + (definition == null ? "없음" : (definition.enabled() ? "활성" : "비활성"))
                    + ", catalyst=" + (catalystId.isBlank() ? "없음" : catalystId) + ", data-version=" + version);
            return;
        }
        sender.sendMessage("사용법: /rpg alchemy <reload|give|inspect|clearjobs>");
    }

    private Integer parseEffectInteger(String value) {
        try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; }
    }

    private Double parseEffectDouble(String value) {
        try { return Double.valueOf(value); } catch (NumberFormatException ignored) { return null; }
    }

    private void handleEffect(CommandSender sender, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "list";
        if (action.equals("list")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용할 수 있습니다.");
                return;
            }
            List<ActiveEffectInstance> active = effectService == null
                    ? List.of() : effectService.getActive(player.getUniqueId());
            if (active.isEmpty()) {
                sender.sendMessage("활성 상태효과가 없습니다.");
            } else {
                sender.sendMessage("활성 상태효과 (" + active.size() + "): ");
                active.forEach(instance -> sender.sendMessage("- " + instance.definition().displayName()
                        + " x" + instance.stacks()));
            }
            return;
        }
        if (!sender.hasPermission("hyunseorpg.admin")) {
            sender.sendMessage("관리자 권한이 필요합니다.");
            return;
        }
        if (effectService == null) {
            sender.sendMessage("상태효과 서비스가 준비되지 않았습니다.");
            return;
        }
        if (action.equals("reload")) {
            boolean loaded = effectService.load();
            sender.sendMessage(loaded ? "상태효과 설정을 다시 불러왔습니다: " + effectService.registry().getAll().size() + "개"
                    : "상태효과 설정을 적용하지 않았습니다: " + String.join("; ", effectService.registry().lastErrors()));
            return;
        }
        if (action.equals("apply")) {
            handleEffectApply(sender, args);
            return;
        }
        if (action.equals("remove")) {
            handleEffectRemove(sender, args);
            return;
        }
        if (action.equals("clear")) {
            handleEffectClear(sender, args);
            return;
        }
        if (action.equals("debug")) {
            handleEffectDebug(sender, args);
            return;
        }
        sender.sendMessage("사용법: /rpg effect <list|apply|remove|clear|debug|reload>");
    }

    private void handleEffectApply(CommandSender sender, String[] args) {
        Player commandPlayer = sender instanceof Player player ? player : null;
        Player target;
        String effectId;
        int durationIndex;
        int amplifierIndex;
        if (args.length == 3) {
            if (commandPlayer == null) {
                sender.sendMessage("사용법: /rpg effect apply <player> <effect_id> [duration_ticks] [amplifier]");
                return;
            }
            target = commandPlayer;
            effectId = args[2];
            durationIndex = -1;
            amplifierIndex = -1;
        } else {
            if (args.length < 4) {
                sender.sendMessage("사용법: /rpg effect apply <player> <effect_id> [duration_ticks] [amplifier]");
                return;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("대상 플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
            effectId = args[3];
            durationIndex = args.length >= 5 ? 4 : -1;
            amplifierIndex = args.length >= 6 ? 5 : -1;
            if (args.length > 6) {
                sender.sendMessage("사용법: /rpg effect apply <player> <effect_id> [duration_ticks] [amplifier]");
                return;
            }
        }
        var definition = effectService.registry().get(effectId).orElse(null);
        if (definition == null) {
            sender.sendMessage("등록되지 않았거나 비활성 상태효과입니다: " + effectId);
            return;
        }
        Integer duration = durationIndex < 0 ? definition.durationTicks() : parseEffectInteger(args[durationIndex]);
        Integer amplifier = amplifierIndex < 0 ? definition.amplifier() : parseEffectInteger(args[amplifierIndex]);
        if (duration == null || duration < 1 || duration > 72000) {
            sender.sendMessage("duration_ticks는 1-72000 범위의 정수여야 합니다.");
            return;
        }
        if (amplifier == null || amplifier < 0 || amplifier > 10) {
            sender.sendMessage("amplifier는 0-10 범위의 정수여야 합니다.");
            return;
        }
        UUID source = commandPlayer == null ? target.getUniqueId() : commandPlayer.getUniqueId();
        boolean applied = durationIndex < 0 && amplifierIndex < 0
                ? effectService.apply(target.getUniqueId(), effectId,
                new EffectContext(source, EffectSourceType.COMMAND, "rpg_effect", target.getUniqueId(), null))
                : effectService.applyDebug(target.getUniqueId(), effectId,
                new EffectContext(source, EffectSourceType.COMMAND, "rpg_effect", target.getUniqueId(), null), duration, amplifier);
        sender.sendMessage(applied ? "상태효과를 적용했습니다: " + target.getName() + " / " + effectId
                : "상태효과 적용에 실패했습니다: " + effectId);
    }

    private void handleEffectRemove(CommandSender sender, String[] args) {
        Player commandPlayer = sender instanceof Player player ? player : null;
        Player target;
        String effectId;
        if (args.length == 3 && commandPlayer != null) {
            target = commandPlayer;
            effectId = args[2];
        } else if (args.length == 4) {
            target = Bukkit.getPlayerExact(args[2]);
            effectId = args[3];
            if (target == null) {
                sender.sendMessage("대상 플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
        } else {
            sender.sendMessage("사용법: /rpg effect remove <player> <effect_id>");
            return;
        }
        sender.sendMessage(effectService.remove(target.getUniqueId(), effectId)
                ? "상태효과를 제거했습니다: " + target.getName() + " / " + effectId
                : "해당 상태효과가 없습니다: " + effectId);
    }

    private void handleEffectClear(CommandSender sender, String[] args) {
        Player commandPlayer = sender instanceof Player player ? player : null;
        Player target;
        if (args.length == 2 && commandPlayer != null) {
            target = commandPlayer;
        } else if (args.length == 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("대상 플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
        } else {
            sender.sendMessage("사용법: /rpg effect clear <player>");
            return;
        }
        int cleared = effectService.clearAndReport(target.getUniqueId());
        sender.sendMessage(target.getName() + "의 상태효과 " + cleared + "개를 제거했습니다.");
    }

    private void handleEffectDebug(CommandSender sender, String[] args) {
        if (args.length >= 5) {
            Player source = Bukkit.getPlayerExact(args[2]);
            Player target = Bukkit.getPlayerExact(args[3]);
            Double baseDamage = parseEffectDouble(args[4]);
            if (source == null || target == null) {
                sender.sendMessage("source와 target 플레이어를 찾을 수 없습니다.");
                return;
            }
            if (baseDamage == null || !Double.isFinite(baseDamage) || baseDamage < 0.0D) {
                sender.sendMessage("base_damage는 0 이상인 숫자여야 합니다.");
                return;
            }
            EffectService.DamageTrace trace = effectService.traceDamage(
                    source.getUniqueId(), target.getUniqueId(), baseDamage);
            double vampirismHeal = effectService.getActive(source.getUniqueId()).stream()
                    .anyMatch(instance -> instance.definition().id().equalsIgnoreCase("effect_vampire"))
                    ? trace.finalDamage() * 0.05D : 0.0D;
            sender.sendMessage("[상태효과 전투 미리보기] source=" + source.getName()
                    + ", target=" + target.getName()
                    + ", base=" + trace.baseDamage()
                    + ", after-outgoing=" + trace.afterOutgoing()
                    + ", after-incoming=" + trace.afterIncoming()
                    + ", final=" + trace.finalDamage()
                    + ", vampirism-heal=" + vampirismHeal);
            return;
        }
        Player target;
        if (args.length == 2 && sender instanceof Player player) {
            target = player;
        } else if (args.length == 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("대상 플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
        } else {
            sender.sendMessage("사용법: /rpg effect debug [player]");
            return;
        }
        List<ActiveEffectInstance> active = effectService.getActive(target.getUniqueId());
        sender.sendMessage("[감전 펄스] " + effectService.shockDebug(target.getUniqueId()));
        sender.sendMessage("[상태효과 디버그] target=" + target.getName() + ", active=" + active.size());
        for (ActiveEffectInstance instance : active) {
            boolean attributePresent = false;
            for (Attribute attribute : Attribute.values()) {
                var attributeInstance = target.getAttribute(attribute);
                if (attributeInstance != null && attributeInstance.getModifiers().stream()
                        .anyMatch(modifier -> modifier.getName().equals("hyunseorpg_alchemy_" + instance.definition().id()))) {
                    attributePresent = true;
                    break;
                }
            }
            sender.sendMessage("- effect=" + instance.definition().id()
                    + ", source=" + instance.source().type() + ":" + instance.source().sourceId()
                    + ", active=true, expires=" + instance.expiresAtTick()
                    + ", attribute-modifier=" + attributePresent);
        }
        if (active.isEmpty()) sender.sendMessage("활성 상태효과가 없습니다.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return rootCompletion(args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pending")) {
            return pendingCompletion(args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("effect")) {
            return effectActionCompletion(args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("alchemy")) {
            return filterCompletion(List.of("reload", "give", "inspect", "clearjobs"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("alchemy")
                && (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("inspect"))) {
            List<String> values = new java.util.ArrayList<>(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).toList());
            if (args[1].equalsIgnoreCase("inspect")) values.add("catalysts");
            return filterCompletion(values, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("alchemy") && args[1].equalsIgnoreCase("give")
                && potionRegistry != null) {
            return filterCompletion(potionRegistry.all().keySet(), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("alchemy") && args[1].equalsIgnoreCase("give")) {
            return List.of("1", "8", "16", "64");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("effect")
                && (args[1].equalsIgnoreCase("apply") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("clear"))) {
            return filterCompletion(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("effect") && args[1].equalsIgnoreCase("debug")) {
            return filterCompletion(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("effect") && args[1].equalsIgnoreCase("debug")) {
            return filterCompletion(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("effect") && args[1].equalsIgnoreCase("debug")) {
            return List.of("1", "5", "10", "20");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("effect")
                && (args[1].equalsIgnoreCase("apply") || args[1].equalsIgnoreCase("remove"))
                && effectService != null) {
            return filterCompletion(effectService.registry().getAll().keySet(), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("effect") && args[1].equalsIgnoreCase("apply")) {
            return List.of("120", "600", "1200", "72000");
        }
        if (args.length == 6 && args[0].equalsIgnoreCase("effect") && args[1].equalsIgnoreCase("apply")) {
            return List.of("0", "1", "2");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) return List.of("mob");
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("mob")) {
            return List.of("bomb", "leap", "normal", "inspect", "roll");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("debug") && args[2].equalsIgnoreCase("roll")) {
            return List.of("zombie");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reload") && reloadService != null) {
            return reloadCompletion(args[1], reloadService.ids());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("doctor")) {
            return List.of("all", "reload", "configs", "items", "equipment", "mobs", "players", "progression", "alchemy", "effects");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return migrationTargetCompletion(args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("migrate")) {
            return List.of("--dry-run", "--apply");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> ids = new java.util.ArrayList<>(itemRegistry.getAll().stream()
                    .filter(data -> !data.legacyProfessionItem())
                    .map(data -> data.itemId()).filter(id -> id.startsWith(prefix)).toList());
            for (WeaponType type : WeaponType.values()) {
                if (("basic_" + type.id()).startsWith(prefix)) ids.add("basic_" + type.id());
            }
            return ids;
        }
        return List.of();
    }

    static List<String> rootCompletion(String prefix) {
        return filterCompletion(List.of("give", "pending", "reload", "doctor", "migrate", "effect", "alchemy", "debug"), prefix);
    }

    static List<String> pendingCompletion(String prefix) {
        return filterCompletion(List.of("claim"), prefix);
    }

    static List<String> migrationTargetCompletion(String prefix) {
        return filterCompletion(List.of("configs", "items", "mobs", "players",
                "alchemy", "legacy", "cleanup", "all"), prefix);
    }

    static List<String> effectActionCompletion(String prefix) {
        return filterCompletion(List.of("list", "apply", "remove", "clear", "debug", "reload"), prefix);
    }

    static List<String> reloadCompletion(String prefix, String[] ids) {
        return filterCompletion(List.of(ids), prefix);
    }

    static List<String> giveCompletion(String prefix, Collection<String> itemIds) {
        return filterCompletion(itemIds, prefix);
    }

    static boolean requiresCanonicalPotion(String rawId) {
        return rawId != null && rawId.trim().toLowerCase(Locale.ROOT).startsWith("potion_");
    }

    private static List<String> filterCompletion(Collection<String> values, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value != null && value.startsWith(normalized)).toList();
    }
}
