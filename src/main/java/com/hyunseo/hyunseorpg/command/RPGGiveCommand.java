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
import com.hyunseo.hyunseorpg.farming.FarmingProfile;
import com.hyunseo.hyunseorpg.farming.FarmingProfileService;
import com.hyunseo.hyunseorpg.farming.FarmingPromotionService;
import com.hyunseo.hyunseorpg.farming.FarmingStage;
import com.hyunseo.hyunseorpg.farming.FarmingStatTokenService;
import com.hyunseo.hyunseorpg.farming.CropGrowthService;
import com.hyunseo.hyunseorpg.farming.CropQuality;
import com.hyunseo.hyunseorpg.farming.DeliveryProvider;
import com.hyunseo.hyunseorpg.farming.DeliveryService;
import com.hyunseo.hyunseorpg.farming.CropQualityService;
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
import com.hyunseo.hyunseorpg.exploration.ExplorationModule;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationStatusSnapshot;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
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
    private FarmingProfileService farmingProfiles;
    private FarmingPromotionService farmingPromotions;
    private PlayerDataService playerDataService;
    private FarmingStatTokenService farmingTokens;
    private CropGrowthService cropGrowthService;
    private DeliveryService deliveryService;
    private CropQualityService cropQualityService;
    private Consumer<Inventory> inventoryNormalizer = inventory -> { };
    private Consumer<String> farmingAuditLogger = ignored -> { };
    private EffectService effectService;
    private PotionRegistry potionRegistry;
    private PaperPotionPdcContract potionPdc;
    private PotionFactory potionFactory;
    private SpecialEquipmentService specialEquipmentService;
    private BoundedSpecialCatalystExecutionService specialCatalystExecutions;
    private AlchemyAuditLog alchemyAuditLog;
    private ExplorationModule explorationModule;

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

    public void setFarmingServices(FarmingProfileService farmingProfiles,
                                   FarmingPromotionService farmingPromotions,
                                   PlayerDataService playerDataService) {
        this.farmingProfiles = farmingProfiles;
        this.farmingPromotions = farmingPromotions;
        this.playerDataService = playerDataService;
    }

    public void setFarmingOperations(FarmingStatTokenService farmingTokens,
                                     CropGrowthService cropGrowthService) {
        this.farmingTokens = farmingTokens;
        this.cropGrowthService = cropGrowthService;
    }

    public void setFarmingDiagnostics(DeliveryService deliveryService, CropQualityService cropQualityService) {
        this.deliveryService = deliveryService;
        this.cropQualityService = cropQualityService;
    }

    public void setFarmingAuditLogger(Consumer<String> farmingAuditLogger) {
        this.farmingAuditLogger = farmingAuditLogger == null ? ignored -> { } : farmingAuditLogger;
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

    public void setExplorationModule(ExplorationModule explorationModule) {
        this.explorationModule = explorationModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("사용법: /" + label
                    + " <give <itemId> [amount]|pending [claim]|reload [항목]|farming|alchemy>", NamedTextColor.YELLOW));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("farming")) {
            if (args.length == 1) {
                sender.sendMessage("농사 기능은 작물, 농기구 및 월드 상호작용을 통해 이용합니다.");
                return true;
            }
            handleFarming(sender, args);
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
        if (args.length >= 1 && args[0].equalsIgnoreCase("exploration")) {
            handleExploration(sender, args);
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
        // Normalize persisted farming stacks before Bukkit attempts to merge the new item.
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

    private void handleExploration(CommandSender sender, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "status";
        if (action.equals("choose")) {
            if (explorationModule == null) {
                sender.sendMessage("탐험 모듈이 준비되지 않았습니다.");
                return;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("전초기지 선택은 플레이어만 할 수 있습니다.");
                return;
            }
            if (args.length < 4) {
                player.sendMessage("사용법: /rpg exploration choose <structure-uuid> <tier1|tier2|tier3|flee>");
                return;
            }
            UUID structureId = parseUuid(args[2], player);
            if (structureId == null) return;
            var result = explorationModule.choose(structureId, player, args[3]);
            switch (result) {
                case ACCEPTED -> player.sendMessage("선택한 난이도로 전초기지 습격을 시작합니다.");
                case FLED -> player.sendMessage("전투를 피했습니다. 약탈자들이 전초기지를 장악한 채 남아 있습니다.");
                case NOT_OWNER -> player.sendMessage("이 선택지는 해당 전초기지에 접근한 플레이어만 사용할 수 있습니다.");
                case OUT_OF_RANGE -> player.sendMessage("전초기지에서 너무 멀리 떨어져 선택할 수 없습니다.");
                case INVALID_CHOICE -> player.sendMessage("허용되지 않은 전초기지 선택입니다.");
                case SPAWN_FAILED -> player.sendMessage("전초기지 습격을 시작하지 못했습니다. 관리자에게 로그를 알려주세요.");
                default -> player.sendMessage("현재 선택할 수 있는 전초기지 습격이 없습니다.");
            }
            return;
        }
        if (!sender.hasPermission("hyunseorpg.admin")) {
            sender.sendMessage("관리자 권한이 필요합니다.");
            return;
        }
        if (explorationModule == null) {
            sender.sendMessage("탐험 모듈이 준비되지 않았습니다.");
            return;
        }
        switch (action) {
            case "status" -> {
                List<ExplorationStatusSnapshot> snapshots = explorationModule.statuses();
                sender.sendMessage("탐험 상태: " + snapshots.size() + "개 runtime/end-reason 기록");
                if (snapshots.isEmpty()) {
                    sender.sendMessage("활성 런타임 또는 최근 종료 기록이 없습니다.");
                    return;
                }
                snapshots.stream().limit(10).map(this::formatExplorationSnapshot).forEach(sender::sendMessage);
                if (snapshots.size() > 10) sender.sendMessage("...외 " + (snapshots.size() - 10) + "개");
            }
            case "inspect" -> {
                UUID structureId = resolveExplorationTarget(sender, args);
                if (structureId == null) return;
                explorationModule.runtimes().pyramidDiagnostics(structureId)
                        .ifPresentOrElse(values -> values.forEach((key, value) -> sender.sendMessage("pyramid." + key + "=" + value)),
                                () -> explorationModule.status(structureId).map(this::formatExplorationSnapshot).ifPresentOrElse(sender::sendMessage,
                                        () -> sender.sendMessage("해당 탐험 구조물 기록을 찾을 수 없습니다: " + structureId)));
            }
            case "reset" -> {
                UUID structureId = resolveExplorationTarget(sender, args);
                if (structureId == null) return;
                boolean reset = explorationModule.runtimes().resetPyramid(structureId);
                sender.sendMessage(reset ? "Desert Pyramid reset 완료: " + structureId
                        : "Desert Pyramid만 reset할 수 없거나 기록을 찾지 못했습니다: " + structureId);
            }
            case "complete" -> {
                UUID structureId = resolveExplorationTarget(sender, args);
                if (structureId == null) return;
                boolean completed = explorationModule.complete(structureId);
                sender.sendMessage(completed
                        ? "탐험 구조물을 완료 처리했습니다: " + structureId
                        : "완료 처리할 수 없습니다. ACTIVE runtime/objective 상태를 확인하세요: " + structureId);
            }
            default -> sender.sendMessage("사용법: /rpg exploration <status|inspect|reset|complete>");
        }
    }

    private UUID resolveExplorationTarget(CommandSender sender, String[] args) {
        if (args.length >= 3) return parseUuid(args[2], sender);
        if (!(sender instanceof Player player)) {
            sender.sendMessage("사용법: /rpg exploration " + (args.length >= 2 ? args[1] : "inspect") + " <structure-uuid>");
            return null;
        }
        return explorationModule.targetedStructure(player, 96.0D)
                .orElseGet(() -> {
                    sender.sendMessage("바라보는 방향에서 가까운 탐험 구조물을 찾지 못했습니다.");
                    return null;
                });
    }

    private UUID parseUuid(String raw, CommandSender sender) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("올바른 UUID가 아닙니다: " + raw);
            return null;
        }
    }

    private String formatExplorationSnapshot(ExplorationStatusSnapshot snapshot) {
        return "탐험 "
                + snapshot.structureId()
                + " type=" + snapshot.structureType()
                + " variant=" + snapshot.variantId()
                + " state=" + snapshot.persistentState()
                + " runtime=" + snapshot.runtimeActive()
                + " participants=" + snapshot.participantCount()
                + " objectives=" + snapshot.objectiveCount()
                + " lastEnd=" + (snapshot.lastEndReason() == null ? "NONE" : snapshot.lastEndReason());
    }

    private void handleFarming(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("delivery")) {
            if (args.length >= 3 && Set.of("reroll", "expire", "complete").contains(args[2].toLowerCase(Locale.ROOT))) {
                handleDeliveryAdmin(sender, args);
                return;
            }
            sender.sendMessage("배달 인벤토리 UI는 제거되었습니다. 월드 기반 전달 지점이 구현될 때까지 제출 기능을 사용할 수 없습니다.");
            return;
        }
        if (!sender.hasPermission("hyunseorpg.admin")) {
            sender.sendMessage("농사 관리자 권한이 필요합니다.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("사용법: /rpg farming <action> ...");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("debugharvest")) {
            handleFarmingDebug(sender, args);
            return;
        }
        if (action.equals("debugquality")) {
            handleQualityDebug(sender, args);
            return;
        }
        if (action.equals("debugdelivery")) {
            handleDeliveryDebug(sender);
            return;
        }
        if (action.equals("repairchunk")) {
            handleFarmingRepair(sender, args);
            return;
        }
        if (farmingProfiles == null || playerDataService == null) {
            sender.sendMessage("농사 서비스가 아직 준비되지 않았습니다.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("사용법: /rpg farming <status|unlock|lock|setstage|setharvests|addharvests|setpoints|addpoints|setfavor|addfavor|reset|give|giveprocessed|giveessence|givetoken|settokens|recalculate|reloadplayer> <player> ...");
            return;
        }
        OfflinePlayer target = resolveOfflinePlayer(args[2]);
        if (target == null) {
            sender.sendMessage("플레이어를 찾을 수 없습니다: " + args[2]);
            return;
        }
        UUID uuid = target.getUniqueId();
        switch (action) {
            case "status" -> farmingStatus(sender, target, uuid);
            case "unlock" -> {
                if (args.length < 4) { sender.sendMessage("작물 ID를 입력하세요."); return; }
                farmingMutation(sender, farmingProfiles.unlockCrop(uuid, args[3]), "작물 해금", args[3]);
                audit("unlock", uuid, args[3]);
            }
            case "lock" -> {
                if (args.length < 4) { sender.sendMessage("작물 ID를 입력하세요."); return; }
                farmingMutation(sender, farmingProfiles.lockCrop(uuid, args[3]), "작물 잠금", args[3]);
                audit("lock", uuid, args[3]);
            }
            case "setstage" -> {
                if (args.length < 4) { sender.sendMessage("단계를 입력하세요: 기본, 숙련, 능숙, 상급, 전문"); return; }
                FarmingStage stage = FarmingStage.fromInput(args[3]).orElse(null);
                if (stage == null) { sender.sendMessage("알 수 없는 농사 단계입니다: " + args[3]); return; }
                boolean success = farmingProfiles.setStage(uuid, stage);
                sender.sendMessage(success ? "농사 단계 변경 완료: " + stage.displayName()
                        : "농사 단계 변경에 실패했습니다.");
                audit("setstage=" + stage.name(), uuid, success ? "success" : "failed");
            }
            case "setharvests", "addharvests" -> handleHarvestMutation(sender, uuid, action, args);
            case "setpoints", "addpoints" -> handlePointMutation(sender, uuid, action, args);
            case "setfavor", "addfavor" -> handleFavorMutation(sender, uuid, action, args);
            case "reset" -> {
                boolean success = farmingProfiles.resetFarmingProfile(uuid);
                sender.sendMessage(success ? "농사 프로필 초기화 완료: " + uuid : "농사 프로필 초기화 실패: " + uuid);
                audit("reset", uuid, success ? "success" : "failed");
            }
            case "give" -> handleFarmingCropGive(sender, target, args);
            case "giveprocessed" -> handleProcessedGive(sender, target, args);
            case "giveessence" -> giveFarmingItem(sender, target, "abundance_essence", parseAmount(args, 3), "admin-farming-essence");
            case "givetoken" -> handleFarmingTokenGive(sender, target, args);
            case "settokens" -> handleFarmingTokenSet(sender, uuid, args);
            case "recalculate" -> sender.sendMessage(farmingProfiles.recalculateUnlocks(uuid)
                    ? "농사 해금을 현재 단계 기준으로 다시 계산했습니다."
                    : "농사 해금 재계산에 실패했습니다.");
            case "reloadplayer" -> {
                PlayerDataService.ReloadResult result = playerDataService.reloadPlayerIfClean(uuid);
                sender.sendMessage("플레이어 농사 데이터 재로드: " + reloadResultName(result));
            }
            default -> sender.sendMessage("알 수 없는 농사 명령입니다: " + action);
        }
    }

    private void handleHarvestMutation(CommandSender sender, UUID uuid, String action, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("수확량을 입력하세요.");
            return;
        }
        Long amount = parseNonNegativeLong(args[3]);
        if (amount == null) {
            sender.sendMessage("수확량은 0 이상의 정수여야 합니다.");
            return;
        }
        boolean success = action.equals("setharvests")
                ? farmingProfiles.setTotalValidHarvests(uuid, amount)
                : farmingProfiles.addTotalValidHarvests(uuid, amount);
        sender.sendMessage(success ? "농사 유효 수확량 변경 완료: " + amount
                : "농사 유효 수확량 변경에 실패했습니다.");
        audit(action + "=" + amount, uuid, success ? "success" : "failed");
    }

    private void handlePointMutation(CommandSender sender, UUID uuid, String action, String[] args) {
        if (args.length < 4) { sender.sendMessage("포인트를 입력하세요."); return; }
        Long amount = parseNonNegativeLong(args[3]);
        if (amount == null) { sender.sendMessage("포인트는 0 이상의 정수여야 합니다."); return; }
        boolean success = action.equals("setpoints")
                ? farmingProfiles.setAbundancePoints(uuid, amount)
                : farmingProfiles.addAbundancePoints(uuid, amount);
        sender.sendMessage(success ? "풍요 포인트 변경 완료: " + amount : "풍요 포인트 변경에 실패했습니다.");
        audit(action + "=" + amount, uuid, success ? "success" : "failed");
    }

    private void handleFavorMutation(CommandSender sender, UUID uuid, String action, String[] args) {
        if (args.length < 5) { sender.sendMessage("사용법: /rpg farming " + action + " <player> <farmer|alchemist> <amount>"); return; }
        DeliveryProvider provider = DeliveryProvider.fromInput(args[3]).orElse(null);
        Long amount = parseNonNegativeLong(args[4]);
        if (provider == null || provider == DeliveryProvider.ESTATE_RESERVED || amount == null) {
            sender.sendMessage("제공자 또는 호감도 수치가 올바르지 않습니다."); return;
        }
        boolean success = action.equals("setfavor")
                ? farmingProfiles.setFavor(uuid, provider, amount)
                : farmingProfiles.addFavor(uuid, provider, amount);
        sender.sendMessage(success ? "호감도 변경 완료: " + provider.displayName() + "=" + amount : "호감도 변경에 실패했습니다.");
        audit(action + "=" + provider.id() + ":" + amount, uuid, success ? "success" : "failed");
    }

    private void handleDeliveryAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hyunseorpg.admin") || deliveryService == null) {
            sender.sendMessage("농사 관리자 권한 또는 배달 서비스가 필요합니다."); return;
        }
        if (args.length < 5) { sender.sendMessage("사용법: /rpg farming delivery <reroll|expire|complete> <player> <provider>"); return; }
        OfflinePlayer target = resolveOfflinePlayer(args[3]);
        DeliveryProvider provider = DeliveryProvider.fromInput(args[4]).orElse(null);
        if (target == null || provider == null || provider == DeliveryProvider.ESTATE_RESERVED) {
            sender.sendMessage("플레이어 또는 제공자가 올바르지 않습니다."); return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        boolean success = switch (action) {
            case "reroll" -> deliveryService.adminReroll(target.getUniqueId(), provider);
            case "expire" -> deliveryService.adminExpire(target.getUniqueId(), provider);
            case "complete" -> deliveryService.adminComplete(target.getUniqueId(), provider);
            default -> false;
        };
        sender.sendMessage(success ? "배달 상태 변경 완료: " + action : "배달 상태 변경에 실패했습니다.");
        audit("delivery-" + action, target.getUniqueId(), success ? "success" : "failed");
    }

    private void handleDeliveryDebug(CommandSender sender) {
        if (deliveryService == null) { sender.sendMessage("배달 서비스가 준비되지 않았습니다."); return; }
        sender.sendMessage("배달 레지스트리 오류=" + deliveryService.lastRegistryErrors());
        sender.sendMessage("배달 제한 시간=" + deliveryService.timeLimitSeconds() + "초");
        sender.sendMessage("배달 재생성 대기시간=" + deliveryService.refreshSeconds() + "초");
    }

    private void handleQualityDebug(CommandSender sender, String[] args) {
        if (cropQualityService == null) { sender.sendMessage("품질 서비스가 준비되지 않았습니다."); return; }
        String crop = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "corn";
        int trials = args.length >= 4 ? Math.min(100000, Math.max(1, parseNonNegativeInt(args[3]) == null ? 1000 : parseNonNegativeInt(args[3]))): 1000;
        if (!farmingProfiles.isKnownCrop(crop)) { sender.sendMessage("알 수 없는 작물입니다: " + crop); return; }
        sender.sendMessage("품질 분포=" + cropQualityService.distribution(crop));
        sender.sendMessage("품질 시뮬레이션(" + trials + ")=" + cropQualityService.simulate(crop, trials, 0.0D, 0.0D));
    }

    private void handleFarmingCropGive(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("사용법: /rpg farming give <player> <crop> <quality> [amount]");
            return;
        }
        String crop = args[3].trim().toLowerCase(Locale.ROOT);
        CropQuality quality = CropQuality.fromId(args[4]).orElse(null);
        if (!farmingProfiles.isKnownCrop(crop) || quality == null) {
            sender.sendMessage("알 수 없는 작물 또는 품질입니다. 품질: 일반, 초급, 중급, 고급, 최고급");
            return;
        }
        String itemId = quality == CropQuality.NORMAL
                ? "crop_" + crop : "crop_" + crop + "_quality_" + quality.id();
        giveFarmingItem(sender, target, itemId, parseAmount(args, 5), "admin-farming-give");
    }

    private void handleProcessedGive(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("사용법: /rpg farming giveprocessed <player> <crop|processed-item-id> <quality> [amount]");
            return;
        }
        String rawItem = args[3].trim().toLowerCase(Locale.ROOT);
        CropQuality quality = CropQuality.fromId(args[4]).orElse(null);
        if (quality == null) { sender.sendMessage("알 수 없는 품질입니다."); return; }
        String itemId = rawItem.startsWith("processed_")
                ? rawItem : processedItemId(rawItem, quality);
        if (itemId == null || !itemRegistry.get(itemId).isPresent()) {
            sender.sendMessage("등록되지 않은 가공품입니다: " + rawItem);
            return;
        }
        giveFarmingItem(sender, target, itemId, parseAmount(args, 5), "admin-farming-processed");
    }

    private String processedItemId(String crop, CropQuality quality) {
        return switch (crop) {
            case "corn" -> "processed_corn_starch_" + quality.id();
            case "onion" -> "processed_onion_concentrate_" + quality.id();
            case "chili" -> "processed_chili_extract_" + quality.id();
            case "garlic" -> "processed_garlic_concentrate_" + quality.id();
            default -> null;
        };
    }

    private void handleFarmingTokenGive(CommandSender sender, OfflinePlayer target, String[] args) {
        if (farmingTokens == null || args.length < 4) {
            sender.sendMessage("사용법: /rpg farming givetoken <player> <token> [amount]");
            return;
        }
        FarmingStatTokenService.Definition definition = farmingTokens.definition(args[3]).orElse(null);
        if (definition == null) {
            sender.sendMessage("알 수 없는 농사 증표입니다: " + args[3]);
            return;
        }
        giveFarmingItem(sender, target, definition.itemId(), parseAmount(args, 4), "admin-farming-token");
    }

    private void handleFarmingTokenSet(CommandSender sender, UUID uuid, String[] args) {
        if (farmingTokens == null || args.length < 5) {
            sender.sendMessage("사용법: /rpg farming settokens <player> <token> <uses>");
            return;
        }
        FarmingStatTokenService.Definition definition = farmingTokens.definition(args[3]).orElse(null);
        Integer uses = parseNonNegativeInt(args[4]);
        if (definition == null || uses == null || uses > definition.maximumUses()) {
            sender.sendMessage("알 수 없는 증표이거나 사용 횟수가 설정된 상한을 초과했습니다.");
            return;
        }
        boolean success = farmingTokens.setUses(uuid, definition.id(), uses);
        sender.sendMessage(success ? "농사 증표 사용 횟수 변경 완료: " + definition.id() + "=" + uses
                : "농사 증표 사용 횟수 변경에 실패했습니다.");
        audit("settokens=" + definition.id() + ":" + uses, uuid, success ? "success" : "failed");
    }

    private void giveFarmingItem(CommandSender sender, OfflinePlayer target,
                                 String itemId, int amount, String cause) {
        if (amount < 1) {
            sender.sendMessage("수량은 1 이상 2304 이하로 입력하세요.");
            return;
        }
        ItemStack item = itemService.create(itemId, amount).orElse(null);
        if (item == null) {
            sender.sendMessage("등록되지 않은 농사 아이템입니다: " + itemId);
            return;
        }
        if (pendingRewards == null) {
            sender.sendMessage("미수령 보상 서비스가 아직 준비되지 않았습니다.");
            return;
        }
        Player online = target.getPlayer();
        if (online != null) {
            inventoryNormalizer.accept(online.getInventory());
            pendingRewards.deliverOrQueue(online, item, cause);
            online.sendMessage("농사 관리자 지급 아이템 도착: " + KoreanDisplay.itemId(itemId, itemService) + " x" + amount);
        } else {
            pendingRewards.queueItem(target.getUniqueId(), item, cause);
            sender.sendMessage("플레이어가 오프라인이라 미수령 보상으로 저장했습니다.");
        }
        sender.sendMessage("농사 아이템 준비 완료: " + KoreanDisplay.itemId(itemId, itemService) + " x" + amount);
        audit("give=" + itemId + "x" + amount, target.getUniqueId(), cause);
    }

    private void handleFarmingDebug(CommandSender sender, String[] args) {
        if (cropGrowthService == null) {
            sender.sendMessage("작물 성장 서비스가 아직 준비되지 않았습니다.");
            return;
        }
        if (args.length >= 3) {
            String mode = args[2].toLowerCase(Locale.ROOT);
            if (mode.equals("on") || mode.equals("off")) {
                boolean enabled = mode.equals("on");
                cropGrowthService.setDebugEvents(enabled);
                cropGrowthService.harvestService().setDebugHarvest(enabled);
            } else if (!mode.equals("status")) {
                sender.sendMessage("사용법: /rpg farming debugharvest <on|off|status>");
                return;
            }
        }
        sender.sendMessage("농사 디버그 이벤트=" + cropGrowthService.debugEventsEnabled()
                + " 수확=" + cropGrowthService.harvestService().debugHarvestEnabled()
                + " 로드된 청크=" + cropGrowthService.loadedChunkCount()
                + " 로드된 작물=" + cropGrowthService.loadedCropCount());
    }

    private void handleFarmingRepair(CommandSender sender, String[] args) {
        if (cropGrowthService == null || args.length < 5) {
            sender.sendMessage("사용법: /rpg farming repairchunk <world> <chunkX> <chunkZ>");
            return;
        }
        World world = Bukkit.getWorld(args[2]);
        Integer chunkX = parseInt(args[3]);
        Integer chunkZ = parseInt(args[4]);
        if (world == null || chunkX == null || chunkZ == null) {
            sender.sendMessage("월드 또는 청크 좌표가 올바르지 않습니다.");
            return;
        }
        CropGrowthService.RepairResult result = cropGrowthService.repairChunk(world, chunkX, chunkZ);
        sender.sendMessage("청크 복구 " + (result.success() ? "성공" : "실패")
                + ": " + result.message() + " 저장=" + result.storedEntries()
                + " 활성=" + result.activeEntries() + " 제거=" + result.removedEntries());
        audit("repairchunk=" + world.getName() + "/" + chunkX + "/" + chunkZ,
                null, result.success() ? "success" : result.message());
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

    private void audit(String action, UUID uuid, String details) {
        farmingAuditLogger.accept("action=" + action + " target=" + (uuid == null ? "none" : uuid)
                + " details=" + details);
    }

    private void farmingStatus(CommandSender sender, OfflinePlayer target, UUID uuid) {
        FarmingProfile profile = farmingProfiles.getFarmingProfile(uuid);
        sender.sendMessage("[농사 상태] " + (target.getName() == null ? uuid : target.getName()));
        sender.sendMessage("데이터 버전=" + profile.dataVersion() + " 단계=" + profile.stage().displayName());
        sender.sendMessage("총 유효 수확량=" + profile.totalValidHarvests());
        sender.sendMessage("작물별 수확량=" + profile.cropHarvests());
        sender.sendMessage("해금 작물=" + profile.unlockedCrops().stream().sorted().map(KoreanDisplay::crop).toList());
        sender.sendMessage("풍요 포인트=" + profile.abundancePoints());
        sender.sendMessage("증표 사용 횟수=" + profile.statTokenUses());
        if (playerDataService != null) {
            sender.sendMessage("데이터 로드=" + playerDataService.isLoaded(uuid)
                    + " 저장 대기=" + playerDataService.isDirty(uuid));
        }
        if (farmingPromotions != null) {
            farmingPromotions.nextRule(profile.stage()).ifPresentOrElse(rule -> {
                sender.sendMessage("다음 단계=" + rule.nextStage().displayName() + " 최소 재질=" + tierName(rule.minimumTier())
                        + " 필요 강화=+" + rule.requiredEnhancement()
                        + " 필요 유효 수확량=" + rule.requiredValidHarvests());
                sender.sendMessage("필요 작물=" + KoreanDisplay.itemId(rule.requiredCropItemId(), itemService) + " x" + rule.requiredCropAmount()

                        + " 다음 해금=" + KoreanDisplay.crop(rule.unlockCrop()));
            }, () -> sender.sendMessage("다음 단계=없음"));
        }
    }

    private String reloadResultName(PlayerDataService.ReloadResult result) {
        return switch (result) {
            case RELOADED -> "재로드 완료";
            case DIRTY -> "저장 대기 데이터가 있어 보류";
            case FAILED -> "실패";
            case INVALID -> "잘못된 UUID";
        };
    }

    private String tierName(String tier) {
        return switch (tier == null ? "" : tier.trim().toLowerCase(Locale.ROOT)) {
            case "wooden" -> "나무";
            case "stone" -> "돌";
            case "gold", "golden" -> "금";
            case "iron" -> "철";
            case "diamond" -> "다이아몬드";
            case "netherite" -> "네더라이트";
            default -> tier == null ? "" : tier;
        };
    }

    private void farmingMutation(CommandSender sender, boolean success, String action, String crop) {
        if (!farmingProfiles.isKnownCrop(crop)) {
            sender.sendMessage("알 수 없는 작물 ID입니다: " + crop);
        } else {
            sender.sendMessage(success ? action + " 완료: " + crop : action + " 저장 실패: " + crop);
        }
    }

    private OfflinePlayer resolveOfflinePlayer(String input) {
        try { return Bukkit.getOfflinePlayer(UUID.fromString(input)); }
        catch (IllegalArgumentException ignored) { }
        Player online = Bukkit.getPlayerExact(input);
        return online != null ? online : Bukkit.getOfflinePlayer(input);
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
            return List.of("all", "reload", "configs", "items", "recipes", "equipment", "mobs", "players", "progression", "quests", "farming", "alchemy", "effects");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return migrationTargetCompletion(args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("exploration")) {
            return explorationActionCompletion(args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("farming")) {
            return farmingActionCompletion(args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("farming") && args[1].equalsIgnoreCase("delivery")) {
            return List.of("farmer", "alchemist", "reroll", "expire", "complete");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("farming") && args[1].equalsIgnoreCase("delivery")
                && Set.of("reroll", "expire", "complete").contains(args[2].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("farming") && args[1].equalsIgnoreCase("delivery")
                && Set.of("reroll", "expire", "complete").contains(args[2].toLowerCase(Locale.ROOT))) {
            return List.of("farmer", "alchemist");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("farming")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("farming")
                && (args[1].equalsIgnoreCase("unlock") || args[1].equalsIgnoreCase("lock"))) {
            return List.of("corn", "onion", "chili", "garlic");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("farming")
                && args[1].equalsIgnoreCase("debugharvest")) {
            return List.of("on", "off", "status");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("farming")
                && args[1].equalsIgnoreCase("repairchunk")) {
            return Bukkit.getWorlds().stream().map(World::getName).toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("farming")
                && (args[1].equalsIgnoreCase("give"))) {
            return List.of("corn", "onion", "chili", "garlic");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("farming")
                && args[1].equalsIgnoreCase("giveprocessed")) {
            return List.of("corn", "onion", "chili", "garlic");
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("farming")
                && args[1].equalsIgnoreCase("give")) {
            return List.of("normal", "basic", "proficient", "advanced", "supreme");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("farming")
                && (args[1].equalsIgnoreCase("givetoken") || args[1].equalsIgnoreCase("settokens"))) {
            return farmingTokens == null ? List.of() : farmingTokens.definitionIds();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("farming") && args[1].equalsIgnoreCase("setstage")) {
            return List.of("BASIC", "SKILLED", "PROFICIENT", "ADVANCED", "EXPERT");
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
        return filterCompletion(List.of("give", "pending", "reload", "doctor", "migrate", "exploration", "farming", "effect", "alchemy", "debug"), prefix);
    }

    static List<String> explorationActionCompletion(String prefix) {
        return filterCompletion(List.of("status", "inspect", "reset", "complete", "choose"), prefix);
    }

    static List<String> farmingActionCompletion(String prefix) {
        return filterCompletion(List.of("status", "unlock", "lock", "setstage", "setharvests",
                "addharvests", "setpoints", "addpoints", "setfavor", "addfavor", "reset", "give",
                "giveprocessed", "giveessence", "givetoken", "settokens", "debugharvest", "debugquality", "debugdelivery",
                "repairchunk", "recalculate", "reloadplayer", "delivery"), prefix);
    }

    static List<String> pendingCompletion(String prefix) {
        return filterCompletion(List.of("claim"), prefix);
    }

    static List<String> migrationTargetCompletion(String prefix) {
        return filterCompletion(List.of("configs", "items", "mobs", "players", "farming",
                "alchemy", "exploration", "legacy", "cleanup", "all"), prefix);
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
