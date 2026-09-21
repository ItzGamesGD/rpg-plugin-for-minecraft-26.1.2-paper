package com.hyunseo.hyunseorpg;

import com.hyunseo.hyunseorpg.command.RPGGiveCommand;
import com.hyunseo.hyunseorpg.command.RPGTestCommand;
import com.hyunseo.hyunseorpg.prototype.thousandeyes.ThousandEyesController;
import com.hyunseo.hyunseorpg.rpgtest.gateway.GatewayPrototypeService;
import com.hyunseo.hyunseorpg.command.RPGLevelAdminCommand;
import com.hyunseo.hyunseorpg.command.RPGMobCommand;
import com.hyunseo.hyunseorpg.command.SpecialEquipmentCommand;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigDoctor;
import com.hyunseo.hyunseorpg.core.config.ConfigMigrationService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.core.config.RPGReloadService;
import com.hyunseo.hyunseorpg.enhancement.EnhancementClassificationService;
import com.hyunseo.hyunseorpg.enhancement.VanillaAnvilEnhancementListener;
import com.hyunseo.hyunseorpg.enhancement.VanillaAnvilPolicyListener;
import com.hyunseo.hyunseorpg.enhancement.EnhancementRegistry;
import com.hyunseo.hyunseorpg.enhancement.EquipmentGrowthConfigValidator;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.equipment.ToolDurabilityService;
import com.hyunseo.hyunseorpg.enchant.EnchantRegistry;
import com.hyunseo.hyunseorpg.enchant.EnchantService;
import com.hyunseo.hyunseorpg.enchant.EnchantRuntimeStateService;
import com.hyunseo.hyunseorpg.enchant.EquipmentEnchantContentService;
import com.hyunseo.hyunseorpg.equipment.trigger.EquipmentEffectTriggerEngine;
import com.hyunseo.hyunseorpg.equipment.trigger.EquipmentEffectTriggerListener;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.equipment.EquipmentActualEffectListener;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.equipment.EquipmentMetadataService;
import com.hyunseo.hyunseorpg.equipment.EquipmentRegistry;
import com.hyunseo.hyunseorpg.exp.ExpService;
import com.hyunseo.hyunseorpg.exp.ExpTable;
import com.hyunseo.hyunseorpg.exp.LevelService;
import com.hyunseo.hyunseorpg.item.RPGItemRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.PendingRewardService;
import com.hyunseo.hyunseorpg.item.VanillaStackingService;
import com.hyunseo.hyunseorpg.mob.MobAbilityRegistry;
import com.hyunseo.hyunseorpg.mob.MobDeathListener;
import com.hyunseo.hyunseorpg.mob.VanillaMobFragmentDropListener;
import com.hyunseo.hyunseorpg.mob.ElementalFragmentPolicy;
import com.hyunseo.hyunseorpg.item.CustomItemVanillaActionBlockListener;
import com.hyunseo.hyunseorpg.mob.MobLevelScalingService;
import com.hyunseo.hyunseorpg.mob.MobRewardService;
import com.hyunseo.hyunseorpg.mob.MobService;
import com.hyunseo.hyunseorpg.mob.MobRegistry;
import com.hyunseo.hyunseorpg.mob.MobSpawnListener;
import com.hyunseo.hyunseorpg.mob.MobSpawnZoneRegistry;
import com.hyunseo.hyunseorpg.mob.MonsterSpawnRegistry;
import com.hyunseo.hyunseorpg.mob.MonsterSpawnService;
import com.hyunseo.hyunseorpg.mob.MonsterBehaviorService;
import com.hyunseo.hyunseorpg.mob.MobTagService;
import com.hyunseo.hyunseorpg.mob.variant.ZombieVariantService;
import com.hyunseo.hyunseorpg.mob.drop.MobDropRegistry;
import com.hyunseo.hyunseorpg.mob.drop.MobDropService;
import com.hyunseo.hyunseorpg.mythic.MythicMobIntegrationService;
import com.hyunseo.hyunseorpg.mythic.MythicMobRegistry;
import com.hyunseo.hyunseorpg.mythic.MythicCustomMobService;
import com.hyunseo.hyunseorpg.player.PlayerDataCache;
import com.hyunseo.hyunseorpg.player.PlayerDataListener;
import com.hyunseo.hyunseorpg.player.PlayerDataRepository;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.YamlPlayerDataRepository;
import com.hyunseo.hyunseorpg.progression.RequirementChecker;
import com.hyunseo.hyunseorpg.progression.NaturalDiscoveryService;
import com.hyunseo.hyunseorpg.progression.DimensionVisitTracker;
import com.hyunseo.hyunseorpg.activity.ActivityBlockRepository;
import com.hyunseo.hyunseorpg.activity.ActivityBlockRewardValidator;
import com.hyunseo.hyunseorpg.alchemy.EffectService;
import com.hyunseo.hyunseorpg.alchemy.BerserkEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.BleedEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.CorrosionEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.FrostbiteEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.NecrosisEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.ProductionEffectListener;
import com.hyunseo.hyunseorpg.alchemy.ShockEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.VampirismEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.VulnerabilityEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.catalyst.BoundedSpecialCatalystExecutionService;
import com.hyunseo.hyunseorpg.alchemy.catalyst.YamlCatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.catalyst.YamlSpecialCatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionFactory;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionPdcContract;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionUseListener;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionUseService;
import com.hyunseo.hyunseorpg.alchemy.potion.YamlPotionRegistry;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeRegistry;
import com.hyunseo.hyunseorpg.alchemy.recipe.YamlAlchemyRecipeRegistry;
import com.hyunseo.hyunseorpg.activity.MiningActivityListener;
import com.hyunseo.hyunseorpg.activity.MiningActivityService;
import com.hyunseo.hyunseorpg.crafting.SoulboundItemService;
import com.hyunseo.hyunseorpg.skill.CooldownCleanupListener;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.skill.SkillInputListener;
import com.hyunseo.hyunseorpg.skill.bowmaster.BowmasterSkillService;
import com.hyunseo.hyunseorpg.skill.swordmaster.SwordmasterBladeService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentEffectListener;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentProgressListener;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentRegistry;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import com.hyunseo.hyunseorpg.special.DedicatedWeaponIds;
import com.hyunseo.hyunseorpg.special.water.WaterTridentListener;
import com.hyunseo.hyunseorpg.special.flame.FlameAxeListener;
import com.hyunseo.hyunseorpg.special.thanatos.ThanatosMaceListener;
import com.hyunseo.hyunseorpg.special.thunder.ThunderAxeListener;
import com.hyunseo.hyunseorpg.special.solaris.SolarisListener;
import com.hyunseo.hyunseorpg.special.moonlit.MoonlitAfterglowListener;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponItemService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import com.hyunseo.hyunseorpg.command.paper.PaperCommandBridge;
import com.hyunseo.hyunseorpg.command.paper.PaperCommandCatalog;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class HyunseoRPGPlugin extends JavaPlugin {
    private ConfigService configService;
    private WeaponService weaponService;
    private WeaponItemService weaponItemService;
    private RPGItemRegistry itemRegistry;
    private RPGItemService itemService;
    private InventoryDeliveryService inventoryDeliveryService;
    private PendingRewardService pendingRewardService;
    private NaturalDiscoveryService naturalDiscoveryService;
    private RPGReloadService reloadService;
    private PlayerDataRepository playerDataRepository;
    private PlayerDataCache playerDataCache;
    private PlayerDataService playerDataService;
    private RequirementChecker requirementChecker;
    private DimensionVisitTracker dimensionVisitTracker;
    private CombatService combatService;
    private CooldownService cooldownService;
    private SwordmasterBladeService swordmasterBladeService;
    private BowmasterSkillService bowmasterSkillService;
    private ExpTable expTable;
    private LevelService levelService;
    private ExpService expService;
    private MobTagService mobTagService;
    private MobAbilityRegistry mobAbilityRegistry;
    private MobRegistry mobRegistry;
    private MobSpawnZoneRegistry mobSpawnZoneRegistry;
    private MobLevelScalingService mobLevelScalingService;
    private MobService mobService;
    private MobDropRegistry mobDropRegistry;
    private MobDropService mobDropService;
    private ElementalFragmentPolicy elementalFragmentPolicy;
    private MonsterSpawnRegistry monsterSpawnRegistry;
    private MonsterSpawnService monsterSpawnService;
    private MonsterBehaviorService monsterBehaviorService;
    private MobRewardService mobRewardService;
    private ZombieVariantService zombieVariantService;
    private MythicMobRegistry mythicMobRegistry;
    private MythicMobIntegrationService mythicMobIntegrationService;
    private MythicCustomMobService mythicCustomMobService;
    private VanillaStackingService vanillaStackingService;
    private ActivityBlockRepository activityBlockRepository;
    private ActivityBlockRewardValidator activityBlockRewardValidator;
    private SoulboundItemService soulboundItemService;
    private MiningActivityService miningActivityService;
    private ToolDurabilityService toolDurabilityService;
    private EnhancementRegistry enhancementRegistry;
    private EquipmentEnhancementService equipmentEnhancementService;
    private EnhancementClassificationService enhancementClassificationService;
    private EquipmentTierService equipmentTierService;
    private EquipmentMetadataService equipmentMetadataService;
    private EquipmentRegistry equipmentRegistry;
    private EquipmentGrowthPolicy equipmentGrowthPolicy;
    private SpecialEquipmentRegistry specialEquipmentRegistry;
    private SpecialEquipmentService specialEquipmentService;
    private EnchantRegistry enchantRegistry;
    private EnchantService enchantService;
    private EquipmentInstanceService equipmentInstanceService;
    private EnchantRuntimeStateService enchantRuntimeStateService;
    private EquipmentEffectTriggerEngine equipmentEffectTriggerEngine;
    private EquipmentEnchantContentService equipmentEnchantContentService;
    private SpecialEquipmentEffectListener specialEquipmentEffectListener;
    private WaterTridentListener waterTridentListener;
    private FlameAxeListener flameAxeListener;
    private ThanatosMaceListener thanatosMaceListener;
    private ThunderAxeListener thunderAxeListener;
    private SolarisListener solarisListener;
    private MoonlitAfterglowListener moonlitAfterglowListener;
    private ConfigMigrationService configMigrationService;
    private EffectService effectService;
    private com.hyunseo.hyunseorpg.alchemy.EffectMovementLockService effectMovementLockService;
    private ProductionEffectListener productionEffectListener;
    private com.hyunseo.hyunseorpg.alchemy.PaperAlchemyCombatAdapter alchemyCombatAdapter;
    private PotionRegistry potionRegistry;
    private PaperPotionPdcContract potionPdc;
    private PotionFactory potionFactory;
    private PaperPotionUseListener potionUseListener;
    private AlchemyRecipeRegistry alchemyRecipeRegistry;
    private YamlCatalystRegistry catalystRegistry;
    private YamlSpecialCatalystRegistry specialCatalystRegistry;
    private BoundedSpecialCatalystExecutionService specialCatalystExecutionService;
    private com.hyunseo.hyunseorpg.alchemy.AlchemyAuditLog alchemyAuditLog;
    private GatewayPrototypeService gatewayPrototypeService;
    private ThousandEyesController thousandEyesController;

    @Override
    public void onEnable() {
        this.configMigrationService = new ConfigMigrationService(this);
        runStartupConfigMigration();
        this.configService = new ConfigService(this);
        this.configService.loadDefaults();
        new EquipmentGrowthConfigValidator(configService).validate();
        this.effectService = new EffectService(this, configService);
        this.alchemyCombatAdapter = new com.hyunseo.hyunseorpg.alchemy.PaperAlchemyCombatAdapter(effectService);
        registerProductionEffectHandlers();
        if (!effectService.load()) {
            getLogger().warning("Unable to load alchemy effect definitions: "
                    + String.join("; ", effectService.registry().lastErrors()));
        }

        this.playerDataRepository = new YamlPlayerDataRepository(this);
        this.playerDataCache = new PlayerDataCache();
        this.playerDataService = new PlayerDataService(this, playerDataRepository, playerDataCache);
        this.itemRegistry = new RPGItemRegistry(configService);
        this.itemRegistry.load();
        this.itemService = new RPGItemService(this, itemRegistry);
        this.potionRegistry = new YamlPotionRegistry(configService, effectService);
        this.potionRegistry.reload();
        this.potionPdc = new PaperPotionPdcContract(this);
        this.potionFactory = new PotionFactory(potionRegistry, itemService, potionPdc, 1);
        this.catalystRegistry = new YamlCatalystRegistry(configService);
        this.specialCatalystRegistry = new YamlSpecialCatalystRegistry(configService);
        this.catalystRegistry.reload();
        this.specialCatalystRegistry.reload();
        this.alchemyRecipeRegistry = new YamlAlchemyRecipeRegistry(
                configService, potionRegistry, itemService, catalystRegistry, specialCatalystRegistry);
        this.alchemyRecipeRegistry.reload();
        this.specialCatalystExecutionService = new BoundedSpecialCatalystExecutionService(
                this, specialCatalystRegistry, potionRegistry, effectService, 128);
        this.potionUseListener = new PaperPotionUseListener(this, potionPdc,
                new PaperPotionUseService(potionRegistry, potionPdc, effectService, itemService, 1,
                        catalystRegistry, specialCatalystExecutionService), specialCatalystExecutionService);
        this.alchemyAuditLog = new com.hyunseo.hyunseorpg.alchemy.AlchemyAuditLog(this);
        this.vanillaStackingService = new VanillaStackingService(configService, itemService);
        this.itemService.setItemNormalizer(vanillaStackingService::normalize);
        this.inventoryDeliveryService = new InventoryDeliveryService();
        this.inventoryDeliveryService.setItemNormalizer(vanillaStackingService::normalize);
        this.naturalDiscoveryService = new NaturalDiscoveryService(this, configService, playerDataService, itemService);
        this.inventoryDeliveryService.setDeliveryObserver(naturalDiscoveryService::discoverDeliveredItem);
        this.weaponService = new WeaponService(this);
        this.weaponItemService = new WeaponItemService(configService, weaponService, itemService);
        this.soulboundItemService = new SoulboundItemService(this, itemService);
        try {
            this.activityBlockRepository = new ActivityBlockRepository(this);
            this.activityBlockRewardValidator = new ActivityBlockRewardValidator(
                    activityBlockRepository, configService);
        } catch (SQLException exception) {
            throw new IllegalStateException("광부 설치 광석 저장소를 초기화할 수 없습니다.", exception);
        }
        this.pendingRewardService = new PendingRewardService(this);
        this.inventoryDeliveryService.setPendingRewardService(pendingRewardService);
        this.miningActivityService = new MiningActivityService(configService, activityBlockRepository,
                activityBlockRewardValidator);
        this.enhancementRegistry = new EnhancementRegistry(configService);
        this.equipmentTierService = new EquipmentTierService(configService, itemService);
        this.equipmentMetadataService = new EquipmentMetadataService(this, itemService, equipmentTierService);
        this.itemService.setItemNormalizer(item -> {
            vanillaStackingService.normalize(item);
            equipmentMetadataService.ensureDataVersion(item);
        });
        this.weaponItemService.setEquipmentMetadataService(equipmentMetadataService);
        this.equipmentEnhancementService = new EquipmentEnhancementService(this, itemService, enhancementRegistry);
        this.specialEquipmentRegistry = new SpecialEquipmentRegistry(configService);
        this.specialEquipmentRegistry.load();
        this.equipmentGrowthPolicy = new EquipmentGrowthPolicy(
                configService, itemService, equipmentTierService, specialEquipmentRegistry);
        this.enhancementClassificationService = new EnhancementClassificationService(
                configService, itemService, equipmentTierService, equipmentGrowthPolicy, specialEquipmentRegistry);
        this.equipmentEnhancementService.setClassificationService(enhancementClassificationService);
        this.equipmentRegistry = new EquipmentRegistry(configService, itemRegistry, itemService,
                enhancementRegistry, equipmentTierService, specialEquipmentRegistry);
        this.equipmentRegistry.load();
        this.specialEquipmentService = new SpecialEquipmentService(this, configService, specialEquipmentRegistry,
                itemService, playerDataService, inventoryDeliveryService, equipmentEnhancementService);
        this.requirementChecker = new RequirementChecker(playerDataService);
        this.dimensionVisitTracker = new DimensionVisitTracker(playerDataService);
        this.combatService = new CombatService();
        this.combatService.setEquipmentEnhancementService(equipmentEnhancementService);
        this.combatService.setEquipmentTierService(equipmentTierService);
        this.combatService.setConfigService(configService);
        this.swordmasterBladeService = new SwordmasterBladeService(this, configService, combatService);
        this.bowmasterSkillService = new BowmasterSkillService(this, configService, weaponService, combatService);
        this.cooldownService = new CooldownService(configService.getSpecialEquipmentBoolean(
                "special-equipment.testing.disable-cooldowns", false));
        this.enchantRegistry = new EnchantRegistry(configService);
        this.enchantRegistry.load();
        this.equipmentInstanceService = new EquipmentInstanceService(this);
        this.enchantRuntimeStateService = new EnchantRuntimeStateService(equipmentInstanceService);
        this.enchantService = new EnchantService(
                this, configService, enchantRegistry, itemService, weaponService, equipmentTierService,
                equipmentInstanceService);
        this.toolDurabilityService = new ToolDurabilityService(equipmentTierService);
        this.itemService.setItemNormalizer(item -> {
            vanillaStackingService.normalize(item);
            equipmentMetadataService.ensureDataVersion(item);
            enchantService.refreshBookLore(item);
        });
        this.inventoryDeliveryService.setItemNormalizer(item -> {
            vanillaStackingService.normalize(item);
            equipmentMetadataService.ensureDataVersion(item);
            enchantService.refreshBookLore(item);
        });
        this.pendingRewardService.setItemNormalizer(vanillaStackingService::normalize);
        this.equipmentEffectTriggerEngine = new EquipmentEffectTriggerEngine(this, enchantRegistry, enchantService,
                cooldownService, equipmentInstanceService, enchantRuntimeStateService);
        this.equipmentEnchantContentService = new EquipmentEnchantContentService(this, configService, combatService,
                enchantService, equipmentInstanceService, enchantRuntimeStateService, equipmentTierService, itemService,
                cooldownService, swordmasterBladeService, bowmasterSkillService,
                activityBlockRewardValidator);
        this.equipmentEffectTriggerEngine.registerHandler("content", equipmentEnchantContentService);
        validateEnchantIntegrity();
        this.expTable = new ExpTable(configService);
        this.levelService = new LevelService(playerDataService, expTable);
        this.expService = new ExpService(levelService);
        this.mobTagService = new MobTagService(this);
        this.zombieVariantService = new ZombieVariantService(this, configService, mobTagService);
        this.mobAbilityRegistry = new MobAbilityRegistry(configService);
        this.mobAbilityRegistry.load();
        this.mobRegistry = new MobRegistry(configService);
        this.mobRegistry.load();
        this.mobSpawnZoneRegistry = new MobSpawnZoneRegistry(configService);
        this.mobSpawnZoneRegistry.load();
        this.monsterSpawnRegistry = new MonsterSpawnRegistry(configService);
        this.monsterSpawnRegistry.load();
        this.mobLevelScalingService = new MobLevelScalingService(configService, playerDataService);
        this.mobService = new MobService(configService, mobTagService, mobRegistry, mobAbilityRegistry, mobLevelScalingService);
        this.elementalFragmentPolicy = new ElementalFragmentPolicy(configService, mobService, playerDataService);
        this.monsterSpawnService = new MonsterSpawnService(
                this, configService, monsterSpawnRegistry, mobService, mobLevelScalingService);
        this.monsterBehaviorService = new MonsterBehaviorService(this, configService, mobService);
        this.thousandEyesController = new ThousandEyesController(this);
        this.mythicMobRegistry = new MythicMobRegistry(configService);
        this.mythicMobRegistry.load();
        this.mythicMobIntegrationService = new MythicMobIntegrationService(
                this, configService, mythicMobRegistry, itemService, inventoryDeliveryService);
        this.mythicCustomMobService = new MythicCustomMobService(
                this, configService, mythicMobRegistry, mythicMobIntegrationService,
                mobService, mobRegistry);
        this.mobDropRegistry = new MobDropRegistry(this, configService);
        this.mobDropRegistry.load();
        this.mobDropService = new MobDropService(this, mobService, mobDropRegistry, itemService, inventoryDeliveryService,
                elementalFragmentPolicy);
        this.mobRewardService = new MobRewardService(
                mobService, expService, mobDropService,
                mythicMobIntegrationService);

        this.gatewayPrototypeService = new GatewayPrototypeService(this, configService);

        configureReloadService();

        registerCommandsSafe();
        registerListeners();
        loadCurrentlyOnlinePlayers();
        playerDataService.startAutosave();
        swordmasterBladeService.start();
        zombieVariantService.start();
        mythicCustomMobService.start();
        monsterSpawnService.start();
        monsterBehaviorService.start();
        effectService.start();

        logRuntimeDiagnostics();
        getLogger().info("HyunseoRPG enabled for Paper 26.1.2.");
    }

    @Override
    public void onDisable() {
        if (specialCatalystExecutionService != null) specialCatalystExecutionService.cancelAll(
                com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystExecution.CancelReason.SERVER_RESTART);
        if (effectMovementLockService != null) effectMovementLockService.clearAll();
        if (specialEquipmentEffectListener != null) {
            specialEquipmentEffectListener.clearAllStates();
        }
        if (waterTridentListener != null) waterTridentListener.shutdown();
        if (flameAxeListener != null) flameAxeListener.shutdown();
        if (thanatosMaceListener != null) thanatosMaceListener.shutdown();
        if (thunderAxeListener != null) thunderAxeListener.shutdown();
        if (solarisListener != null) solarisListener.shutdown();
        if (moonlitAfterglowListener != null) moonlitAfterglowListener.shutdown();
        if (gatewayPrototypeService != null) gatewayPrototypeService.shutdown();
        if (thousandEyesController != null) thousandEyesController.remove();
        if (equipmentEnchantContentService != null) {
            equipmentEnchantContentService.shutdown();
        }
        if (effectService != null) effectService.shutdown();
        if (cooldownService != null) {
            cooldownService.clearAll();
        }
        if (swordmasterBladeService != null) {
            swordmasterBladeService.clearAll();
        }
        if (bowmasterSkillService != null) {
            bowmasterSkillService.clearAll();
        }
        if (zombieVariantService != null) {
            zombieVariantService.cancelAll();
        }
        if (pendingRewardService != null) pendingRewardService.save();
        if (mythicCustomMobService != null) {
            mythicCustomMobService.stop();
        }
        if (monsterSpawnService != null) {
            monsterSpawnService.stop();
        }
        if (monsterBehaviorService != null) {
            monsterBehaviorService.stop();
        }
        if (playerDataService != null) {
            playerDataService.stopAutosave();
            playerDataService.saveAllAndClear();
        }
        if (activityBlockRepository != null) {
            activityBlockRepository.close();
        }
        getLogger().info("HyunseoRPG disabled.");
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public PlayerDataRepository getPlayerDataRepository() {
        return playerDataRepository;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public EffectService getEffectService() {
        return effectService;
    }

    public RPGItemService getItemService() {
        return itemService;
    }

    public PotionRegistry getPotionRegistry() { return potionRegistry; }
    public AlchemyRecipeRegistry getAlchemyRecipeRegistry() { return alchemyRecipeRegistry; }
    public YamlCatalystRegistry getCatalystRegistry() { return catalystRegistry; }
    public YamlSpecialCatalystRegistry getSpecialCatalystRegistry() { return specialCatalystRegistry; }
    public BoundedSpecialCatalystExecutionService getSpecialCatalystExecutionService() { return specialCatalystExecutionService; }

    public LevelService getLevelService() {
        return levelService;
    }

    public ExpService getExpService() {
        return expService;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public MobService getMobService() {
        return mobService;
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommandsSafe() {
        RPGGiveCommand give = new RPGGiveCommand(
                itemRegistry, itemService, soulboundItemService, reloadService, weaponItemService,
                zombieVariantService);
        give.setMaintenanceServices(new ConfigDoctor(this), new ConfigMigrationService(this));
        give.setPendingRewardService(pendingRewardService);
        give.setEffectService(effectService);
        give.setAlchemyServices(potionRegistry, potionPdc, specialCatalystExecutionService, alchemyAuditLog);
        give.setPotionFactory(potionFactory);
        give.setSpecialEquipmentService(specialEquipmentService);
        give.setInventoryNormalizer(vanillaStackingService::normalizeAndMergeInventory);

        RPGTestCommand test = new RPGTestCommand(itemRegistry, itemService, soulboundItemService,
                weaponItemService, equipmentEnhancementService,
                reloadService, equipmentMetadataService, equipmentRegistry, gatewayPrototypeService, thousandEyesController);
        RPGLevelAdminCommand level = new RPGLevelAdminCommand(playerDataService, expService, levelService);
        RPGMobCommand mob = new RPGMobCommand(configService, mobService, mobLevelScalingService,
                mythicCustomMobService, monsterBehaviorService);
        SpecialEquipmentCommand special = new SpecialEquipmentCommand(specialEquipmentService);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            registerPaperCommand(commands, "rpg", give, give);
            registerPaperCommand(commands, "rpgtest", test, test);
            registerPaperCommand(commands, "rpglevel", level, level);
            registerPaperCommand(commands, "rpgmob", mob, mob);
            registerPaperCommand(commands, "specialequipment", special, special);
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerPaperCommand(Commands commands, String name, CommandExecutor executor, TabCompleter completer) {
        var spec = PaperCommandCatalog.BY_NAME.get(name);
        if (spec == null) throw new IllegalStateException("Missing Paper command metadata: " + name);
        commands.register(name, spec.description(), spec.aliases(), new PaperCommandBridge(name, executor, completer));
    }

    private void registerProductionEffectHandlers() {
        this.effectMovementLockService = new com.hyunseo.hyunseorpg.alchemy.EffectMovementLockService(this);
        effectService.handlers().register("vampirism", new VampirismEffectHandler(configService));
        effectService.handlers().register("berserk", new BerserkEffectHandler(configService));
        effectService.handlers().register("corrosion", new CorrosionEffectHandler(configService));
        effectService.handlers().register("frostbite", new FrostbiteEffectHandler(configService));
        effectService.handlers().register("shock", new ShockEffectHandler(configService, effectMovementLockService));
        effectService.handlers().register("bleed", new BleedEffectHandler(configService));
        effectService.handlers().register("vulnerability", new VulnerabilityEffectHandler(configService));
        effectService.handlers().register("necrosis", new NecrosisEffectHandler(configService));
        this.productionEffectListener = new ProductionEffectListener(effectService);
    }

    private void configureReloadService() {
        this.reloadService = new RPGReloadService(configService);
        reloadService.registerDetailed("all", this::reloadAllRegistriesDetailed);
        reloadService.register("enhancement", () -> {
            configService.reloadEquipmentGrowthConfig();
            boolean valid = new EquipmentGrowthConfigValidator(configService).validate();
            if (valid) equipmentRegistry.load();
            return valid;
        });
        reloadService.register("equipment", () -> {
            configService.reloadEquipmentGrowthConfig();
            boolean valid = new EquipmentGrowthConfigValidator(configService).validate();
            if (valid) equipmentRegistry.load();
            return valid;
        });
        reloadService.register("special-equipment", () -> {
            configService.reloadSpecialEquipmentConfig();
            specialEquipmentRegistry.load();
            equipmentRegistry.load();
            return !specialEquipmentRegistry.getAll().isEmpty();
        });
        reloadService.registerDetailed("items", () -> {
            configService.reloadItemsConfig();
            itemRegistry.load();
            equipmentRegistry.load();
            return RPGReloadService.ReloadOutcome.pass("items");
        });
        reloadService.register("enchant", () -> {
            configService.reloadEnchantsConfig();
            enchantRegistry.load();
            refreshOnlineEnchantLore();
            return true;
        });
        reloadService.register("gateway-boss", () -> {
            configService.reloadGatewayBossConfig();
            return true;
        });
        reloadService.register("drops", () -> {
            configService.reloadMobsConfig();
            mobRegistry.load();
            mobDropRegistry.load();
            return true;
        });
        reloadService.register("mobs", () -> {
            configService.reloadMobsConfig();
            mobAbilityRegistry.load();
            mobRegistry.load();
            mobSpawnZoneRegistry.load();
            monsterSpawnRegistry.load();
            mobDropRegistry.load();
            return true;
        });
        reloadService.register("monster-spawns", () -> {
            configService.reloadMonsterSpawnsConfig();
            monsterSpawnRegistry.load();
            return true;
        });
        reloadService.register("mythic-mobs", () -> {
            configService.reloadMythicMobsConfig();
            mythicMobRegistry.load();
            return true;
        });
        reloadService.registerDetailed("effects", () -> {
            configService.reloadAlchemyEffectsConfigs();
            boolean valid = effectService.reload();
            return valid
                    ? RPGReloadService.ReloadOutcome.pass("effects")
                    : new RPGReloadService.ReloadOutcome(false, java.util.List.of(
                    RPGReloadService.ReloadDetail.fail("effects", String.join("; ", effectService.registry().lastErrors()))));
        });
        reloadService.registerDetailed("alchemy", this::reloadAlchemyRegistriesDetailed);
    }

    private RPGReloadService.ReloadOutcome reloadAlchemyRegistriesDetailed() {
        configService.reloadAlchemyEffectsConfigs();
        boolean effectsOk = effectService.validateReload();
        boolean potionsOk = effectsOk && potionRegistry.reload();
        boolean recipesOk = potionsOk && alchemyRecipeRegistry.reload();
        boolean catalystsOk = recipesOk && catalystRegistry.reload();
        boolean specialCatalystsOk = catalystsOk && specialCatalystRegistry.reload();
        java.util.List<RPGReloadService.ReloadDetail> details = new java.util.ArrayList<>();
        details.add(effectsOk ? RPGReloadService.ReloadDetail.pass("effects")
                : RPGReloadService.ReloadDetail.fail("effects", String.join("; ", effectService.registry().lastErrors())));
        details.add(!effectsOk ? RPGReloadService.ReloadDetail.skip("potions", "SKIPPED due to effects dependency failure")
                : potionsOk ? RPGReloadService.ReloadDetail.pass("potions")
                : RPGReloadService.ReloadDetail.fail("potions", "potion registry rejected candidate"));
        details.add(!potionsOk ? RPGReloadService.ReloadDetail.skip("recipes", "SKIPPED due to potion dependency failure")
                : recipesOk ? RPGReloadService.ReloadDetail.pass("recipes")
                : RPGReloadService.ReloadDetail.fail("recipes", "alchemy recipe registry rejected candidate"));
        details.add(!recipesOk ? RPGReloadService.ReloadDetail.skip("catalysts", "SKIPPED due to recipe dependency failure")
                : catalystsOk ? RPGReloadService.ReloadDetail.pass("catalysts")
                : RPGReloadService.ReloadDetail.fail("catalysts", "catalyst registry rejected candidate"));
        details.add(!catalystsOk ? RPGReloadService.ReloadDetail.skip("special-catalysts", "SKIPPED due to catalyst dependency failure")
                : specialCatalystsOk ? RPGReloadService.ReloadDetail.pass("special-catalysts")
                : RPGReloadService.ReloadDetail.fail("special-catalysts", "special catalyst registry rejected candidate"));
        boolean success = effectsOk && potionsOk && recipesOk && catalystsOk && specialCatalystsOk;
        if (success) {
            effectService.commitReload();
            specialCatalystExecutionService.cancelAll(com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystExecution.CancelReason.SERVER_RESTART);
            alchemyAuditLog.admin("reload", null, "alchemy registries committed; stale jobs cleared");
        }
        return new RPGReloadService.ReloadOutcome(success, details);
    }

    private RPGReloadService.ReloadOutcome reloadAllRegistriesDetailed() {
        ConfigDoctor.DoctorReport preflight = new ConfigDoctor(this).run("reload");
        if (!preflight.success()) {
            return new RPGReloadService.ReloadOutcome(false, reloadDetailsFromDoctor(preflight));
        }
        configService.reloadEquipmentGrowthConfig();
        configService.reloadItemsConfig();
        configService.reloadMobsConfig();
        configService.reloadAlchemyEffectsConfigs();
        itemRegistry.load();
        equipmentRegistry.load();
        enchantRegistry.load();
        configService.reloadSpecialEquipmentConfig();
        specialEquipmentRegistry.load();
        equipmentRegistry.load();
        mobAbilityRegistry.load();
        mobRegistry.load();
        configService.reloadMonsterSpawnsConfig();
        monsterSpawnRegistry.load();
        configService.reloadMythicMobsConfig();
        mythicMobRegistry.load();
        mobSpawnZoneRegistry.load();
        configService.reloadGatewayBossConfig();
        mobDropRegistry.load();
        java.util.List<RPGReloadService.ReloadDetail> details = new java.util.ArrayList<>();
        boolean effectsOk = effectService.validateReload();
        boolean potionsOk = effectsOk && potionRegistry.reload();
        boolean alchemyRecipesOk = potionsOk && alchemyRecipeRegistry.reload();
        boolean catalystsOk = alchemyRecipesOk && catalystRegistry.reload();
        boolean specialCatalystsOk = catalystsOk && specialCatalystRegistry.reload();
        details.add(effectsOk
                ? RPGReloadService.ReloadDetail.pass("effects")
                : RPGReloadService.ReloadDetail.fail("effects", String.join("; ", effectService.registry().lastErrors())));
        details.add(!effectsOk ? RPGReloadService.ReloadDetail.skip("potions", "SKIPPED due to effects dependency failure")
                : potionsOk ? RPGReloadService.ReloadDetail.pass("potions")
                : RPGReloadService.ReloadDetail.fail("potions", "potion registry rejected candidate"));
        details.add(!potionsOk ? RPGReloadService.ReloadDetail.skip("alchemy-recipes", "SKIPPED due to potion dependency failure")
                : alchemyRecipesOk ? RPGReloadService.ReloadDetail.pass("alchemy-recipes")
                : RPGReloadService.ReloadDetail.fail("alchemy-recipes", "alchemy recipe registry rejected candidate"));
        details.add(!alchemyRecipesOk ? RPGReloadService.ReloadDetail.skip("catalysts", "SKIPPED due to recipe dependency failure")
                : catalystsOk ? RPGReloadService.ReloadDetail.pass("catalysts")
                : RPGReloadService.ReloadDetail.fail("catalysts", "catalyst registry rejected candidate"));
        details.add(!catalystsOk ? RPGReloadService.ReloadDetail.skip("special-catalysts", "SKIPPED due to catalyst dependency failure")
                : specialCatalystsOk ? RPGReloadService.ReloadDetail.pass("special-catalysts")
                : RPGReloadService.ReloadDetail.fail("special-catalysts", "special catalyst registry rejected candidate"));
        if (!effectsOk || !potionsOk || !alchemyRecipesOk || !catalystsOk
                || !specialCatalystsOk) {
            return new RPGReloadService.ReloadOutcome(false, details);
        }
        effectService.commitReload();
        specialCatalystExecutionService.cancelAll(com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystExecution.CancelReason.SERVER_RESTART);
        alchemyAuditLog.admin("reload-all", null, "alchemy registries committed; stale jobs cleared");
        return new RPGReloadService.ReloadOutcome(true, details);
    }

    private java.util.List<RPGReloadService.ReloadDetail> reloadDetailsFromDoctor(ConfigDoctor.DoctorReport report) {
        java.util.List<RPGReloadService.ReloadDetail> details = new java.util.ArrayList<>();
        for (String group : java.util.List.of("items", "effects")) {
            java.util.List<String> errors = report.lines().stream()
                    .filter(line -> line.startsWith("ERROR") && line.contains(group))
                    .toList();
            if (errors.isEmpty()) {
                details.add(RPGReloadService.ReloadDetail.pass(group));
            } else {
                details.add(new RPGReloadService.ReloadDetail(group, "FAIL", errors));
            }
        }
        return details;
    }

    private void refreshOnlineEnchantLore() {
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) refreshEnchantLore(item);
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) refreshEnchantLore(item);
            refreshEnchantLore(player.getInventory().getItemInOffHand());
        }
    }

    private void refreshEnchantLore(org.bukkit.inventory.ItemStack item) {
        enchantService.refreshEquippedLore(item);
        enchantService.refreshBookLore(item);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDataListener(playerDataService), this);
        getServer().getPluginManager().registerEvents(gatewayPrototypeService, this);
        getServer().getPluginManager().registerEvents(effectService, this);
        getServer().getPluginManager().registerEvents(effectMovementLockService, this);
        getServer().getPluginManager().registerEvents(productionEffectListener, this);
        getServer().getPluginManager().registerEvents(potionUseListener, this);
        getServer().getPluginManager().registerEvents(specialCatalystExecutionService, this);
        getServer().getPluginManager().registerEvents(
                new com.hyunseo.hyunseorpg.alchemy.brewing.BrewingStandAlchemyListener(
                        alchemyRecipeRegistry, potionFactory,
                        new com.hyunseo.hyunseorpg.alchemy.brewing.BrewingInputResolver(
                                potionPdc, itemService, catalystRegistry, specialCatalystRegistry)), this);
        getServer().getPluginManager().registerEvents(new CooldownCleanupListener(cooldownService), this);
        getServer().getPluginManager().registerEvents(new SkillInputListener(
                this, equipmentEffectTriggerEngine, enchantService, equipmentInstanceService, alchemyCombatAdapter,
                item -> DedicatedWeaponIds.owns(specialEquipmentService.getSpecialId(item))), this);
        getServer().getPluginManager().registerEvents(new EquipmentEffectTriggerListener(
                equipmentEffectTriggerEngine, combatService, equipmentInstanceService), this);
        getServer().getPluginManager().registerEvents(equipmentEnchantContentService, this);
        getServer().getPluginManager().registerEvents(
                new com.hyunseo.hyunseorpg.enchant.EnchantLoreRefreshListener(this, enchantService), this);
        getServer().getPluginManager().registerEvents(
                new com.hyunseo.hyunseorpg.enchant.NativeEnchantMigrationListener(enchantService), this);
        getServer().getPluginManager().registerEvents(new MiningActivityListener(
                miningActivityService, activityBlockRepository), this);
        getServer().getPluginManager().registerEvents(vanillaStackingService, this);
        getServer().getPluginManager().registerEvents(soulboundItemService, this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(
                mobService, mobLevelScalingService, zombieVariantService), this);
        getServer().getPluginManager().registerEvents(
                new com.hyunseo.hyunseorpg.mob.VanillaWitchSpawnBlockListener(), this);
        getServer().getPluginManager().registerEvents(monsterSpawnService, this);
        getServer().getPluginManager().registerEvents(monsterBehaviorService, this);
        getServer().getPluginManager().registerEvents(thousandEyesController, this);
        getServer().getPluginManager().registerEvents(zombieVariantService, this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(
                mobRewardService, mobService, inventoryDeliveryService, mythicMobIntegrationService), this);
        getServer().getPluginManager().registerEvents(new VanillaMobFragmentDropListener(
                configService, mobService, itemService, inventoryDeliveryService, elementalFragmentPolicy), this);
        getServer().getPluginManager().registerEvents(new CustomItemVanillaActionBlockListener(itemService), this);
        getServer().getPluginManager().registerEvents(mythicCustomMobService, this);
        getServer().getPluginManager().registerEvents(dimensionVisitTracker, this);
        getServer().getPluginManager().registerEvents(naturalDiscoveryService, this);
        // Stage 1: vanilla enchanting, anvils, trades and loot must remain authoritative.
        getServer().getPluginManager().registerEvents(new EquipmentActualEffectListener(
                configService, equipmentTierService, equipmentEnhancementService,
                combatService, itemService, toolDurabilityService,
                item -> specialEquipmentService.getSpecialId(item).equals(WaterTridentListener.ID)), this);
        getServer().getPluginManager().registerEvents(new SpecialEquipmentProgressListener(playerDataService), this);
        this.specialEquipmentEffectListener = new SpecialEquipmentEffectListener(
                configService, specialEquipmentService, itemService,
                combatService, cooldownService);
        getServer().getPluginManager().registerEvents(specialEquipmentEffectListener, this);
        this.waterTridentListener = new WaterTridentListener(
                configService, specialEquipmentService, combatService, cooldownService);
        getServer().getPluginManager().registerEvents(waterTridentListener, this);
        this.flameAxeListener = new FlameAxeListener(this, configService, specialEquipmentService,
                equipmentInstanceService, combatService);
        getServer().getPluginManager().registerEvents(flameAxeListener, this);
        this.thanatosMaceListener = new ThanatosMaceListener(this, configService, specialEquipmentService,
                equipmentInstanceService, combatService, cooldownService, effectMovementLockService);
        getServer().getPluginManager().registerEvents(thanatosMaceListener, this);
        this.thunderAxeListener = new ThunderAxeListener(this, configService, specialEquipmentService,
                equipmentInstanceService, combatService, cooldownService);
        getServer().getPluginManager().registerEvents(thunderAxeListener, this);
        this.solarisListener = new SolarisListener(this, configService, specialEquipmentService,
                equipmentInstanceService, combatService, cooldownService);
        getServer().getPluginManager().registerEvents(solarisListener, this);
        this.moonlitAfterglowListener = new MoonlitAfterglowListener(configService, specialEquipmentService,
                combatService, cooldownService);
        getServer().getPluginManager().registerEvents(moonlitAfterglowListener, this);
        getServer().getPluginManager().registerEvents(new VanillaAnvilPolicyListener(), this);
        getServer().getPluginManager().registerEvents(
                new VanillaAnvilEnhancementListener(equipmentEnhancementService), this);
    }

    private void loadCurrentlyOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            playerDataService.loadPlayer(player);
        }
    }

    private void logRuntimeDiagnostics() {
        long loadedHyunseoPlugins = java.util.Arrays.stream(getServer().getPluginManager().getPlugins())
                .filter(plugin -> plugin instanceof HyunseoRPGPlugin)
                .count();
        long configuredRetiredDefinitions = enchantRegistry.countConfiguredRetiredEnchants();
        String jarPath;
        try {
            jarPath = getFile().getAbsolutePath();
        } catch (RuntimeException exception) {
            jarPath = "unavailable (" + exception.getClass().getSimpleName() + ")";
        }
        getLogger().info("[HyunseoRPG Runtime] Plugin name: " + getDescription().getName());
        getLogger().info("[HyunseoRPG Runtime] Plugin version: " + getDescription().getVersion());
        getLogger().info("[HyunseoRPG Runtime] Main class: " + getClass().getName());
        getLogger().info("[HyunseoRPG Runtime] Loaded JAR path: " + jarPath);
        getLogger().info("[HyunseoRPG Runtime] JAR file name: " + new java.io.File(jarPath).getName());
        getLogger().info("[HyunseoRPG Runtime] Build identifier: " + new java.io.File(jarPath).getName());
        logJarIntegrity(new java.io.File(jarPath));
        getLogger().info("[HyunseoRPG Runtime] Config data folder: " + getDataFolder().getAbsolutePath());
        getLogger().info("[HyunseoRPG Runtime] enchants.yml schema version: "
                + configService.getEnchantsInt("schema-version", 0));
        getLogger().info("[HyunseoRPG Runtime] enchant lore version: "
                + configService.getEnchantsInt("enchant-lore-version", 0));
        getLogger().info("[HyunseoRPG Runtime] Registered enchant count: " + enchantRegistry.getAll().size());
        getLogger().info("[HyunseoRPG Runtime] HyunseoRPG plugin instances: " + loadedHyunseoPlugins
                + ", retired definitions in live config (ignored): " + configuredRetiredDefinitions);
    }

    private void logJarIntegrity(java.io.File jar) {
        try {
            String jarHash = jar.isFile() ? sha256(jar) : "unavailable";
            byte[] embeddedItems = null;
            if (jar.isFile()) {
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
                    java.util.zip.ZipEntry entry = zip.getEntry("items.yml");
                    if (entry != null) {
                        try (java.io.InputStream input = zip.getInputStream(entry)) {
                            embeddedItems = input.readAllBytes();
                        }
                    }
                }
            }
            if (embeddedItems == null) {
                try (java.io.InputStream input = getResource("items.yml")) {
                    if (input != null) embeddedItems = input.readAllBytes();
                }
            }
            java.io.File externalItemsFile = new java.io.File(getDataFolder(), "items.yml");
            byte[] externalItems = externalItemsFile.isFile()
                    ? java.nio.file.Files.readAllBytes(externalItemsFile.toPath()) : null;
            getLogger().info("[HyunseoRPG Runtime] JAR SHA-256: " + jarHash);
            getLogger().info("[HyunseoRPG Runtime] embedded items.yml SHA-256: "
                    + (embeddedItems == null ? "missing" : sha256(embeddedItems)));
            getLogger().info("[HyunseoRPG Runtime] external items.yml SHA-256: "
                    + (externalItems == null ? "missing" : sha256(externalItems)));
            getLogger().info("[HyunseoRPG Runtime] embedded enchant books: " + countEnchantBooks(embeddedItems));
            getLogger().info("[HyunseoRPG Runtime] external enchant books: " + countEnchantBooks(externalItems));
        } catch (Exception exception) {
            getLogger().warning("[HyunseoRPG Runtime] Unable to inspect loaded JAR resources: "
                    + exception.getClass().getSimpleName());
        }
    }

    private String sha256(java.io.File file) throws java.io.IOException {
        try (java.io.InputStream input = new java.io.FileInputStream(file)) {
            return sha256(input);
        }
    }

    private String sha256(byte[] bytes) throws java.security.NoSuchAlgorithmException {
        return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private String sha256(java.io.InputStream input) throws java.io.IOException {
        java.security.MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new java.io.IOException("SHA-256 is unavailable", exception);
        }
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) digest.update(buffer, 0, read);
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private int countEnchantBooks(byte[] bytes) {
        if (bytes == null) return 0;
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?m)^\\s+enchant_book_[A-Za-z0-9_]+\\s*:")
                .matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private void validateEnchantIntegrity() {
        java.util.Set<String> handlers = java.util.Set.of("content", "skill");
        java.util.Set<String> itemIds = itemRegistry.getAll().stream()
                .map(com.hyunseo.hyunseorpg.item.RPGItemData::itemId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.List<String> problems = enchantRegistry.validateIntegrity(handlers, itemIds);
        if (problems.isEmpty()) {
            getLogger().info("[HyunseoRPG/ConfigDoctor] enchant registry integrity: OK ("
                    + enchantRegistry.getAll().size() + " definitions)");
            return;
        }
        for (String problem : problems) {
            getLogger().warning("[HyunseoRPG/ConfigDoctor] " + problem);
        }
    }

    private void runStartupConfigMigration() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Unable to create plugin data folder before config migration.");
        }
        try {
            ConfigMigrationService.MigrationReport report = configMigrationService
                    .migrate("configs", true);
            for (String line : report.lines()) {
                if (line.startsWith("ERROR")) getLogger().severe(line);
                else if (!line.startsWith("[HyunseoRPG Migration]")) getLogger().info(line);
            }
            if (!report.success()) {
                getLogger().severe("Startup config migration failed; continuing with existing live configuration.");
            }
        } catch (RuntimeException exception) {
            getLogger().log(java.util.logging.Level.SEVERE,
                    "Startup config migration failed; continuing with existing live configuration.", exception);
        }
    }
}
