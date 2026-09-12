package com.hyunseo.hyunseorpg;

import com.hyunseo.hyunseorpg.classsystem.ClassStatRegistry;
import com.hyunseo.hyunseorpg.classsystem.ClassStatService;
import com.hyunseo.hyunseorpg.classsystem.ClassService;
import com.hyunseo.hyunseorpg.classsystem.ClassWeaponService;
import com.hyunseo.hyunseorpg.activity.ActivityCoinListener;
import com.hyunseo.hyunseorpg.activity.ActivityCoinRewardService;
import com.hyunseo.hyunseorpg.boss.BossSessionManager;
import com.hyunseo.hyunseorpg.boss.BossSummonProtectionListener;
import com.hyunseo.hyunseorpg.command.ClassResetCommand;
import com.hyunseo.hyunseorpg.command.ClassSelectCommand;
import com.hyunseo.hyunseorpg.command.ClassStatGuiCommand;
import com.hyunseo.hyunseorpg.command.CraftingCommand;
import com.hyunseo.hyunseorpg.command.RPGGiveCommand;
import com.hyunseo.hyunseorpg.command.RPGTestCommand;
import com.hyunseo.hyunseorpg.prototype.thousandeyes.ThousandEyesController;
import com.hyunseo.hyunseorpg.rpgtest.gateway.GatewayPrototypeService;
import com.hyunseo.hyunseorpg.command.WeaponProficiencyCommand;
import com.hyunseo.hyunseorpg.command.RPGCooldownCommand;
import com.hyunseo.hyunseorpg.command.RPGLevelAdminCommand;
import com.hyunseo.hyunseorpg.command.RPGMobCommand;
import com.hyunseo.hyunseorpg.command.RPGQuestCommand;
import com.hyunseo.hyunseorpg.command.RPGStatAdminCommand;
import com.hyunseo.hyunseorpg.command.RPGStatBalanceCommand;
import com.hyunseo.hyunseorpg.command.SkillStatGuiCommand;
import com.hyunseo.hyunseorpg.command.ShopAdminCommand;
import com.hyunseo.hyunseorpg.command.ShopCommand;
import com.hyunseo.hyunseorpg.command.StatGuiCommand;
import com.hyunseo.hyunseorpg.command.SpecialEquipmentCommand;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigDoctor;
import com.hyunseo.hyunseorpg.core.config.ConfigMigrationService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.core.config.RPGReloadService;
import com.hyunseo.hyunseorpg.economy.CoinDisplayTask;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.enhancement.VanillaEnchantBlockListener;
import com.hyunseo.hyunseorpg.enhancement.AnvilGrowthListener;
import com.hyunseo.hyunseorpg.enhancement.EnhancementRegistry;
import com.hyunseo.hyunseorpg.enhancement.EquipmentGrowthConfigValidator;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentGrowthGuiService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentPromotionService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentRepairService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentSupportGuiService;
import com.hyunseo.hyunseorpg.enhancement.FutureEquipmentFeatureRegistry;
import com.hyunseo.hyunseorpg.equipment.HoeHarvestModifierService;
import com.hyunseo.hyunseorpg.equipment.ToolDurabilityService;
import com.hyunseo.hyunseorpg.enchant.EnchantRegistry;
import com.hyunseo.hyunseorpg.enchant.EnchantService;
import com.hyunseo.hyunseorpg.enchant.EnchantRuntimeStateService;
import com.hyunseo.hyunseorpg.enchant.EquipmentEnchantContentService;
import com.hyunseo.hyunseorpg.equipment.trigger.EquipmentEffectTriggerEngine;
import com.hyunseo.hyunseorpg.equipment.trigger.EquipmentEffectTriggerListener;
import com.hyunseo.hyunseorpg.equipment.EquipmentOptionRegistry;
import com.hyunseo.hyunseorpg.equipment.EquipmentOptionService;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.equipment.EquipmentActualEffectListener;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.equipment.EquipmentMetadataService;
import com.hyunseo.hyunseorpg.equipment.EquipmentRegistry;
import com.hyunseo.hyunseorpg.exp.ExpService;
import com.hyunseo.hyunseorpg.exp.ExpTable;
import com.hyunseo.hyunseorpg.exp.LevelPlayerListener;
import com.hyunseo.hyunseorpg.exp.LevelService;
import com.hyunseo.hyunseorpg.mana.ManaBossBarService;
import com.hyunseo.hyunseorpg.mana.ManaPlayerListener;
import com.hyunseo.hyunseorpg.mana.ManaRegenTask;
import com.hyunseo.hyunseorpg.mana.ManaService;
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
import com.hyunseo.hyunseorpg.progression.ProgressionLoopRegistry;
import com.hyunseo.hyunseorpg.progression.ProgressionAccessRewardService;
import com.hyunseo.hyunseorpg.progression.MagicStoneFragmentService;
import com.hyunseo.hyunseorpg.progression.DimensionVisitTracker;
import com.hyunseo.hyunseorpg.quest.QuestProgressListener;
import com.hyunseo.hyunseorpg.quest.QuestRegistry;
import com.hyunseo.hyunseorpg.quest.QuestService;
import com.hyunseo.hyunseorpg.quest.AutoQuestService;
import com.hyunseo.hyunseorpg.quest.availability.ContentAvailabilityService;
import com.hyunseo.hyunseorpg.quest.availability.ItemObtainabilityService;
import com.hyunseo.hyunseorpg.quest.availability.MonsterEligibilityService;
import com.hyunseo.hyunseorpg.quest.availability.PlayerDiscoveryService;
import com.hyunseo.hyunseorpg.activity.ActivityBlockRepository;
import com.hyunseo.hyunseorpg.activity.ActivityBlockRewardValidator;
import com.hyunseo.hyunseorpg.farming.CropBlockAdapter;
import com.hyunseo.hyunseorpg.farming.CropGrowthService;
import com.hyunseo.hyunseorpg.farming.CropIndex;
import com.hyunseo.hyunseorpg.farming.CropQualityService;
import com.hyunseo.hyunseorpg.farming.CropRegistry;
import com.hyunseo.hyunseorpg.farming.CropStorage;
import com.hyunseo.hyunseorpg.farming.FarmingProfileService;
import com.hyunseo.hyunseorpg.farming.FarmingStage;
import com.hyunseo.hyunseorpg.farming.FarmingPromotionService;
import com.hyunseo.hyunseorpg.farming.FarmingHoePromotionService;
import com.hyunseo.hyunseorpg.farming.DeliveryRegistry;
import com.hyunseo.hyunseorpg.farming.DeliveryService;
import com.hyunseo.hyunseorpg.farming.DeliveryDataService;
import com.hyunseo.hyunseorpg.farming.DeliveryItemValidator;
import com.hyunseo.hyunseorpg.farming.DeliveryRewardCalculator;
import com.hyunseo.hyunseorpg.farming.DeliveryGuiService;
import com.hyunseo.hyunseorpg.farming.AbundancePointService;
import com.hyunseo.hyunseorpg.farming.FavorService;
import com.hyunseo.hyunseorpg.farming.FarmingHubGuiService;
import com.hyunseo.hyunseorpg.farming.FarmingEssenceService;
import com.hyunseo.hyunseorpg.farming.FarmingItemBridge;
import com.hyunseo.hyunseorpg.farming.FarmingStatTokenService;
import com.hyunseo.hyunseorpg.farming.FarmingStatTokenListener;
import com.hyunseo.hyunseorpg.farming.VanillaCropBlockAdapter;
import com.hyunseo.hyunseorpg.farming.YamlChunkCropStorage;
import com.hyunseo.hyunseorpg.alchemy.EffectService;
import com.hyunseo.hyunseorpg.alchemy.BerserkEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.BleedEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.CorrosionEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.FrostbiteEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.NecrosisEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.ProductionEffectListener;
import com.hyunseo.hyunseorpg.alchemy.EffectListGuiService;
import com.hyunseo.hyunseorpg.command.EffectCommand;
import com.hyunseo.hyunseorpg.alchemy.ShockEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.VampirismEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.VulnerabilityEffectHandler;
import com.hyunseo.hyunseorpg.alchemy.catalyst.BoundedSpecialCatalystExecutionService;
import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystApplicationService;
import com.hyunseo.hyunseorpg.alchemy.catalyst.YamlCatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.catalyst.YamlSpecialCatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.gui.AlchemyCatalystGuiService;
import com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiControllerService;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionFactory;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionPdcContract;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionUseListener;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionUseService;
import com.hyunseo.hyunseorpg.alchemy.potion.YamlPotionRegistry;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeRegistry;
import com.hyunseo.hyunseorpg.alchemy.recipe.YamlAlchemyRecipeRegistry;
import com.hyunseo.hyunseorpg.exploration.ExplorationModule;
import com.hyunseo.hyunseorpg.exploration.integration.ExistingHyunseoRpgAdapters;
import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.integration.BukkitExplorationPorts;
import com.hyunseo.hyunseorpg.activity.MiningActivityListener;
import com.hyunseo.hyunseorpg.activity.MiningActivityService;
import com.hyunseo.hyunseorpg.crafting.CraftingService;
import com.hyunseo.hyunseorpg.crafting.CraftingGuiService;
import com.hyunseo.hyunseorpg.crafting.CraftingLayoutRegistry;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeData;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeRegistry;
import com.hyunseo.hyunseorpg.crafting.CraftingTransactionService;
import com.hyunseo.hyunseorpg.crafting.SoulboundItemService;
import com.hyunseo.hyunseorpg.skill.CooldownCleanupListener;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.skill.SkillInputListener;
import com.hyunseo.hyunseorpg.skill.SkillRegistry;
import com.hyunseo.hyunseorpg.skill.SkillStatService;
import com.hyunseo.hyunseorpg.skill.SkillService;
import com.hyunseo.hyunseorpg.skill.bowmaster.BowmasterSkillService;
import com.hyunseo.hyunseorpg.skill.lancer.LancerSkillService;
import com.hyunseo.hyunseorpg.skill.swordmaster.SwordmasterBasicAttackListener;
import com.hyunseo.hyunseorpg.skill.swordmaster.SwordmasterBladeService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentEffectListener;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentMenuService;
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
import com.hyunseo.hyunseorpg.shop.ShopGuiListener;
import com.hyunseo.hyunseorpg.shop.ShopGuiService;
import com.hyunseo.hyunseorpg.shop.ShopRegistry;
import com.hyunseo.hyunseorpg.shop.ShopService;
import com.hyunseo.hyunseorpg.stat.StatCalculator;
import com.hyunseo.hyunseorpg.stat.StatModifierCleanupListener;
import com.hyunseo.hyunseorpg.stat.StatModifierService;
import com.hyunseo.hyunseorpg.stat.StatService;
import com.hyunseo.hyunseorpg.ui.StatGuiListener;
import com.hyunseo.hyunseorpg.ui.StatGuiService;
import com.hyunseo.hyunseorpg.ui.RPGMenuListener;
import com.hyunseo.hyunseorpg.ui.RPGMenuService;
import com.hyunseo.hyunseorpg.weapon.WeaponProficiencyListener;
import com.hyunseo.hyunseorpg.weapon.WeaponProficiencyService;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponItemService;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class HyunseoRPGPlugin extends JavaPlugin {
    private ConfigService configService;
    private WeaponService weaponService;
    private WeaponProficiencyService weaponProficiencyService;
    private WeaponItemService weaponItemService;
    private RPGItemRegistry itemRegistry;
    private RPGItemService itemService;
    private InventoryDeliveryService inventoryDeliveryService;
    private PendingRewardService pendingRewardService;
    private NaturalDiscoveryService naturalDiscoveryService;
    private PlayerDiscoveryService playerDiscoveryService;
    private ContentAvailabilityService contentAvailabilityService;
    private RPGReloadService reloadService;
    private ClassWeaponService classWeaponService;
    private ClassService classService;
    private ClassStatRegistry classStatRegistry;
    private ClassStatService classStatService;
    private PlayerDataRepository playerDataRepository;
    private PlayerDataCache playerDataCache;
    private PlayerDataService playerDataService;
    private CoinService coinService;
    private CoinDisplayTask coinDisplayTask;
    private ShopRegistry shopRegistry;
    private ShopService shopService;
    private ShopGuiService shopGuiService;
    private RequirementChecker requirementChecker;
    private DimensionVisitTracker dimensionVisitTracker;
    private BossSessionManager bossSessionManager;
    private ProgressionLoopRegistry progressionLoopRegistry;
    private ProgressionAccessRewardService progressionAccessRewardService;
    private MagicStoneFragmentService magicStoneFragmentService;
    private RPGMenuService rpgMenuService;
    private QuestRegistry questRegistry;
    private QuestService questService;
    private AutoQuestService autoQuestService;
    private ManaBossBarService manaBossBarService;
    private ManaService manaService;
    private ManaRegenTask manaRegenTask;
    private StatCalculator statCalculator;
    private StatModifierService statModifierService;
    private StatService statService;
    private SkillStatService skillStatService;
    private StatGuiService statGuiService;
    private CombatService combatService;
    private CooldownService cooldownService;
    private SkillRegistry skillRegistry;
    private SwordmasterBladeService swordmasterBladeService;
    private BowmasterSkillService bowmasterSkillService;
    private LancerSkillService lancerSkillService;
    private SkillService skillService;
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
    private CraftingService craftingService;
    private CraftingGuiService craftingGuiService;
    private CraftingRecipeRegistry craftingRecipeRegistry;
    private CraftingLayoutRegistry craftingLayoutRegistry;
    private CraftingTransactionService craftingTransactionService;
    private VanillaStackingService vanillaStackingService;
    private ActivityBlockRepository activityBlockRepository;
    private ActivityBlockRewardValidator activityBlockRewardValidator;
    private SoulboundItemService soulboundItemService;
    private MiningActivityService miningActivityService;
    private ActivityCoinRewardService activityCoinRewardService;
    private CropGrowthService cropGrowthService;
    private FarmingProfileService farmingProfileService;
    private FarmingPromotionService farmingPromotionService;
    private FarmingHoePromotionService farmingHoePromotionService;
    private DeliveryRegistry deliveryRegistry;
    private DeliveryService deliveryService;
    private DeliveryGuiService deliveryGuiService;
    private AbundancePointService abundancePointService;
    private FavorService favorService;
    private FarmingHubGuiService farmingHubGuiService;
    private FarmingEssenceService farmingEssenceService;
    private FarmingItemBridge farmingItemBridge;
    private FarmingStatTokenService farmingStatTokenService;
    private CropQualityService cropQualityService;
    private HoeHarvestModifierService hoeHarvestModifierService;
    private ToolDurabilityService toolDurabilityService;
    private EquipmentOptionRegistry equipmentOptionRegistry;
    private EquipmentOptionService equipmentOptionService;
    private EnhancementRegistry enhancementRegistry;
    private EquipmentEnhancementService equipmentEnhancementService;
    private EquipmentGrowthGuiService equipmentGrowthGuiService;
    private EquipmentPromotionService equipmentPromotionService;
    private EquipmentTierService equipmentTierService;
    private EquipmentRepairService equipmentRepairService;
    private EquipmentMetadataService equipmentMetadataService;
    private EquipmentRegistry equipmentRegistry;
    private EquipmentGrowthPolicy equipmentGrowthPolicy;
    private SpecialEquipmentRegistry specialEquipmentRegistry;
    private SpecialEquipmentService specialEquipmentService;
    private SpecialEquipmentMenuService specialEquipmentMenuService;
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
    private EquipmentSupportGuiService equipmentSupportGuiService;
    private FutureEquipmentFeatureRegistry futureEquipmentFeatureRegistry;
    private ConfigMigrationService configMigrationService;
    private EffectService effectService;
    private com.hyunseo.hyunseorpg.alchemy.EffectMovementLockService effectMovementLockService;
    private ProductionEffectListener productionEffectListener;
    private EffectListGuiService effectListGuiService;
    private com.hyunseo.hyunseorpg.alchemy.PaperAlchemyCombatAdapter alchemyCombatAdapter;
    private PotionRegistry potionRegistry;
    private PaperPotionPdcContract potionPdc;
    private PotionFactory potionFactory;
    private PaperPotionUseListener potionUseListener;
    private AlchemyRecipeRegistry alchemyRecipeRegistry;
    private YamlCatalystRegistry catalystRegistry;
    private YamlSpecialCatalystRegistry specialCatalystRegistry;
    private BoundedSpecialCatalystExecutionService specialCatalystExecutionService;
    private CatalystApplicationService catalystApplicationService;
    private AlchemyCatalystGuiService alchemyCatalystGui;
    private AlchemyGuiControllerService alchemyGuiController;
    private com.hyunseo.hyunseorpg.alchemy.AlchemyAuditLog alchemyAuditLog;
    private ExplorationModule explorationModule;
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
        this.effectListGuiService = new EffectListGuiService(this, effectService);
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
        this.alchemyRecipeRegistry = new YamlAlchemyRecipeRegistry(configService, potionRegistry, itemService);
        this.alchemyRecipeRegistry.reload();
        this.catalystRegistry = new YamlCatalystRegistry(configService);
        this.specialCatalystRegistry = new YamlSpecialCatalystRegistry(configService);
        this.catalystRegistry.reload();
        this.specialCatalystRegistry.reload();
        this.specialCatalystExecutionService = new BoundedSpecialCatalystExecutionService(
                this, specialCatalystRegistry, potionRegistry, effectService, 128);
        this.potionUseListener = new PaperPotionUseListener(this, potionPdc,
                new PaperPotionUseService(potionRegistry, potionPdc, effectService, itemService, 1,
                        catalystRegistry, specialCatalystExecutionService), specialCatalystExecutionService);
        this.catalystApplicationService = new CatalystApplicationService(
                potionRegistry, potionPdc, catalystRegistry, itemService, 1);
        this.alchemyAuditLog = new com.hyunseo.hyunseorpg.alchemy.AlchemyAuditLog(this);
        this.cropQualityService = new CropQualityService(configService, itemService);
        this.cropQualityService.load();
        this.vanillaStackingService = new VanillaStackingService(configService, itemService);
        this.itemService.setItemNormalizer(vanillaStackingService::normalize);
        this.vanillaStackingService.setAdditionalNormalizer(itemService::normalizeFarmingItem);
        this.inventoryDeliveryService = new InventoryDeliveryService();
        this.inventoryDeliveryService.setItemNormalizer(vanillaStackingService::normalize);
        this.naturalDiscoveryService = new NaturalDiscoveryService(this, configService, playerDataService, itemService);
        this.inventoryDeliveryService.setDeliveryObserver(naturalDiscoveryService::discoverDeliveredItem);
        this.weaponService = new WeaponService(this);
        this.weaponProficiencyService = new WeaponProficiencyService(configService, playerDataService);
        this.weaponItemService = new WeaponItemService(configService, weaponService, itemService);
        this.soulboundItemService = new SoulboundItemService(this, itemService);
        this.craftingRecipeRegistry = new CraftingRecipeRegistry(configService, itemService);
        if (!this.craftingRecipeRegistry.load()) {
            getLogger().warning("Unable to load canonical crafting recipes; continuing with the last valid snapshot.");
        }
        this.craftingLayoutRegistry = new CraftingLayoutRegistry(configService);
        if (!this.craftingLayoutRegistry.reload(craftingRecipeRegistry)) {
            getLogger().warning("Unable to load canonical crafting layout; continuing with the last valid snapshot.");
        }
        this.craftingTransactionService = new CraftingTransactionService(craftingRecipeRegistry, itemService,
                soulboundItemService, vanillaStackingService, naturalDiscoveryService::discoverDeliveredItem);
        this.craftingTransactionService.setDynamicOutputResolver((player, recipe) ->
                potionFactory.create(recipe.outputId(), recipe.outputAmount()));
        this.craftingService = new CraftingService(craftingTransactionService);
        this.craftingGuiService = new CraftingGuiService(this, itemService, craftingRecipeRegistry,
                craftingLayoutRegistry, craftingTransactionService);
        try {
            this.activityBlockRepository = new ActivityBlockRepository(this);
            this.activityBlockRewardValidator = new ActivityBlockRewardValidator(
                    activityBlockRepository, configService);
        } catch (SQLException exception) {
            throw new IllegalStateException("광부 설치 광석 저장소를 초기화할 수 없습니다.", exception);
        }
        this.equipmentOptionRegistry = new EquipmentOptionRegistry(configService);
        this.equipmentOptionRegistry.load();
        this.equipmentOptionService = new EquipmentOptionService(this, configService, equipmentOptionRegistry);
        this.coinService = new CoinService(playerDataService);
        this.pendingRewardService = new PendingRewardService(this, coinService);
        this.inventoryDeliveryService.setPendingRewardService(pendingRewardService);
        this.activityCoinRewardService = new ActivityCoinRewardService(this, configService, coinService);
        CropRegistry cropRegistry = new CropRegistry(configService, itemService);
        this.farmingProfileService = new FarmingProfileService(playerDataService, cropRegistry);
        this.farmingEssenceService = new FarmingEssenceService(configService);
        this.farmingItemBridge = new FarmingItemBridge(itemService, cropQualityService, farmingEssenceService);
        this.farmingStatTokenService = new FarmingStatTokenService(
                this, configService, itemService, farmingProfileService);
        this.farmingStatTokenService.load();
        this.craftingTransactionService.setRecipeAccessAllowed(this::canAccessCraftingRecipe);
        this.deliveryRegistry = new DeliveryRegistry(configService, itemService);
        if (!this.deliveryRegistry.load()) {
            getLogger().warning("Unable to load farming delivery definitions: "
                    + String.join("; ", deliveryRegistry.lastErrors()));
        }
        this.abundancePointService = new AbundancePointService(playerDataService);
        this.craftingTransactionService.setAbundancePointService(abundancePointService);
        this.favorService = new FavorService(configService);
        this.favorService.load();
        this.deliveryService = new DeliveryService(
                new DeliveryDataService(playerDataService, abundancePointService), deliveryRegistry);
        this.deliveryGuiService = new DeliveryGuiService(this, deliveryService,
                new DeliveryItemValidator(itemService, cropQualityService),
                new DeliveryRewardCalculator(cropQualityService, configService, favorService),
                inventoryDeliveryService, itemService, farmingProfileService);
        CropIndex cropIndex = new CropIndex();
        CropStorage cropStorage = new YamlChunkCropStorage(this);
        CropBlockAdapter cropBlockAdapter = new VanillaCropBlockAdapter();
        this.cropGrowthService = new CropGrowthService(
                this, configService, itemService, cropRegistry, cropIndex, cropStorage, cropBlockAdapter,
                farmingProfileService, inventoryDeliveryService);
        this.cropGrowthService.harvestService().setQualityService(cropQualityService);
        this.cropGrowthService.harvestService().setAbundancePointService(abundancePointService);
        this.progressionAccessRewardService = new ProgressionAccessRewardService(
                configService, itemService, inventoryDeliveryService);
        this.magicStoneFragmentService = new MagicStoneFragmentService(
                configService, itemService, inventoryDeliveryService, activityBlockRewardValidator,
                cropGrowthService.harvestValidator());
        this.miningActivityService = new MiningActivityService(configService, activityCoinRewardService,
                activityBlockRepository, progressionAccessRewardService, activityBlockRewardValidator);
        this.enhancementRegistry = new EnhancementRegistry(configService);
        this.equipmentTierService = new EquipmentTierService(configService, itemService);
        this.equipmentMetadataService = new EquipmentMetadataService(this, itemService, equipmentTierService);
        this.itemService.setItemNormalizer(item -> {
            itemService.normalizeFarmingItem(item);
            vanillaStackingService.normalize(item);
            equipmentMetadataService.ensureDataVersion(item);
        });
        this.weaponItemService.setEquipmentMetadataService(equipmentMetadataService);
        this.equipmentEnhancementService = new EquipmentEnhancementService(this, itemService, enhancementRegistry);
        this.equipmentPromotionService = new EquipmentPromotionService(this, configService, itemService, equipmentEnhancementService, equipmentTierService);
        this.equipmentEnhancementService.setPromotionService(equipmentPromotionService);
        this.specialEquipmentRegistry = new SpecialEquipmentRegistry(configService);
        this.specialEquipmentRegistry.load();
        this.equipmentGrowthPolicy = new EquipmentGrowthPolicy(
                configService, itemService, equipmentTierService, specialEquipmentRegistry);
        this.equipmentEnhancementService.setGrowthPolicy(equipmentGrowthPolicy);
        this.equipmentPromotionService.setGrowthPolicy(equipmentGrowthPolicy);
        this.farmingHoePromotionService = new FarmingHoePromotionService(
                this, configService, equipmentTierService);
        this.farmingHoePromotionService.load();
        this.deliveryGuiService.setHoePromotionService(farmingHoePromotionService);
        this.cropGrowthService.harvestService().setHoePromotionService(farmingHoePromotionService);
        this.hoeHarvestModifierService = new HoeHarvestModifierService(
                configService, equipmentTierService, equipmentEnhancementService);
        this.farmingPromotionService = new FarmingPromotionService(
                this, configService, itemService, farmingProfileService, equipmentTierService, equipmentEnhancementService);
        this.farmingPromotionService.setHoePromotionService(farmingHoePromotionService);
        this.equipmentRegistry = new EquipmentRegistry(configService, itemRegistry, itemService,
                enhancementRegistry, equipmentTierService, specialEquipmentRegistry);
        this.equipmentRegistry.load();
        this.specialEquipmentService = new SpecialEquipmentService(this, configService, specialEquipmentRegistry,
                itemService, playerDataService, inventoryDeliveryService, equipmentEnhancementService,
                equipmentPromotionService);
        this.specialEquipmentService.setCraftingTransactionService(craftingTransactionService, craftingRecipeRegistry);
        this.specialEquipmentMenuService = new SpecialEquipmentMenuService(this, specialEquipmentService);
        this.equipmentRepairService = new EquipmentRepairService(configService, coinService, equipmentTierService,
                equipmentGrowthPolicy, equipmentEnhancementService, equipmentPromotionService);
        this.equipmentGrowthGuiService = new EquipmentGrowthGuiService(this, equipmentEnhancementService, equipmentPromotionService);
        this.equipmentGrowthGuiService.setCoinService(coinService);
        this.equipmentGrowthGuiService.setRepairService(equipmentRepairService);
        this.equipmentGrowthGuiService.setGrowthPolicy(equipmentGrowthPolicy);
        this.coinDisplayTask = new CoinDisplayTask(this, configService, coinService, abundancePointService);
        this.shopRegistry = new ShopRegistry(configService, itemService, getLogger());
        this.shopRegistry.load();
        this.shopService = new ShopService(coinService, itemService, vanillaStackingService);
        this.shopService.setFarmingSaleModifier(hoeHarvestModifierService, cropQualityService);
        this.shopService.setFarmingPurchaseGate(farmingProfileService);
        this.shopGuiService = new ShopGuiService(this, shopRegistry, shopService, coinService);
        this.equipmentGrowthGuiService.setShopGuiService(shopGuiService);
        this.requirementChecker = new RequirementChecker(playerDataService, coinService);
        this.dimensionVisitTracker = new DimensionVisitTracker(playerDataService);
        this.statCalculator = new StatCalculator(configService);
        this.statModifierService = new StatModifierService();
        this.combatService = new CombatService();
        this.combatService.setEquipmentEnhancementService(equipmentEnhancementService);
        this.combatService.setEquipmentPromotionService(equipmentPromotionService);
        this.combatService.setEquipmentTierService(equipmentTierService);
        this.combatService.setConfigService(configService);
        this.manaBossBarService = new ManaBossBarService();
        this.manaService = new ManaService(configService, playerDataService, manaBossBarService, statCalculator, statModifierService);
        this.statService = new StatService(playerDataService, statCalculator, statModifierService, manaService);
        this.combatService.setStatService(statService);
        this.classStatRegistry = new ClassStatRegistry(configService);
        this.classStatRegistry.load();
        this.classStatService = new ClassStatService(playerDataService, classStatRegistry);
        this.skillRegistry = new SkillRegistry(configService);
        this.skillRegistry.load();
        this.swordmasterBladeService = new SwordmasterBladeService(this, configService, combatService, classStatService);
        this.bowmasterSkillService = new BowmasterSkillService(this, configService, weaponService, combatService, classStatService);
        this.lancerSkillService = new LancerSkillService(this, configService, combatService, classStatService);
        this.skillStatService = new SkillStatService(configService, playerDataService, skillRegistry, weaponProficiencyService);
        this.statGuiService = new StatGuiService(this, statService, skillStatService, classStatService);
        this.cooldownService = new CooldownService(configService.getSpecialEquipmentBoolean(
                "special-equipment.testing.disable-cooldowns", false));
        this.skillService = new SkillService(configService, playerDataService, weaponService, weaponProficiencyService, manaService, cooldownService, combatService, skillRegistry, swordmasterBladeService, bowmasterSkillService, lancerSkillService);
        this.enchantRegistry = new EnchantRegistry(configService);
        this.enchantRegistry.load();
        this.equipmentInstanceService = new EquipmentInstanceService(this);
        this.enchantRuntimeStateService = new EnchantRuntimeStateService(equipmentInstanceService);
        this.enchantService = new EnchantService(
                this, configService, enchantRegistry, itemService, equipmentPromotionService, weaponService, equipmentTierService,
                specialEquipmentRegistry, equipmentInstanceService);
        this.enchantService.setGrowthPolicy(equipmentGrowthPolicy);
        this.toolDurabilityService = new ToolDurabilityService(
                configService, equipmentTierService, equipmentPromotionService,
                enchantService, hoeHarvestModifierService);
        this.cropGrowthService.harvestService().setHoeHarvestServices(
                hoeHarvestModifierService, toolDurabilityService);
        this.equipmentGrowthGuiService.setFarmingPromotionService(farmingPromotionService);
        this.itemService.setItemNormalizer(item -> {
            itemService.normalizeFarmingItem(item);
            vanillaStackingService.normalize(item);
            equipmentMetadataService.ensureDataVersion(item);
            farmingHoePromotionService.ensureData(item);
            enchantService.refreshBookLore(item);
        });
        this.inventoryDeliveryService.setItemNormalizer(item -> {
            itemService.normalizeFarmingItem(item);
            vanillaStackingService.normalize(item);
            equipmentMetadataService.ensureDataVersion(item);
            farmingHoePromotionService.ensureData(item);
            enchantService.refreshBookLore(item);
        });
        this.pendingRewardService.setItemNormalizer(vanillaStackingService::normalize);
        this.equipmentEffectTriggerEngine = new EquipmentEffectTriggerEngine(this, enchantRegistry, enchantService,
                cooldownService, equipmentInstanceService, enchantRuntimeStateService);
        this.equipmentEnchantContentService = new EquipmentEnchantContentService(this, configService, combatService,
                enchantService, equipmentInstanceService, enchantRuntimeStateService, equipmentTierService, itemService,
                equipmentPromotionService, cooldownService, swordmasterBladeService,
                activityBlockRewardValidator);
        this.equipmentEffectTriggerEngine.registerHandler("content", equipmentEnchantContentService);
        validateEnchantIntegrity();
        this.equipmentGrowthGuiService.setEnchantService(enchantService);
        this.futureEquipmentFeatureRegistry = new FutureEquipmentFeatureRegistry(configService);
        this.equipmentSupportGuiService = new EquipmentSupportGuiService(
                this, configService, itemService, equipmentTierService, equipmentGrowthPolicy,
                equipmentInstanceService, equipmentPromotionService, enchantService, coinService,
                shopGuiService, futureEquipmentFeatureRegistry);
        this.equipmentGrowthGuiService.setSupportService(equipmentSupportGuiService);
        this.skillService.setEnchantService(enchantService);
        this.skillService.setEquipmentEffectTriggerEngine(equipmentEffectTriggerEngine);
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
        this.bossSessionManager = new BossSessionManager(
                this, configService, itemService, inventoryDeliveryService, coinService, playerDataService,
                pendingRewardService);
        this.mobDropRegistry = new MobDropRegistry(this, configService);
        this.mobDropRegistry.load();
        this.playerDiscoveryService = new PlayerDiscoveryService(playerDataService, mobTagService);
        MonsterEligibilityService monsterEligibilityService = new MonsterEligibilityService(
                configService, playerDataService, mobRegistry, monsterSpawnRegistry, playerDiscoveryService);
        ItemObtainabilityService itemObtainabilityService = new ItemObtainabilityService(
                configService, itemRegistry, itemService, craftingRecipeRegistry, mobDropRegistry, shopRegistry, playerDataService);
        this.contentAvailabilityService = new ContentAvailabilityService(monsterEligibilityService, itemObtainabilityService);
        this.progressionLoopRegistry = new ProgressionLoopRegistry(
                configService, itemRegistry);
        this.progressionLoopRegistry.loadAndValidate();
        this.mobDropService = new MobDropService(this, mobService, mobDropRegistry, itemService, inventoryDeliveryService,
                elementalFragmentPolicy);
        this.mobRewardService = new MobRewardService(
                mobService, expService, coinService, mobDropService,
                mythicMobIntegrationService);
        this.questRegistry = new QuestRegistry(this, configService);
        this.questRegistry.load();
        this.questService = new QuestService(playerDataService, questRegistry, requirementChecker, coinService, expService);
        this.autoQuestService = new AutoQuestService(configService, playerDataService, itemService,
                coinService, expService, contentAvailabilityService);
        this.manaRegenTask = new ManaRegenTask(this, configService, manaService);
        this.rpgMenuService = new RPGMenuService(
                this, shopGuiService, equipmentGrowthGuiService, statGuiService, bossSessionManager,
                craftingGuiService, coinService, itemService, craftingRecipeRegistry, craftingTransactionService,
                soulboundItemService, questService, autoQuestService);
        this.alchemyCatalystGui = new AlchemyCatalystGuiService(this, catalystApplicationService,
                catalystRegistry, inventoryDeliveryService, itemService);
        this.alchemyGuiController = new AlchemyGuiControllerService(configService, craftingGuiService);
        this.farmingHubGuiService = new FarmingHubGuiService(
                craftingGuiService, deliveryGuiService, farmingProfileService);
        this.farmingHubGuiService.setMainMenuOpener(rpgMenuService::openMain);
        this.rpgMenuService.setFarmingHubGuiService(farmingHubGuiService);
        this.alchemyGuiController.setMainMenuOpener(rpgMenuService::openMain);
        this.alchemyGuiController.setCatalystOpener(player -> alchemyCatalystGui.open(player, alchemyGuiController::openInventory));
        this.rpgMenuService.setAlchemyGuiController(alchemyGuiController);

        this.explorationModule = new ExplorationModule(this,
                BukkitExplorationPorts.compose(
                        this,
                        ExistingHyunseoRpgAdapters.mobPort(mobService),
                        ExistingHyunseoRpgAdapters.itemRewardPort(itemService, inventoryDeliveryService),
                        ExistingHyunseoRpgAdapters.entityCleanupPort(mobService)),
                null);
        this.gatewayPrototypeService = new GatewayPrototypeService(this, configService);

        configureReloadService();
        reloadService.register("exploration", explorationModule::reload);

        registerCommandsSafe();
        registerListeners();
        if (!explorationModule.start()) {
            getLogger().severe("Exploration module failed to start; keeping it disabled for this boot.");
        }
        loadCurrentlyOnlinePlayers();
        cropGrowthService.start();
        playerDataService.startAutosave();
        manaRegenTask.start();
        coinDisplayTask.start();
        swordmasterBladeService.start();
        zombieVariantService.start();
        bossSessionManager.start();
        mythicCustomMobService.start();
        monsterSpawnService.start();
        monsterBehaviorService.start();
        effectService.start();

        logRuntimeDiagnostics();
        getLogger().info("HyunseoRPG enabled for Paper 26.1.2.");
    }

    @Override
    public void onDisable() {
        if (equipmentGrowthGuiService != null) {
            equipmentGrowthGuiService.returnOpenInputs();
        }
        if (equipmentSupportGuiService != null) equipmentSupportGuiService.clearSessions();
        if (alchemyGuiController != null) alchemyGuiController.closeAll(com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiController.CloseReason.SERVER_SHUTDOWN);
        if (alchemyCatalystGui != null) alchemyCatalystGui.closeAll();
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
        if (manaRegenTask != null) {
            manaRegenTask.cancel();
        }
        if (activityCoinRewardService != null) {
            activityCoinRewardService.clear();
        }
        if (effectService != null) effectService.shutdown();
        if (cropGrowthService != null) {
            cropGrowthService.shutdown();
        }
        if (deliveryGuiService != null) deliveryGuiService.returnOpenInputs();
        if (coinDisplayTask != null) {
            coinDisplayTask.cancel();
        }
        if (manaBossBarService != null) {
            manaBossBarService.removeAll();
        }
        if (statModifierService != null) {
            statModifierService.clearAll();
        }
        if (cooldownService != null) {
            cooldownService.clearAll();
        }
        if (swordmasterBladeService != null) {
            swordmasterBladeService.clearAll();
        }
        if (bowmasterSkillService != null) {
            bowmasterSkillService.clearAll();
        }
        if (lancerSkillService != null) {
            lancerSkillService.clearAll();
        }
        if (zombieVariantService != null) {
            zombieVariantService.cancelAll();
        }
        if (bossSessionManager != null) {
            bossSessionManager.stop();
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
        if (explorationModule != null) {
            explorationModule.stop();
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

    public ClassWeaponService getClassWeaponService() {
        return classWeaponService;
    }

    public ClassService getClassService() {
        return classService;
    }

    public PlayerDataRepository getPlayerDataRepository() {
        return playerDataRepository;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public FarmingProfileService getFarmingProfileService() {
        return farmingProfileService;
    }

    public AbundancePointService getAbundancePointService() {
        return abundancePointService;
    }

    public FarmingEssenceService getFarmingEssenceService() {
        return farmingEssenceService;
    }

    public FarmingItemBridge getFarmingItemBridge() {
        return farmingItemBridge;
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
    public CatalystApplicationService getCatalystApplicationService() { return catalystApplicationService; }

    private boolean canAccessCraftingRecipe(Player player, CraftingRecipeData recipe) {
        if (recipe.farmingType().equalsIgnoreCase("essence")) {
            if (!farmingEssenceService.enabled()
                    || !recipe.ingredients().isEmpty()
                    || !recipe.outputId().equals(farmingEssenceService.resultItemId())
                    || recipe.outputAmount() != farmingEssenceService.resultCount()
                    || recipe.requiredAbundancePoints() != farmingEssenceService.requiredAbundancePoints()) {
                return false;
            }
            return FarmingStage.fromInput(farmingEssenceService.unlockStage())
                    .map(required -> farmingProfileService.getFarmingProfile(player).stage().atLeast(required))
                    .orElse(false);
        }
        if (recipe.requiredFarmingStage().isBlank()) return true;
        return FarmingStage.fromInput(recipe.requiredFarmingStage())
                .map(required -> farmingProfileService.getFarmingProfile(player).stage().atLeast(required))
                .orElse(false);
    }

    public FarmingStatTokenService getFarmingStatTokenService() {
        return farmingStatTokenService;
    }

    public FavorService getFavorService() {
        return favorService;
    }

    public CoinService getCoinService() {
        return coinService;
    }

    public ManaService getManaService() {
        return manaService;
    }

    public StatService getStatService() {
        return statService;
    }

    public LevelService getLevelService() {
        return levelService;
    }

    public ExpService getExpService() {
        return expService;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public SkillService getSkillService() {
        return skillService;
    }

    public MobService getMobService() {
        return mobService;
    }

    private void registerCommandsSafe() {
        PluginCommand rpgCommand = getCommand("rpg");
        if (rpgCommand == null) {
            getLogger().severe("Command 'rpg' is missing from plugin.yml.");
        } else {
            RPGGiveCommand giveCommand = new RPGGiveCommand(
                    itemRegistry, itemService, soulboundItemService, reloadService, weaponItemService,
                    rpgMenuService, zombieVariantService);
            giveCommand.setMaintenanceServices(new ConfigDoctor(this), new ConfigMigrationService(this));
            giveCommand.setPendingRewardService(pendingRewardService);
            giveCommand.setFarmingServices(farmingProfileService, farmingPromotionService, playerDataService);
            giveCommand.setFarmingOperations(farmingStatTokenService, cropGrowthService);
            giveCommand.setFarmingDiagnostics(deliveryService, cropQualityService);
            giveCommand.setFarmingAuditLogger(message -> getLogger().info("[FarmingAdmin] " + message));
            giveCommand.setDeliveryGuiService(deliveryGuiService);
            giveCommand.setFarmingHubGuiService(farmingHubGuiService);
            giveCommand.setEffectService(effectService);
            giveCommand.setEffectListGuiService(effectListGuiService);
            giveCommand.setAlchemyServices(potionRegistry,
                    potionPdc,
                    specialCatalystExecutionService, alchemyGuiController, alchemyAuditLog);
            giveCommand.setPotionFactory(potionFactory);
            giveCommand.setSpecialEquipmentService(specialEquipmentService);
            giveCommand.setInventoryNormalizer(vanillaStackingService::normalizeAndMergeInventory);
            giveCommand.setExplorationModule(explorationModule);
            rpgCommand.setExecutor(giveCommand);
            rpgCommand.setTabCompleter(giveCommand);
        }

        PluginCommand effectCommand = getCommand("effectlist");
        if (effectCommand == null) {
            getLogger().severe("Command 'effectlist' is missing from plugin.yml.");
        } else {
            EffectCommand executor = new EffectCommand(effectListGuiService);
            effectCommand.setExecutor(executor);
            effectCommand.setTabCompleter(executor);
        }

        PluginCommand rpgTestCommand = getCommand("rpgtest");
        if (rpgTestCommand == null) {
            getLogger().severe("Command 'rpgtest' is missing from plugin.yml.");
        } else {
            RPGTestCommand testCommand = new RPGTestCommand(
                    coinService, itemRegistry, itemService, soulboundItemService, weaponItemService,
                    equipmentEnhancementService, equipmentPromotionService, bossSessionManager, reloadService,
                    equipmentMetadataService, equipmentRegistry, gatewayPrototypeService, thousandEyesController);
            rpgTestCommand.setExecutor(testCommand);
            rpgTestCommand.setTabCompleter(testCommand);
        }

        PluginCommand craftingCommand = getCommand("crafting");
        if (craftingCommand == null) {
            getLogger().severe("Command 'crafting' is missing from plugin.yml.");
        } else {
            CraftingCommand executor = new CraftingCommand(craftingGuiService, craftingRecipeRegistry,
                    craftingLayoutRegistry, craftingTransactionService);
            craftingCommand.setExecutor(executor);
            craftingCommand.setTabCompleter(executor);
        }

        PluginCommand weaponInfoCommand = getCommand("weaponinfo");
        if (weaponInfoCommand == null) {
            getLogger().severe("Command 'weaponinfo' is missing from plugin.yml.");
        } else {
            WeaponProficiencyCommand proficiencyCommand = new WeaponProficiencyCommand(weaponProficiencyService);
            weaponInfoCommand.setExecutor(proficiencyCommand);
            weaponInfoCommand.setTabCompleter(proficiencyCommand);
        }

        PluginCommand statAdminCommand = getCommand("rpgstat");
        if (statAdminCommand == null) {
            getLogger().severe("Command 'rpgstat' is missing from plugin.yml.");
        } else {
            RPGStatAdminCommand statAdminExecutor = new RPGStatAdminCommand(statService, manaService);
            statAdminCommand.setExecutor(statAdminExecutor);
            statAdminCommand.setTabCompleter(statAdminExecutor);
        }

        PluginCommand statBalanceCommand = getCommand("rpgstatbalance");
        if (statBalanceCommand == null) {
            getLogger().severe("Command 'rpgstatbalance' is missing from plugin.yml.");
        } else {
            RPGStatBalanceCommand statBalanceExecutor = new RPGStatBalanceCommand(configService);
            statBalanceCommand.setExecutor(statBalanceExecutor);
            statBalanceCommand.setTabCompleter(statBalanceExecutor);
        }

        PluginCommand statGuiCommand = getCommand("stats");
        if (statGuiCommand == null) {
            getLogger().severe("Command 'stats' is missing from plugin.yml.");
        } else {
            statGuiCommand.setExecutor(new StatGuiCommand(statGuiService));
        }

        PluginCommand skillStatGuiCommand = getCommand("skillstats");
        if (skillStatGuiCommand == null) {
            getLogger().severe("Command 'skillstats' is missing from plugin.yml.");
        } else {
            skillStatGuiCommand.setExecutor(new SkillStatGuiCommand(statGuiService));
        }

        PluginCommand classStatGuiCommand = getCommand("weaponstats");
        if (classStatGuiCommand == null) {
            getLogger().severe("Command 'classstats' is missing from plugin.yml.");
        } else {
            classStatGuiCommand.setExecutor(new ClassStatGuiCommand(statGuiService));
        }

        PluginCommand levelAdminCommand = getCommand("rpglevel");
        if (levelAdminCommand == null) {
            getLogger().severe("Command 'rpglevel' is missing from plugin.yml.");
        } else {
            RPGLevelAdminCommand levelAdminExecutor = new RPGLevelAdminCommand(playerDataService, expService, levelService);
            levelAdminCommand.setExecutor(levelAdminExecutor);
            levelAdminCommand.setTabCompleter(levelAdminExecutor);
        }

        PluginCommand cooldownCommand = getCommand("rpgcooldown");
        if (cooldownCommand == null) {
            getLogger().severe("Command 'rpgcooldown' is missing from plugin.yml.");
        } else {
            RPGCooldownCommand cooldownExecutor = new RPGCooldownCommand(configService, cooldownService);
            cooldownCommand.setExecutor(cooldownExecutor);
            cooldownCommand.setTabCompleter(cooldownExecutor);
        }

        PluginCommand rpgMobCommand = getCommand("rpgmob");
        if (rpgMobCommand == null) {
            getLogger().severe("Command 'rpgmob' is missing from plugin.yml.");
        } else {
            RPGMobCommand rpgMobExecutor = new RPGMobCommand(configService, mobService, mobLevelScalingService,
                    mythicCustomMobService, monsterBehaviorService);
            rpgMobCommand.setExecutor(rpgMobExecutor);
            rpgMobCommand.setTabCompleter(rpgMobExecutor);
        }

        PluginCommand rpgQuestCommand = getCommand("rpgquest");
        if (rpgQuestCommand == null) {
            getLogger().severe("Command 'rpgquest' is missing from plugin.yml.");
        } else {
            RPGQuestCommand rpgQuestExecutor = new RPGQuestCommand(questService);
            rpgQuestCommand.setExecutor(rpgQuestExecutor);
            rpgQuestCommand.setTabCompleter(rpgQuestExecutor);
        }
        PluginCommand shopCommand = getCommand("shop");
        if (shopCommand == null) {
            getLogger().severe("Command 'shop' is missing from plugin.yml.");
        } else {
            ShopCommand shopExecutor = new ShopCommand(shopGuiService, shopRegistry);
            shopCommand.setExecutor(shopExecutor);
            shopCommand.setTabCompleter(shopExecutor);
        }

        PluginCommand shopAdminCommand = getCommand("shopadmin");
        if (shopAdminCommand == null) {
            getLogger().severe("Command 'shopadmin' is missing from plugin.yml.");
        } else {
            ShopAdminCommand shopAdminExecutor = new ShopAdminCommand(shopGuiService, shopRegistry);
            shopAdminCommand.setExecutor(shopAdminExecutor);
            shopAdminCommand.setTabCompleter(shopAdminExecutor);
        }

        PluginCommand specialEquipmentCommand = getCommand("specialequipment");
        if (specialEquipmentCommand == null) {
            getLogger().severe("Command 'specialequipment' is missing from plugin.yml.");
        } else {
            SpecialEquipmentCommand executor = new SpecialEquipmentCommand(specialEquipmentService, specialEquipmentMenuService);
            specialEquipmentCommand.setExecutor(executor);
            specialEquipmentCommand.setTabCompleter(executor);
        }
    }

    private void registerCommands() {
        PluginCommand classSelectCommand = getCommand("직업선택");
        if (classSelectCommand == null) {
            getLogger().severe("Command '직업선택' is missing from plugin.yml.");
            return;
        }

        ClassSelectCommand classSelectExecutor = new ClassSelectCommand(classService);
        classSelectCommand.setExecutor(classSelectExecutor);
        classSelectCommand.setTabCompleter(classSelectExecutor);

        PluginCommand classResetCommand = getCommand("직업초기화");
        if (classResetCommand == null) {
            getLogger().severe("Command '직업초기화' is missing from plugin.yml.");
            return;
        }

        classResetCommand.setExecutor(new ClassResetCommand(classService));

        PluginCommand statAdminCommand = getCommand("rpgstat");
        if (statAdminCommand == null) {
            getLogger().severe("Command 'rpgstat' is missing from plugin.yml.");
            return;
        }

        RPGStatAdminCommand statAdminExecutor = new RPGStatAdminCommand(statService, manaService);
        statAdminCommand.setExecutor(statAdminExecutor);
        statAdminCommand.setTabCompleter(statAdminExecutor);

        PluginCommand statBalanceCommand = getCommand("rpgstatbalance");
        if (statBalanceCommand == null) {
            getLogger().severe("Command 'rpgstatbalance' is missing from plugin.yml.");
            return;
        }
        RPGStatBalanceCommand statBalanceExecutor = new RPGStatBalanceCommand(configService);
        statBalanceCommand.setExecutor(statBalanceExecutor);
        statBalanceCommand.setTabCompleter(statBalanceExecutor);

        PluginCommand statGuiCommand = getCommand("스탯");
        if (statGuiCommand == null) {
            getLogger().severe("Command '스탯' is missing from plugin.yml.");
            return;
        }
        statGuiCommand.setExecutor(new StatGuiCommand(statGuiService));

        PluginCommand skillStatGuiCommand = getCommand("스킬스탯");
        if (skillStatGuiCommand == null) {
            getLogger().severe("Command '스킬스탯' is missing from plugin.yml.");
            return;
        }
        skillStatGuiCommand.setExecutor(new SkillStatGuiCommand(statGuiService));

        PluginCommand classStatGuiCommand = getCommand("직업스탯");
        if (classStatGuiCommand == null) {
            getLogger().severe("Command '직업스탯' is missing from plugin.yml.");
            return;
        }
        classStatGuiCommand.setExecutor(new ClassStatGuiCommand(statGuiService));

        PluginCommand levelAdminCommand = getCommand("rpglevel");
        if (levelAdminCommand == null) {
            getLogger().severe("Command 'rpglevel' is missing from plugin.yml.");
            return;
        }

        RPGLevelAdminCommand levelAdminExecutor = new RPGLevelAdminCommand(playerDataService, expService, levelService);
        levelAdminCommand.setExecutor(levelAdminExecutor);
        levelAdminCommand.setTabCompleter(levelAdminExecutor);

        PluginCommand cooldownCommand = getCommand("rpgcooldown");
        if (cooldownCommand == null) {
            getLogger().severe("Command 'rpgcooldown' is missing from plugin.yml.");
            return;
        }

        RPGCooldownCommand cooldownExecutor = new RPGCooldownCommand(configService, cooldownService);
        cooldownCommand.setExecutor(cooldownExecutor);
        cooldownCommand.setTabCompleter(cooldownExecutor);

        PluginCommand rpgMobCommand = getCommand("rpgmob");
        if (rpgMobCommand == null) {
            getLogger().severe("Command 'rpgmob' is missing from plugin.yml.");
            return;
        }

        RPGMobCommand rpgMobExecutor = new RPGMobCommand(configService, mobService, mobLevelScalingService,
                mythicCustomMobService, monsterBehaviorService);
        rpgMobCommand.setExecutor(rpgMobExecutor);
        rpgMobCommand.setTabCompleter(rpgMobExecutor);

        PluginCommand rpgQuestCommand = getCommand("rpgquest");
        if (rpgQuestCommand == null) {
            getLogger().severe("Command 'rpgquest' is missing from plugin.yml.");
            return;
        }

        RPGQuestCommand rpgQuestExecutor = new RPGQuestCommand(questService, autoQuestService,
                rpgMenuService, contentAvailabilityService, playerDiscoveryService);
        rpgQuestCommand.setExecutor(rpgQuestExecutor);
        rpgQuestCommand.setTabCompleter(rpgQuestExecutor);
        PluginCommand questCommand = getCommand("quest");
        if (questCommand != null) {
            questCommand.setExecutor(rpgQuestExecutor);
            questCommand.setTabCompleter(rpgQuestExecutor);
        }

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
        reloadService.register("promotion", () -> {
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
        reloadService.register("repair", () -> true);
        reloadService.register("equipment-support", () -> {
            configService.reloadEquipmentSupportConfig();
            return true;
        });
        reloadService.register("special-equipment", () -> {
            configService.reloadSpecialEquipmentConfig();
            specialEquipmentRegistry.load();
            equipmentRegistry.load();
            if (specialEquipmentRegistry.getAll().isEmpty()) return false;
            if (!craftingRecipeRegistry.load()) return false;
            if (!craftingLayoutRegistry.reload(craftingRecipeRegistry)) return false;
            return !specialEquipmentRegistry.getAll().isEmpty();
        });
        reloadService.register("quests", () -> {
            configService.reloadQuestsConfig();
            questRegistry.load();
            return true;
        });
        reloadService.registerDetailed("items", () -> {
            configService.reloadItemsConfig();
            itemRegistry.load();
            equipmentRegistry.load();
            return RPGReloadService.ReloadOutcome.pass("items");
        });
        reloadService.register("vanilla-enchants", () -> true);
        reloadService.register("enchant", () -> {
            configService.reloadEnchantsConfig();
            enchantRegistry.load();
            refreshOnlineEnchantLore();
            return true;
        });
        reloadService.register("bosses", () -> {
            configService.reloadBossesConfig();
            return true;
        });
        reloadService.register("drops", () -> {
            configService.reloadMobsConfig();
            mobRegistry.load();
            mobDropRegistry.load();
            return true;
        });
        reloadService.register("shops", () -> {
            shopRegistry.reload();
            return true;
        });
        reloadService.registerDetailed("crafting", () -> {
            configService.reloadCraftingConfig();
            configService.reloadFarmingQualityConfig();
            cropQualityService.load();
            if (!craftingRecipeRegistry.load()) {
                return new RPGReloadService.ReloadOutcome(false, java.util.List.of(
                        RPGReloadService.ReloadDetail.fail("crafting-recipes", String.join("; ", craftingRecipeRegistry.lastErrors()))));
            }
            if (!craftingLayoutRegistry.reload(craftingRecipeRegistry)) {
                return new RPGReloadService.ReloadOutcome(false, java.util.List.of(
                        RPGReloadService.ReloadDetail.fail("crafting-layout", String.join("; ", craftingLayoutRegistry.lastErrors()))));
            }
            return new RPGReloadService.ReloadOutcome(true, java.util.List.of(
                    RPGReloadService.ReloadDetail.pass("crafting-recipes"),
                    RPGReloadService.ReloadDetail.pass("crafting-layout")));
        });
        reloadService.register("recipes", () -> {
            configService.reloadCraftingConfig();
            configService.reloadFarmingQualityConfig();
            cropQualityService.load();
            return craftingRecipeRegistry.load()
                    && craftingLayoutRegistry.reload(craftingRecipeRegistry);
        });
        reloadService.register("skills", () -> {
            skillRegistry.load();
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
        reloadService.registerDetailed("progression-loop", () -> {
            configService.reloadProgressionLoopConfig();
            boolean valid = progressionLoopRegistry.loadAndValidate();
            return valid
                    ? RPGReloadService.ReloadOutcome.pass("progression-loop")
                    : new RPGReloadService.ReloadOutcome(false, java.util.List.of(
                        RPGReloadService.ReloadDetail.fail("progression-loop", String.join("; ", progressionLoopRegistry.lastErrors()))));
        });
        reloadService.register("activity-coins", () -> {
            configService.reloadProgressionLoopConfig();
            return progressionLoopRegistry.loadAndValidate();
        });
        reloadService.registerDetailed("farming", () -> {
            configService.reloadFarmingCropsConfig();
            configService.reloadFarmingGrowthConfig();
            configService.reloadFarmingHarvestConfig();
            configService.reloadFarmingProgressionConfig();
            configService.reloadFarmingQualityConfig();
            configService.reloadFarmingHoeEnhancementConfig();
            configService.reloadFarmingHoePromotionConfig();
            configService.reloadFarmingDeliveriesConfig();
            configService.reloadFarmingFavorConfig();
            configService.reloadFarmingEssenceConfig();
            configService.reloadFarmingStatTokensConfig();
            farmingHoePromotionService.load();
            farmingStatTokenService.load();
            favorService.load();
            cropQualityService.load();
            boolean deliveriesOk = deliveryRegistry.load();
            boolean farmingOk = cropGrowthService.reload();
            boolean recipesOk = craftingRecipeRegistry.load();
            boolean layoutOk = recipesOk && craftingLayoutRegistry.reload(craftingRecipeRegistry);
            java.util.List<RPGReloadService.ReloadDetail> details = new java.util.ArrayList<>();
            details.add(farmingOk
                    ? RPGReloadService.ReloadDetail.pass("farming")
                    : RPGReloadService.ReloadDetail.fail("farming", String.join("; ", cropGrowthService.registry().lastErrors())));
            details.add(deliveriesOk
                    ? RPGReloadService.ReloadDetail.pass("farming-deliveries")
                    : RPGReloadService.ReloadDetail.fail("farming-deliveries", String.join("; ", deliveryRegistry.lastErrors())));
            details.add(recipesOk
                    ? RPGReloadService.ReloadDetail.pass("crafting-recipes")
                    : RPGReloadService.ReloadDetail.fail("crafting-recipes", String.join("; ", craftingRecipeRegistry.lastErrors())));
            details.add(!recipesOk
                    ? RPGReloadService.ReloadDetail.skip("crafting-layout", "SKIPPED due to recipe dependency failure")
                    : layoutOk
                        ? RPGReloadService.ReloadDetail.pass("crafting-layout")
                        : RPGReloadService.ReloadDetail.fail("crafting-layout", String.join("; ", craftingLayoutRegistry.lastErrors())));
            return new RPGReloadService.ReloadOutcome(farmingOk && deliveriesOk && recipesOk && layoutOk, details);
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
            alchemyGuiController.closeAll(com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiController.CloseReason.REPLACED);
            alchemyCatalystGui.closeAll();
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
        configService.reloadEquipmentSupportConfig();
        configService.reloadItemsConfig();
        configService.reloadMobsConfig();
        configService.reloadCraftingConfig();
        configService.reloadProgressionLoopConfig();
        configService.reloadFarmingCropsConfig();
        configService.reloadFarmingGrowthConfig();
        configService.reloadFarmingHarvestConfig();
        configService.reloadFarmingProgressionConfig();
        configService.reloadFarmingQualityConfig();
        configService.reloadFarmingHoeEnhancementConfig();
        configService.reloadFarmingHoePromotionConfig();
        configService.reloadFarmingDeliveriesConfig();
        configService.reloadFarmingFavorConfig();
        configService.reloadFarmingEssenceConfig();
        configService.reloadFarmingStatTokensConfig();
        configService.reloadAlchemyEffectsConfigs();
        farmingHoePromotionService.load();
        farmingStatTokenService.load();
        favorService.load();
        boolean deliveriesOk = deliveryRegistry.load();
        configService.reloadQuestsConfig();
        itemRegistry.load();
        cropQualityService.load();
        equipmentRegistry.load();
        equipmentOptionRegistry.load();
        enchantRegistry.load();
        configService.reloadSpecialEquipmentConfig();
        specialEquipmentRegistry.load();
        equipmentRegistry.load();
        skillRegistry.load();
        classStatRegistry.load();
        mobAbilityRegistry.load();
        mobRegistry.load();
        configService.reloadMonsterSpawnsConfig();
        monsterSpawnRegistry.load();
        configService.reloadMythicMobsConfig();
        mythicMobRegistry.load();
        mobSpawnZoneRegistry.load();
        configService.reloadBossesConfig();
        mobDropRegistry.load();
        shopRegistry.reload();
        questRegistry.load();
        java.util.List<RPGReloadService.ReloadDetail> details = new java.util.ArrayList<>();
        boolean progressionOk = progressionLoopRegistry.loadAndValidate();
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
        details.add(progressionOk
                ? RPGReloadService.ReloadDetail.pass("progression-loop")
                : RPGReloadService.ReloadDetail.fail("progression-loop", String.join("; ", progressionLoopRegistry.lastErrors())));
        boolean farmingOk = cropGrowthService.reload();
        details.add(farmingOk
                ? RPGReloadService.ReloadDetail.pass("farming")
                : RPGReloadService.ReloadDetail.fail("farming", String.join("; ", cropGrowthService.registry().lastErrors())));
        details.add(deliveriesOk
                ? RPGReloadService.ReloadDetail.pass("farming-deliveries")
                : RPGReloadService.ReloadDetail.fail("farming-deliveries", String.join("; ", deliveryRegistry.lastErrors())));
        boolean recipesOk = craftingRecipeRegistry.load();
        details.add(recipesOk
                ? RPGReloadService.ReloadDetail.pass("crafting-recipes")
                : RPGReloadService.ReloadDetail.fail("crafting-recipes", String.join("; ", craftingRecipeRegistry.lastErrors())));
        boolean layoutOk = recipesOk && craftingLayoutRegistry.reload(craftingRecipeRegistry);
        details.add(!recipesOk
                ? RPGReloadService.ReloadDetail.skip("crafting-layout", "SKIPPED due to dependency failure")
                : layoutOk
                    ? RPGReloadService.ReloadDetail.pass("crafting-layout")
                    : RPGReloadService.ReloadDetail.fail("crafting-layout", String.join("; ", craftingLayoutRegistry.lastErrors())));
        if (!progressionOk || !effectsOk || !potionsOk || !alchemyRecipesOk || !catalystsOk
                || !specialCatalystsOk || !farmingOk || !deliveriesOk || !recipesOk || !layoutOk) {
            return new RPGReloadService.ReloadOutcome(false, details);
        }
        effectService.commitReload();
        specialCatalystExecutionService.cancelAll(com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystExecution.CancelReason.SERVER_RESTART);
        alchemyGuiController.closeAll(com.hyunseo.hyunseorpg.alchemy.gui.AlchemyGuiController.CloseReason.REPLACED);
        alchemyCatalystGui.closeAll();
        alchemyAuditLog.admin("reload-all", null, "alchemy registries committed; stale jobs cleared");
        return new RPGReloadService.ReloadOutcome(true, details);
    }

    private java.util.List<RPGReloadService.ReloadDetail> reloadDetailsFromDoctor(ConfigDoctor.DoctorReport report) {
        java.util.List<RPGReloadService.ReloadDetail> details = new java.util.ArrayList<>();
        for (String group : java.util.List.of("items", "progression-loop", "farming", "effects", "crafting-recipes", "crafting-layout")) {
            java.util.List<String> errors = report.lines().stream()
                    .filter(line -> line.startsWith("ERROR") && (line.contains(group)
                            || (group.equals("crafting-recipes") && line.contains("crafting.yml"))
                            || (group.equals("crafting-layout") && line.contains("layout"))))
                    .toList();
            if (group.equals("crafting-layout") && details.stream()
                    .anyMatch(detail -> detail.id().equals("crafting-recipes") && detail.status().equals("FAIL"))) {
                details.add(RPGReloadService.ReloadDetail.skip(group, "SKIPPED due to dependency failure"));
            } else if (errors.isEmpty()) {
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
        getServer().getPluginManager().registerEvents(new PlayerDataListener(playerDataService, statService), this);
        getServer().getPluginManager().registerEvents(gatewayPrototypeService, this);
        getServer().getPluginManager().registerEvents(effectService, this);
        getServer().getPluginManager().registerEvents(effectMovementLockService, this);
        getServer().getPluginManager().registerEvents(productionEffectListener, this);
        getServer().getPluginManager().registerEvents(effectListGuiService, this);
        getServer().getPluginManager().registerEvents(potionUseListener, this);
        getServer().getPluginManager().registerEvents(alchemyGuiController, this);
        getServer().getPluginManager().registerEvents(alchemyCatalystGui, this);
        getServer().getPluginManager().registerEvents(specialCatalystExecutionService, this);
        getServer().getPluginManager().registerEvents(
                new com.hyunseo.hyunseorpg.alchemy.AlchemyVanillaBypassListener(this, alchemyAuditLog), this);
        getServer().getPluginManager().registerEvents(new ManaPlayerListener(this, manaService, manaBossBarService), this);
        getServer().getPluginManager().registerEvents(new StatModifierCleanupListener(statModifierService), this);
        getServer().getPluginManager().registerEvents(new CooldownCleanupListener(cooldownService), this);
        getServer().getPluginManager().registerEvents(new RPGMenuListener(rpgMenuService), this);
        getServer().getPluginManager().registerEvents(new SkillInputListener(
                this, skillService, equipmentInstanceService, alchemyCombatAdapter,
                item -> DedicatedWeaponIds.owns(specialEquipmentService.getSpecialId(item))), this);
        getServer().getPluginManager().registerEvents(new EquipmentEffectTriggerListener(
                equipmentEffectTriggerEngine, combatService, equipmentInstanceService), this);
        getServer().getPluginManager().registerEvents(equipmentEnchantContentService, this);
        getServer().getPluginManager().registerEvents(
                new com.hyunseo.hyunseorpg.enchant.EnchantLoreRefreshListener(this, enchantService), this);
        getServer().getPluginManager().registerEvents(new WeaponProficiencyListener(configService, weaponService, weaponProficiencyService), this);
        getServer().getPluginManager().registerEvents(cropGrowthService, this);
        getServer().getPluginManager().registerEvents(new ActivityCoinListener(
                activityCoinRewardService, progressionAccessRewardService, activityBlockRepository,
                configService, activityBlockRewardValidator, cropGrowthService.harvestValidator()), this);
        getServer().getPluginManager().registerEvents(new MiningActivityListener(
                miningActivityService, activityBlockRepository), this);
        getServer().getPluginManager().registerEvents(craftingGuiService, this);
        getServer().getPluginManager().registerEvents(deliveryGuiService, this);
        getServer().getPluginManager().registerEvents(farmingHubGuiService, this);
        getServer().getPluginManager().registerEvents(new FarmingStatTokenListener(farmingStatTokenService), this);
        getServer().getPluginManager().registerEvents(vanillaStackingService, this);
        getServer().getPluginManager().registerEvents(soulboundItemService, this);
        getServer().getPluginManager().registerEvents(bowmasterSkillService, this);
        getServer().getPluginManager().registerEvents(new SwordmasterBasicAttackListener(configService, playerDataService, weaponService, combatService), this);
        getServer().getPluginManager().registerEvents(new StatGuiListener(this, statGuiService, statService, skillStatService, classStatService), this);
        getServer().getPluginManager().registerEvents(new LevelPlayerListener(this, levelService), this);
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
        getServer().getPluginManager().registerEvents(magicStoneFragmentService, this);
        getServer().getPluginManager().registerEvents(bossSessionManager, this);
        getServer().getPluginManager().registerEvents(new BossSummonProtectionListener(bossSessionManager), this);
        getServer().getPluginManager().registerEvents(mythicCustomMobService, this);
        getServer().getPluginManager().registerEvents(new QuestProgressListener(questService, autoQuestService), this);
        getServer().getPluginManager().registerEvents(playerDiscoveryService, this);
        getServer().getPluginManager().registerEvents(dimensionVisitTracker, this);
        getServer().getPluginManager().registerEvents(naturalDiscoveryService, this);
        getServer().getPluginManager().registerEvents(new VanillaEnchantBlockListener(configService), this);
        getServer().getPluginManager().registerEvents(new com.hyunseo.hyunseorpg.economy.EconomySafetyListener(), this);
        getServer().getPluginManager().registerEvents(new EquipmentActualEffectListener(
                configService, equipmentTierService, equipmentEnhancementService,
                equipmentPromotionService, combatService, itemService, activityBlockRewardValidator,
                toolDurabilityService,
                item -> specialEquipmentService.getSpecialId(item).equals(WaterTridentListener.ID)), this);
        getServer().getPluginManager().registerEvents(new SpecialEquipmentProgressListener(playerDataService), this);
        this.specialEquipmentEffectListener = new SpecialEquipmentEffectListener(
                configService, specialEquipmentService, itemService,
                combatService, cooldownService, equipmentPromotionService);
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
        getServer().getPluginManager().registerEvents(specialEquipmentMenuService, this);
        getServer().getPluginManager().registerEvents(new AnvilGrowthListener(this, equipmentGrowthGuiService), this);
        getServer().getPluginManager().registerEvents(equipmentSupportGuiService, this);
        getServer().getPluginManager().registerEvents(new ShopGuiListener(shopGuiService), this);
    }

    private void loadCurrentlyOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            playerDataService.loadPlayer(player);
            statService.refreshPlayerStats(player);
            manaService.refresh(player);
            levelService.refreshVanillaExpBar(player);
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
