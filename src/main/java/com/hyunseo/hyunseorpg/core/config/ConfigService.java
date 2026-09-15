package com.hyunseo.hyunseorpg.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigService {
    private final JavaPlugin plugin;
    private FileConfiguration statsConfig;
    private FileConfiguration expConfig;
    private FileConfiguration mobsConfig;
    private FileConfiguration classesConfig;
    private FileConfiguration questsConfig;
    private FileConfiguration skillsConfig;
    private FileConfiguration weaponsConfig;
    private FileConfiguration itemsConfig;
    private FileConfiguration equipmentGrowthConfig;
    private FileConfiguration enchantsConfig;
    private FileConfiguration equipmentInputsConfig;
    private FileConfiguration craftingConfig;
    private FileConfiguration mythicMobsConfig;
    private FileConfiguration bossesConfig;
    private FileConfiguration gatewayBossConfig;
    private FileConfiguration monsterSpawnsConfig;
    private FileConfiguration specialEquipmentConfig;
    private FileConfiguration farmingCropsConfig;
    private FileConfiguration farmingGrowthConfig;
    private FileConfiguration farmingHarvestConfig;
    private FileConfiguration farmingProgressionConfig;
    private FileConfiguration farmingQualityConfig;
    private FileConfiguration farmingHoeEnhancementConfig;
    private FileConfiguration farmingHoePromotionConfig;
    private FileConfiguration farmingCookingConfig;
    private FileConfiguration farmingDeliveriesConfig;
    private FileConfiguration farmingFavorConfig;
    private FileConfiguration farmingEssenceConfig;
    private FileConfiguration farmingStatTokensConfig;
    private FileConfiguration alchemyEffectsConfig;
    private FileConfiguration alchemyComponentsConfig;
    private FileConfiguration alchemyConflictsConfig;
    private FileConfiguration alchemyScalingConfig;
    private FileConfiguration alchemyAbundanceConfig;
    private FileConfiguration alchemyPotionsConfig;
    private FileConfiguration alchemyRecipesConfig;
    private FileConfiguration alchemyCatalystsConfig;
    private FileConfiguration alchemyGuiConfig;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadDefaults() {
        // Reload the live config before each registry reload. Do not copy defaults here:
        // doing so resurrects keys that an administrator intentionally removed.
        File mainConfigFile = new File(plugin.getDataFolder(), "config.yml");
        if (!mainConfigFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        this.statsConfig = loadManagedConfig("stats.yml");
        this.expConfig = loadManagedConfig("exp.yml");
        this.mobsConfig = loadManagedConfig("mobs.yml");
        this.classesConfig = loadManagedConfig("classes.yml");
        this.questsConfig = loadManagedConfig("quests.yml");
        this.skillsConfig = loadManagedConfig("skills.yml");
        this.weaponsConfig = loadManagedConfig("weapons.yml");
        // Lifestyle professions were removed from the active runtime. The field and
        // accessors remain as a compatibility facade for legacy source only; the
        // live professions.yml file is migrated to archive/legacy instead of loaded.
        this.itemsConfig = loadManagedConfig("items.yml");
        this.equipmentGrowthConfig = loadManagedConfig("equipment-growth.yml");
        this.enchantsConfig = loadManagedConfig("enchants.yml");
        this.equipmentInputsConfig = loadManagedConfig("equipment-inputs.yml");
        this.craftingConfig = loadManagedConfig("crafting.yml");
        this.mythicMobsConfig = loadManagedConfig("mythic-mobs.yml");
        this.bossesConfig = loadManagedConfig("bosses.yml");
        this.gatewayBossConfig = loadManagedConfig("gateway-boss.yml");
        this.monsterSpawnsConfig = loadManagedConfig("monster-spawns.yml");
        this.specialEquipmentConfig = loadManagedConfig("special-equipment.yml");
        this.farmingCropsConfig = loadManagedConfig("farming/crops.yml");
        this.farmingGrowthConfig = loadManagedConfig("farming/growth.yml");
        this.farmingHarvestConfig = loadManagedConfig("farming/harvest.yml");
        this.farmingProgressionConfig = loadManagedConfig("farming/progression.yml");
        this.farmingQualityConfig = loadManagedConfig("farming/quality.yml");
        this.farmingHoeEnhancementConfig = loadManagedConfig("farming/hoe_enhancement.yml");
        this.farmingHoePromotionConfig = loadManagedConfig("farming/hoe_promotion.yml");
        this.farmingCookingConfig = loadManagedConfig("farming/cooking.yml");
        this.farmingDeliveriesConfig = loadManagedConfig("farming/deliveries.yml");
        this.farmingFavorConfig = loadManagedConfig("farming/favor.yml");
        this.farmingEssenceConfig = loadManagedConfig("farming/essence.yml");
        this.farmingStatTokensConfig = loadManagedConfig("farming/stat_tokens.yml");
        this.alchemyEffectsConfig = loadManagedConfig("alchemy/effects.yml");
        this.alchemyComponentsConfig = loadManagedConfig("alchemy/components.yml");
        this.alchemyConflictsConfig = loadManagedConfig("alchemy/conflicts.yml");
        this.alchemyScalingConfig = loadManagedConfig("alchemy/scaling.yml");
        this.alchemyAbundanceConfig = loadManagedConfig("alchemy/abundance.yml");
        this.alchemyPotionsConfig = loadManagedConfig("alchemy/potions.yml");
        this.alchemyRecipesConfig = loadManagedConfig("alchemy/recipes.yml");
        this.alchemyCatalystsConfig = loadManagedConfig("alchemy/catalysts.yml");
        this.alchemyGuiConfig = loadManagedConfig("alchemy/gui.yml");
        // Existing server files are authoritative. Missing defaults are handled by
        // the explicit /rpg migrate command, never during ordinary startup/reload.
    }

    public double getDouble(String path, double defaultValue) {
        return plugin.getConfig().getDouble(path, defaultValue);
    }

    public long getLong(String path, long defaultValue) {
        return plugin.getConfig().getLong(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return plugin.getConfig().getBoolean(path, defaultValue);
    }

    public String getString(String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }

    public List<String> getStringList(String path) {
        return plugin.getConfig().getStringList(path);
    }

    public void setValue(String path, Object value) {
        plugin.getConfig().set(path, value);
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public int getStatsInt(String path, int defaultValue) {
        return statsConfig.getInt(path, defaultValue);
    }

    public double getStatsDouble(String path, double defaultValue) {
        return statsConfig.getDouble(path, defaultValue);
    }

    public int getExpInt(String path, int defaultValue) {
        return expConfig.getInt(path, defaultValue);
    }

    public long getExpLong(String path, long defaultValue) {
        return expConfig.getLong(path, defaultValue);
    }

    public double getExpDouble(String path, double defaultValue) {
        return expConfig.getDouble(path, defaultValue);
    }

    public boolean getExpBoolean(String path, boolean defaultValue) {
        return expConfig.getBoolean(path, defaultValue);
    }

    public int getMobsInt(String path, int defaultValue) {
        return mobsConfig.getInt(path, defaultValue);
    }

    public double getMobsDouble(String path, double defaultValue) {
        return mobsConfig.getDouble(path, defaultValue);
    }

    public boolean getMobsBoolean(String path, boolean defaultValue) {
        return mobsConfig.getBoolean(path, defaultValue);
    }

    public int getClassesInt(String path, int defaultValue) {
        return classesConfig.getInt(path, defaultValue);
    }

    public double getClassesDouble(String path, double defaultValue) {
        return classesConfig.getDouble(path, defaultValue);
    }

    public String getClassesString(String path, String defaultValue) {
        return classesConfig.getString(path, defaultValue);
    }

    public Set<String> getClassesKeys(String path) {
        ConfigurationSection section = classesConfig.getConfigurationSection(path);
        return section == null ? Set.of() : section.getKeys(false);
    }

    public ConfigurationSection getClassesSection(String path) {
        return classesConfig.getConfigurationSection(path);
    }

    public void setStatsValue(String path, Object value) {
        statsConfig.set(path, value);
    }

    public void saveStatsConfig() {
        try {
            statsConfig.save(new File(plugin.getDataFolder(), "stats.yml"));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", exception);
        }
    }

    public String getMobsString(String path, String defaultValue) {
        return mobsConfig.getString(path, defaultValue);
    }

    public List<String> getMobsStringList(String path) {
        return mobsConfig.getStringList(path);
    }

    public Set<String> getMobsKeys(String path) {
        if (mobsConfig.getConfigurationSection(path) == null) {
            return Set.of();
        }
        return mobsConfig.getConfigurationSection(path).getKeys(false);
    }

    public ConfigurationSection getMobsSection(String path) {
        return mobsConfig.getConfigurationSection(path);
    }

    public boolean getMythicMobsBoolean(String path, boolean defaultValue) {
        return mythicMobsConfig.getBoolean(path, defaultValue);
    }

    public int getMythicMobsInt(String path, int defaultValue) {
        return mythicMobsConfig.getInt(path, defaultValue);
    }

    public long getMythicMobsLong(String path, long defaultValue) {
        return mythicMobsConfig.getLong(path, defaultValue);
    }

    public double getMythicMobsDouble(String path, double defaultValue) {
        return mythicMobsConfig.getDouble(path, defaultValue);
    }

    public String getMythicMobsString(String path, String defaultValue) {
        return mythicMobsConfig.getString(path, defaultValue);
    }

    public List<String> getMythicMobsStringList(String path) {
        return mythicMobsConfig.getStringList(path);
    }

    public Set<String> getMythicMobsKeys(String path) {
        ConfigurationSection section = mythicMobsConfig.getConfigurationSection(path);
        return section == null ? Set.of() : section.getKeys(false);
    }

    public ConfigurationSection getMythicMobsSection(String path) {
        return mythicMobsConfig.getConfigurationSection(path);
    }

    public void reloadMythicMobsConfig() {
        this.mythicMobsConfig = loadManagedConfig("mythic-mobs.yml");
    }

    public void reloadMobsConfig() {
        this.mobsConfig = loadManagedConfig("mobs.yml");
    }

    public void reloadItemsConfig() {
        this.itemsConfig = loadManagedConfig("items.yml");
    }

    public boolean getMonsterSpawnsBoolean(String path, boolean defaultValue) {
        return monsterSpawnsConfig.getBoolean(path, defaultValue);
    }

    public int getMonsterSpawnsInt(String path, int defaultValue) {
        return monsterSpawnsConfig.getInt(path, defaultValue);
    }

    public long getMonsterSpawnsLong(String path, long defaultValue) {
        return monsterSpawnsConfig.getLong(path, defaultValue);
    }

    public double getMonsterSpawnsDouble(String path, double defaultValue) {
        return monsterSpawnsConfig.getDouble(path, defaultValue);
    }

    public Set<String> getMonsterSpawnsKeys(String path) {
        ConfigurationSection section = monsterSpawnsConfig.getConfigurationSection(path);
        return section == null ? Set.of() : section.getKeys(false);
    }

    public ConfigurationSection getMonsterSpawnsSection(String path) {
        return monsterSpawnsConfig.getConfigurationSection(path);
    }

    public void reloadMonsterSpawnsConfig() {
        this.monsterSpawnsConfig = loadManagedConfig("monster-spawns.yml");
    }

    public boolean getBossesBoolean(String path, boolean defaultValue) {
        return bossesConfig.getBoolean(path, defaultValue);
    }

    public int getBossesInt(String path, int defaultValue) {
        return bossesConfig.getInt(path, defaultValue);
    }

    public long getBossesLong(String path, long defaultValue) {
        return bossesConfig.getLong(path, defaultValue);
    }

    public double getBossesDouble(String path, double defaultValue) {
        return bossesConfig.getDouble(path, defaultValue);
    }

    public String getBossesString(String path, String defaultValue) {
        return bossesConfig.getString(path, defaultValue);
    }

    public ConfigurationSection getBossesSection(String path) {
        return bossesConfig.getConfigurationSection(path);
    }

    public java.util.Set<String> getBossesKeys(String path) {
        ConfigurationSection section = getBossesSection(path);
        return section == null ? java.util.Set.of() : section.getKeys(false);
    }

    public void reloadBossesConfig() {
        this.bossesConfig = loadManagedConfig("bosses.yml");
        this.gatewayBossConfig = loadManagedConfig("gateway-boss.yml");
    }

    /** Dedicated Gateway Boss combat tuning; kept apart from ordinary boss-session rewards. */
    public boolean getGatewayBossBoolean(String path, boolean defaultValue) {
        return gatewayBossConfig.getBoolean(path, defaultValue);
    }

    public int getGatewayBossInt(String path, int defaultValue) {
        return gatewayBossConfig.getInt(path, defaultValue);
    }

    public double getGatewayBossDouble(String path, double defaultValue) {
        return gatewayBossConfig.getDouble(path, defaultValue);
    }

    public Set<String> getQuestsKeys(String path) {
        if (questsConfig.getConfigurationSection(path) == null) {
            return Set.of();
        }
        return questsConfig.getConfigurationSection(path).getKeys(false);
    }

    public boolean getQuestsBoolean(String path, boolean defaultValue) {
        return questsConfig.getBoolean(path, defaultValue);
    }

    public int getQuestsInt(String path, int defaultValue) {
        return questsConfig.getInt(path, defaultValue);
    }

    public long getQuestsLong(String path, long defaultValue) {
        return questsConfig.getLong(path, defaultValue);
    }

    public String getQuestsString(String path, String defaultValue) {
        return questsConfig.getString(path, defaultValue);
    }

    public List<String> getQuestsStringList(String path) {
        return questsConfig.getStringList(path);
    }

    public void reloadQuestsConfig() {
        this.questsConfig = loadManagedConfig("quests.yml");
    }

    public ConfigurationSection getQuestsSection(String path) {
        return questsConfig.getConfigurationSection(path);
    }

    public Set<String> getSkillsKeys(String path) {
        return getKeys(skillsConfig, path);
    }

    public ConfigurationSection getSkillsSection(String path) {
        return skillsConfig.getConfigurationSection(path);
    }

    public String getSkillsString(String path, String defaultValue) {
        return skillsConfig.getString(path, defaultValue);
    }

    public int getSkillsInt(String path, int defaultValue) {
        return skillsConfig.getInt(path, defaultValue);
    }

    public double getSkillsDouble(String path, double defaultValue) {
        return skillsConfig.getDouble(path, defaultValue);
    }

    public Set<String> getWeaponsKeys(String path) {
        return getKeys(weaponsConfig, path);
    }

    public ConfigurationSection getWeaponsSection(String path) {
        return weaponsConfig.getConfigurationSection(path);
    }

    public int getWeaponsInt(String path, int defaultValue) {
        return weaponsConfig.getInt(path, defaultValue);
    }

    public double getWeaponsDouble(String path, double defaultValue) {
        return weaponsConfig.getDouble(path, defaultValue);
    }

    public String getWeaponsString(String path, String defaultValue) {
        return weaponsConfig.getString(path, defaultValue);
    }

    public String getCraftingString(String path, String defaultValue) {
        return craftingConfig.getString(path, defaultValue);
    }

    public boolean getCraftingBoolean(String path, boolean defaultValue) {
        return craftingConfig.getBoolean(path, defaultValue);
    }

    public int getCraftingInt(String path, int defaultValue) {
        return craftingConfig.getInt(path, defaultValue);
    }

    public ConfigurationSection getCraftingSection(String path) {
        return craftingConfig.getConfigurationSection(path);
    }

    public List<String> getCraftingStringList(String path) {
        return craftingConfig.getStringList(path);
    }

    public Set<String> getCraftingKeys(String path) {
        return getKeys(craftingConfig, path);
    }

    public void setCraftingValue(String path, Object value) {
        craftingConfig.set(path, value);
    }

    public boolean saveCraftingConfig() {
        try {
            craftingConfig.save(new File(plugin.getDataFolder(), "crafting.yml"));
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save crafting.yml", exception);
            return false;
        }
    }

    /** Writes a validated layout atomically without mutating the live configuration on failure. */
    public boolean replaceCraftingLayout(Map<String, Map<String, Integer>> layouts) {
        File target = new File(plugin.getDataFolder(), "crafting.yml");
        File temporary = new File(plugin.getDataFolder(), "crafting.yml.tmp");
        YamlConfiguration candidate = YamlConfiguration.loadConfiguration(target);
        candidate.set("crafting.layout", null);
        for (Map.Entry<String, Map<String, Integer>> category : layouts.entrySet()) {
            String root = "crafting.layout." + category.getKey();
            for (Map.Entry<String, Integer> entry : category.getValue().entrySet()) {
                candidate.set(root + "." + entry.getKey(), entry.getValue());
            }
        }
        try {
            candidate.save(temporary);
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            this.craftingConfig = candidate;
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to atomically save crafting.yml", exception);
            if (temporary.exists() && !temporary.delete()) {
                plugin.getLogger().fine("Unable to remove temporary crafting config: " + temporary);
            }
            return false;
        }
    }

    public void reloadCraftingConfig() {
        this.craftingConfig = loadManagedConfig("crafting.yml");
    }



    public Set<String> getItemsKeys(String path) {
        return getKeys(itemsConfig, path);
    }

    public String getItemsString(String path, String defaultValue) {
        return itemsConfig.getString(path, defaultValue);
    }

    public List<String> getItemsStringList(String path) {
        return itemsConfig.getStringList(path);
    }

    public int getItemsInt(String path, int defaultValue) {
        return itemsConfig.getInt(path, defaultValue);
    }

    public boolean getItemsBoolean(String path, boolean defaultValue) {
        return itemsConfig.getBoolean(path, defaultValue);
    }

    public double getItemsDouble(String path, double defaultValue) {
        return itemsConfig.getDouble(path, defaultValue);
    }

    public Set<String> getEquipmentGrowthKeys(String path) {
        return getKeys(equipmentGrowthConfig, path);
    }

    public ConfigurationSection getEquipmentGrowthSection(String path) {
        return equipmentGrowthConfig.getConfigurationSection(path);
    }

    public String getEquipmentGrowthString(String path, String defaultValue) {
        return equipmentGrowthConfig.getString(path, defaultValue);
    }

    public int getEquipmentGrowthInt(String path, int defaultValue) {
        return equipmentGrowthConfig.getInt(path, defaultValue);
    }

    public double getEquipmentGrowthDouble(String path, double defaultValue) {
        return equipmentGrowthConfig.getDouble(path, defaultValue);
    }

    public boolean getEquipmentGrowthBoolean(String path, boolean defaultValue) {
        return equipmentGrowthConfig.getBoolean(path, defaultValue);
    }

    public List<String> getEquipmentGrowthStringList(String path) {
        return equipmentGrowthConfig.getStringList(path);
    }

    public void reloadEquipmentGrowthConfig() {
        this.equipmentGrowthConfig = loadManagedConfig("equipment-growth.yml");
    }

    public void reloadEnchantsConfig() {
        this.enchantsConfig = loadManagedConfig("enchants.yml");
    }

    public Set<String> getSpecialEquipmentKeys(String path) {
        return getKeys(specialEquipmentConfig, path);
    }

    public ConfigurationSection getSpecialEquipmentSection(String path) {
        return specialEquipmentConfig.getConfigurationSection(path);
    }

    public String getSpecialEquipmentString(String path, String defaultValue) {
        return specialEquipmentConfig.getString(path, defaultValue);
    }

    public int getSpecialEquipmentInt(String path, int defaultValue) {
        return specialEquipmentConfig.getInt(path, defaultValue);
    }

    public double getSpecialEquipmentDouble(String path, double defaultValue) {
        return specialEquipmentConfig.getDouble(path, defaultValue);
    }

    public boolean getSpecialEquipmentBoolean(String path, boolean defaultValue) {
        return specialEquipmentConfig.getBoolean(path, defaultValue);
    }

    public List<String> getSpecialEquipmentStringList(String path) {
        return specialEquipmentConfig.getStringList(path);
    }

    public void reloadSpecialEquipmentConfig() {
        this.specialEquipmentConfig = loadManagedConfig("special-equipment.yml");
    }

    public ConfigurationSection getFarmingCropsSection(String path) {
        return farmingCropsConfig.getConfigurationSection(path);
    }

    public String getFarmingCropsString(String path, String defaultValue) {
        return farmingCropsConfig.getString(path, defaultValue);
    }

    public int getFarmingCropsInt(String path, int defaultValue) {
        return farmingCropsConfig.getInt(path, defaultValue);
    }

    public boolean getFarmingCropsBoolean(String path, boolean defaultValue) {
        return farmingCropsConfig.getBoolean(path, defaultValue);
    }

    public void reloadFarmingCropsConfig() {
        this.farmingCropsConfig = loadManagedConfig("farming/crops.yml");
    }

    public ConfigurationSection getFarmingGrowthSection(String path) {
        return farmingGrowthConfig.getConfigurationSection(path);
    }

    public long getFarmingGrowthLong(String path, long defaultValue) {
        return farmingGrowthConfig.getLong(path, defaultValue);
    }

    public boolean getFarmingGrowthBoolean(String path, boolean defaultValue) {
        return farmingGrowthConfig.getBoolean(path, defaultValue);
    }

    public void reloadFarmingGrowthConfig() {
        this.farmingGrowthConfig = loadManagedConfig("farming/growth.yml");
    }

    public int getFarmingHarvestInt(String path, int defaultValue) {
        return farmingHarvestConfig.getInt(path, defaultValue);
    }

    public long getFarmingHarvestLong(String path, long defaultValue) {
        return farmingHarvestConfig.getLong(path, defaultValue);
    }

    public double getFarmingHarvestDouble(String path, double defaultValue) {
        return farmingHarvestConfig.getDouble(path, defaultValue);
    }

    public boolean getFarmingHarvestBoolean(String path, boolean defaultValue) {
        return farmingHarvestConfig.getBoolean(path, defaultValue);
    }

    public void reloadFarmingHarvestConfig() {
        this.farmingHarvestConfig = loadManagedConfig("farming/harvest.yml");
    }

    public String getFarmingProgressionString(String path, String defaultValue) {
        return farmingProgressionConfig.getString(path, defaultValue);
    }

    public int getFarmingProgressionInt(String path, int defaultValue) {
        return farmingProgressionConfig.getInt(path, defaultValue);
    }

    public long getFarmingProgressionLong(String path, long defaultValue) {
        return farmingProgressionConfig.getLong(path, defaultValue);
    }

    public double getFarmingProgressionDouble(String path, double defaultValue) {
        return farmingProgressionConfig.getDouble(path, defaultValue);
    }

    public boolean getFarmingProgressionBoolean(String path, boolean defaultValue) {
        return farmingProgressionConfig.getBoolean(path, defaultValue);
    }

    public ConfigurationSection getFarmingProgressionSection(String path) {
        return farmingProgressionConfig.getConfigurationSection(path);
    }

    public void reloadFarmingProgressionConfig() {
        this.farmingProgressionConfig = loadManagedConfig("farming/progression.yml");
    }

    public Set<String> getFarmingQualityKeys(String path) {
        return getKeys(farmingQualityConfig, path);
    }

    public ConfigurationSection getFarmingQualitySection(String path) {
        return farmingQualityConfig.getConfigurationSection(path);
    }

    public String getFarmingQualityString(String path, String defaultValue) {
        return farmingQualityConfig.getString(path, defaultValue);
    }

    public double getFarmingQualityDouble(String path, double defaultValue) {
        return farmingQualityConfig.getDouble(path, defaultValue);
    }

    public boolean getFarmingQualityBoolean(String path, boolean defaultValue) {
        return farmingQualityConfig.getBoolean(path, defaultValue);
    }

    public void reloadFarmingQualityConfig() {
        this.farmingQualityConfig = loadManagedConfig("farming/quality.yml");
    }

    public Set<String> getFarmingCookingKeys(String path) {
        return getKeys(farmingCookingConfig, path);
    }

    public ConfigurationSection getFarmingCookingSection(String path) {
        return farmingCookingConfig.getConfigurationSection(path);
    }

    public String getFarmingCookingString(String path, String defaultValue) {
        return farmingCookingConfig.getString(path, defaultValue);
    }

    public int getFarmingCookingInt(String path, int defaultValue) {
        return farmingCookingConfig.getInt(path, defaultValue);
    }

    public boolean getFarmingCookingBoolean(String path, boolean defaultValue) {
        return farmingCookingConfig.getBoolean(path, defaultValue);
    }

    public List<String> getFarmingCookingStringList(String path) {
        return farmingCookingConfig.getStringList(path);
    }

    public void reloadFarmingCookingConfig() {
        this.farmingCookingConfig = loadManagedConfig("farming/cooking.yml");
    }

    public Set<String> getFarmingDeliveriesKeys(String path) {
        return getKeys(farmingDeliveriesConfig, path);
    }

    public ConfigurationSection getFarmingDeliveriesSection(String path) {
        return farmingDeliveriesConfig.getConfigurationSection(path);
    }

    public long getFarmingDeliveriesLong(String path, long defaultValue) {
        return farmingDeliveriesConfig.getLong(path, defaultValue);
    }

    public boolean getFarmingDeliveriesBoolean(String path, boolean defaultValue) {
        return farmingDeliveriesConfig.getBoolean(path, defaultValue);
    }

    public int getFarmingDeliveriesInt(String path, int defaultValue) {
        return farmingDeliveriesConfig.getInt(path, defaultValue);
    }

    public double getFarmingDeliveriesDouble(String path, double defaultValue) {
        return farmingDeliveriesConfig.getDouble(path, defaultValue);
    }

    public String getFarmingDeliveriesString(String path, String defaultValue) {
        return farmingDeliveriesConfig.getString(path, defaultValue);
    }

    public void reloadFarmingDeliveriesConfig() {
        this.farmingDeliveriesConfig = loadManagedConfig("farming/deliveries.yml");
    }

    public boolean getFarmingFavorBoolean(String path, boolean defaultValue) {
        return farmingFavorConfig.getBoolean(path, defaultValue);
    }

    public long getFarmingFavorLong(String path, long defaultValue) {
        return farmingFavorConfig.getLong(path, defaultValue);
    }

    public double getFarmingFavorDouble(String path, double defaultValue) {
        return farmingFavorConfig.getDouble(path, defaultValue);
    }

    public String getFarmingFavorString(String path, String defaultValue) {
        return farmingFavorConfig.getString(path, defaultValue);
    }

    public Set<String> getFarmingFavorKeys(String path) {
        return getKeys(farmingFavorConfig, path);
    }

    public ConfigurationSection getFarmingFavorSection(String path) {
        return farmingFavorConfig.getConfigurationSection(path);
    }

    public void reloadFarmingFavorConfig() {
        this.farmingFavorConfig = loadManagedConfig("farming/favor.yml");
    }

    public boolean getFarmingEssenceBoolean(String path, boolean defaultValue) {
        return farmingEssenceConfig.getBoolean(path, defaultValue);
    }

    public int getFarmingEssenceInt(String path, int defaultValue) {
        return farmingEssenceConfig.getInt(path, defaultValue);
    }

    public long getFarmingEssenceLong(String path, long defaultValue) {
        return farmingEssenceConfig.getLong(path, defaultValue);
    }

    public String getFarmingEssenceString(String path, String defaultValue) {
        return farmingEssenceConfig.getString(path, defaultValue);
    }

    public List<String> getFarmingEssenceStringList(String path) {
        return List.copyOf(farmingEssenceConfig.getStringList(path));
    }

    public Set<String> getFarmingEssenceKeys(String path) {
        return getKeys(farmingEssenceConfig, path);
    }

    public ConfigurationSection getFarmingEssenceSection(String path) {
        return farmingEssenceConfig.getConfigurationSection(path);
    }

    public void reloadFarmingEssenceConfig() {
        this.farmingEssenceConfig = loadManagedConfig("farming/essence.yml");
    }

    public boolean getFarmingStatTokensBoolean(String path, boolean defaultValue) {
        return farmingStatTokensConfig.getBoolean(path, defaultValue);
    }

    public int getFarmingStatTokensInt(String path, int defaultValue) {
        return farmingStatTokensConfig.getInt(path, defaultValue);
    }

    public double getFarmingStatTokensDouble(String path, double defaultValue) {
        return farmingStatTokensConfig.getDouble(path, defaultValue);
    }

    public String getFarmingStatTokensString(String path, String defaultValue) {
        return farmingStatTokensConfig.getString(path, defaultValue);
    }

    public Set<String> getFarmingStatTokensKeys(String path) {
        return getKeys(farmingStatTokensConfig, path);
    }

    public ConfigurationSection getFarmingStatTokensSection(String path) {
        return farmingStatTokensConfig.getConfigurationSection(path);
    }

    public void reloadFarmingStatTokensConfig() {
        this.farmingStatTokensConfig = loadManagedConfig("farming/stat_tokens.yml");
    }

    public ConfigurationSection getAlchemyEffectsSection(String path) {
        return alchemyEffectsConfig.getConfigurationSection(path);
    }

    public Set<String> getAlchemyEffectsKeys(String path) {
        return getKeys(alchemyEffectsConfig, path);
    }

    public String getAlchemyEffectsString(String path, String defaultValue) {
        return alchemyEffectsConfig.getString(path, defaultValue);
    }

    public int getAlchemyEffectsInt(String path, int defaultValue) {
        return alchemyEffectsConfig.getInt(path, defaultValue);
    }

    public boolean getAlchemyEffectsBoolean(String path, boolean defaultValue) {
        return alchemyEffectsConfig.getBoolean(path, defaultValue);
    }

    public List<Map<?, ?>> getAlchemyEffectsMapList(String path) {
        return List.copyOf(alchemyEffectsConfig.getMapList(path));
    }

    public ConfigurationSection getAlchemyComponentsSection(String path) {
        return alchemyComponentsConfig.getConfigurationSection(path);
    }

    public Set<String> getAlchemyComponentsKeys(String path) {
        return getKeys(alchemyComponentsConfig, path);
    }

    public boolean getAlchemyComponentsBoolean(String path, boolean defaultValue) {
        return alchemyComponentsConfig.getBoolean(path, defaultValue);
    }

    public ConfigurationSection getAlchemyConflictsSection(String path) {
        return alchemyConflictsConfig.getConfigurationSection(path);
    }

    public Set<String> getAlchemyConflictsKeys(String path) {
        return getKeys(alchemyConflictsConfig, path);
    }

    public int getAlchemyConflictsInt(String path, int defaultValue) {
        return alchemyConflictsConfig.getInt(path, defaultValue);
    }

    public List<String> getAlchemyConflictsStringList(String path) {
        return List.copyOf(alchemyConflictsConfig.getStringList(path));
    }

    public double getAlchemyScalingDouble(String path, double defaultValue) {
        return alchemyScalingConfig.getDouble(path, defaultValue);
    }

    public int getAlchemyScalingInt(String path, int defaultValue) {
        return alchemyScalingConfig.getInt(path, defaultValue);
    }

    public void reloadAlchemyEffectsConfigs() {
        this.alchemyEffectsConfig = loadManagedConfig("alchemy/effects.yml");
        this.alchemyComponentsConfig = loadManagedConfig("alchemy/components.yml");
        this.alchemyConflictsConfig = loadManagedConfig("alchemy/conflicts.yml");
        this.alchemyScalingConfig = loadManagedConfig("alchemy/scaling.yml");
        this.alchemyAbundanceConfig = loadManagedConfig("alchemy/abundance.yml");
        this.alchemyPotionsConfig = loadManagedConfig("alchemy/potions.yml");
        this.alchemyRecipesConfig = loadManagedConfig("alchemy/recipes.yml");
        this.alchemyCatalystsConfig = loadManagedConfig("alchemy/catalysts.yml");
        this.alchemyGuiConfig = loadManagedConfig("alchemy/gui.yml");
    }

    public ConfigurationSection getAlchemyAbundanceSection(String path) { return alchemyAbundanceConfig.getConfigurationSection(path); }
    public Set<String> getAlchemyAbundanceKeys(String path) { return getKeys(alchemyAbundanceConfig, path); }
    public String getAlchemyAbundanceString(String path, String fallback) { return alchemyAbundanceConfig.getString(path, fallback); }
    public boolean getAlchemyAbundanceBoolean(String path, boolean fallback) { return alchemyAbundanceConfig.getBoolean(path, fallback); }
    public int getAlchemyAbundanceInt(String path, int fallback) { return alchemyAbundanceConfig.getInt(path, fallback); }

    public ConfigurationSection getAlchemyPotionsSection(String path) { return alchemyPotionsConfig.getConfigurationSection(path); }
    public Set<String> getAlchemyPotionKeys(String path) { return getKeys(alchemyPotionsConfig, path); }
    public String getAlchemyPotionString(String path, String fallback) { return alchemyPotionsConfig.getString(path, fallback); }
    public boolean getAlchemyPotionBoolean(String path, boolean fallback) { return alchemyPotionsConfig.getBoolean(path, fallback); }

    public ConfigurationSection getAlchemyRecipesSection(String path) { return alchemyRecipesConfig.getConfigurationSection(path); }
    public Set<String> getAlchemyRecipeKeys(String path) { return getKeys(alchemyRecipesConfig, path); }
    public String getAlchemyRecipeString(String path, String fallback) { return alchemyRecipesConfig.getString(path, fallback); }
    public boolean getAlchemyRecipeBoolean(String path, boolean fallback) { return alchemyRecipesConfig.getBoolean(path, fallback); }
    public List<String> getAlchemyRecipeStringList(String path) { return List.copyOf(alchemyRecipesConfig.getStringList(path)); }

    public ConfigurationSection getAlchemyCatalystsSection(String path) { return alchemyCatalystsConfig.getConfigurationSection(path); }
    public Set<String> getAlchemyCatalystKeys(String path) { return getKeys(alchemyCatalystsConfig, path); }
    public String getAlchemyCatalystString(String path, String fallback) { return alchemyCatalystsConfig.getString(path, fallback); }
    public boolean getAlchemyCatalystBoolean(String path, boolean fallback) { return alchemyCatalystsConfig.getBoolean(path, fallback); }
    public int getAlchemyCatalystInt(String path, int fallback) { return alchemyCatalystsConfig.getInt(path, fallback); }
    public double getAlchemyCatalystDouble(String path, double fallback) { return alchemyCatalystsConfig.getDouble(path, fallback); }
    public List<String> getAlchemyCatalystStringList(String path) { return List.copyOf(alchemyCatalystsConfig.getStringList(path)); }

    /** Migrates the known legacy sculk material in the live external config without restoring deleted keys. */
    public synchronized String normalizeAlchemyCatalystMaterial(String catalystId, String canonicalMaterial) {
        String path = "catalysts." + catalystId + ".vanilla-material";
        String current = alchemyCatalystsConfig.getString(path, canonicalMaterial);
        if (!"SCULK".equalsIgnoreCase(current)) return current;
        alchemyCatalystsConfig.set(path, canonicalMaterial);
        File target = new File(plugin.getDataFolder(), "alchemy/catalysts.yml");
        File temporary = new File(plugin.getDataFolder(), "alchemy/catalysts.yml.tmp");
        try {
            temporary.getParentFile().mkdirs();
            alchemyCatalystsConfig.save(temporary);
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to persist legacy alchemy catalyst migration for " + catalystId, exception);
        }
        return canonicalMaterial;
    }

    public ConfigurationSection getAlchemyGuiSection(String path) { return alchemyGuiConfig.getConfigurationSection(path); }
    public boolean getAlchemyGuiBoolean(String path, boolean fallback) { return alchemyGuiConfig.getBoolean(path, fallback); }

    public double getFarmingHoeEnhancementDouble(String path, double defaultValue) {
        return farmingHoeEnhancementConfig.getDouble(path, defaultValue);
    }

    public int getFarmingHoeEnhancementInt(String path, int defaultValue) {
        return farmingHoeEnhancementConfig.getInt(path, defaultValue);
    }

    public boolean getFarmingHoeEnhancementBoolean(String path, boolean defaultValue) {
        return farmingHoeEnhancementConfig.getBoolean(path, defaultValue);
    }

    public Set<String> getFarmingHoeEnhancementKeys(String path) {
        return getKeys(farmingHoeEnhancementConfig, path);
    }

    public void reloadFarmingHoeEnhancementConfig() {
        this.farmingHoeEnhancementConfig = loadManagedConfig("farming/hoe_enhancement.yml");
    }

    public double getFarmingHoePromotionDouble(String path, double defaultValue) {
        return farmingHoePromotionConfig.getDouble(path, defaultValue);
    }

    public int getFarmingHoePromotionInt(String path, int defaultValue) {
        return farmingHoePromotionConfig.getInt(path, defaultValue);
    }

    public String getFarmingHoePromotionString(String path, String defaultValue) {
        return farmingHoePromotionConfig.getString(path, defaultValue);
    }

    public boolean getFarmingHoePromotionBoolean(String path, boolean defaultValue) {
        return farmingHoePromotionConfig.getBoolean(path, defaultValue);
    }

    public List<String> getFarmingHoePromotionStringList(String path) {
        return farmingHoePromotionConfig.getStringList(path);
    }

    public Set<String> getFarmingHoePromotionKeys(String path) {
        return getKeys(farmingHoePromotionConfig, path);
    }

    public void reloadFarmingHoePromotionConfig() {
        this.farmingHoePromotionConfig = loadManagedConfig("farming/hoe_promotion.yml");
    }

    public Set<String> getEnchantsKeys(String path) {
        return getKeys(enchantsConfig, path);
    }

    public ConfigurationSection getEnchantsSection(String path) {
        return enchantsConfig.getConfigurationSection(path);
    }

    public String getEnchantsString(String path, String defaultValue) {
        return enchantsConfig.getString(path, defaultValue);
    }

    public int getEnchantsInt(String path, int defaultValue) {
        return enchantsConfig.getInt(path, defaultValue);
    }

    public double getEnchantsDouble(String path, double defaultValue) {
        return enchantsConfig.getDouble(path, defaultValue);
    }

    public boolean getEnchantsBoolean(String path, boolean defaultValue) {
        return enchantsConfig.getBoolean(path, defaultValue);
    }

    public List<String> getEnchantsStringList(String path) {
        return enchantsConfig.getStringList(path);
    }

    public Set<String> getEquipmentInputsKeys(String path) {
        return getKeys(equipmentInputsConfig, path);
    }

    public String getEquipmentInputsString(String path, String defaultValue) {
        return equipmentInputsConfig.getString(path, defaultValue);
    }

    public void setMobsValue(String path, Object value) {
        mobsConfig.set(path, value);
    }

    public void saveMobsConfig() {
        try {
            mobsConfig.save(new File(plugin.getDataFolder(), "mobs.yml"));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save mobs.yml", exception);
        }
    }

    private FileConfiguration loadManagedConfig(String fileName) {
        File targetFile = new File(plugin.getDataFolder(), fileName);
        if (!targetFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        // Existing files are authoritative. Reapplying resource defaults on every reload
        // makes deleted YAML keys reappear and can silently undo server administration.
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(targetFile);
        return configuration;
    }

    private void migrateElementDropTables(FileConfiguration configuration) {
        if (configuration.getInt("loot-crafting-schema-version", 0) >= 1) return;

        setElementDropTable(configuration, "water_common", "water_fragment", "PRISMARINE_SHARD", "물의 파편");
        setElementDropTable(configuration, "ice_common", "ice_fragment", "SNOWBALL", "얼어붙은 파편");
        setElementDropTable(configuration, "soul_common", "soul_stone", "ECHO_SHARD", "영혼석");
        setElementDropTable(configuration, "earth_common", "earth_fragment", "COBBLESTONE", "땅의 파편");
        setElementDropTable(configuration, "fire_common", "fire_fragment", "MAGMA_CREAM", "불의 파편");
        setElementDropTable(configuration, "wind_common", "wind_fragment", "FEATHER", "돌풍 파편");

        Map<String, String> mobTables = Map.ofEntries(
                Map.entry("frost_blaze", "ice_common"),
                Map.entry("explosive_breeze", "wind_common"),
                Map.entry("riptide_drowned", "water_common"),
                Map.entry("mining_giant", "earth_common"),
                Map.entry("lava_cube", "fire_common"),
                Map.entry("stone_armored_zombie", "soul_common"),
                Map.entry("fake_explosion_creeper", "fire_common"),
                Map.entry("swapping_witch", "soul_common"),
                Map.entry("charging_zombie", "soul_common"),
                Map.entry("rapid_shooter", "soul_common"),
                Map.entry("splitting_creeper", "fire_common"));
        for (Map.Entry<String, String> entry : mobTables.entrySet()) {
            for (String root : List.of("custom-mobs", "mob-definitions")) {
                if (configuration.isSet(root + "." + entry.getKey())) {
                    configuration.set(root + "." + entry.getKey() + ".drop-table", entry.getValue());
                }
            }
        }
        configuration.set("custom-mobs.coward_creeper", null);
        configuration.set("custom-mobs.piglin_spearman", null);
        configuration.set("mob-definitions.coward_creeper", null);
        configuration.set("mob-definitions.piglin_spearman", null);
        configuration.set("loot-crafting-schema-version", 1);
        saveManagedConfig(configuration, "mobs.yml");
        plugin.getLogger().info("Migrated mobs.yml to elemental loot tables.");
    }

    private void migrateMythicElementDrops(FileConfiguration configuration) {
        if (configuration.getInt("loot-crafting-schema-version", 0) >= 1) return;
        configuration.set("mobs.frost_blaze.drops", List.of(Map.of(
                "item-id", "ice_fragment", "chance", 1.0D, "min", 2, "max", 4)));
        configuration.set("mobs.explosive_breeze.drops", List.of(Map.of(
                "item-id", "wind_fragment", "chance", 1.0D, "min", 2, "max", 4)));
        configuration.set("mobs.riptide_drowned.drops", List.of(Map.of(
                "item-id", "water_fragment", "chance", 1.0D, "min", 2, "max", 4)));
        configuration.set("loot-crafting-schema-version", 1);
        saveManagedConfig(configuration, "mythic-mobs.yml");
        plugin.getLogger().info("Migrated MythicMobs custom drops to elemental materials.");
    }

    private void setElementDropTable(FileConfiguration configuration, String tableId, String itemId,
                                     String material, String displayName) {
        String path = "drop-tables." + tableId + ".entries";
        configuration.set(path, List.of(Map.of(
                "item-id", itemId,
                "material", material,
                "display-name", displayName,
                "chance", 1.0D,
                "min-amount", 2,
                "max-amount", 4)));
    }

    private void saveManagedConfig(FileConfiguration configuration, String fileName) {
        try {
            configuration.save(new File(plugin.getDataFolder(), fileName));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate " + fileName, exception);
        }
    }

    private void removeLegacyCustomMob(FileConfiguration configuration, String root, String resourceName) {
        boolean changed = false;
        for (String id : List.of("laser_ghast", "bombing_ghast", "coward_creeper", "piglin_spearman")) {
            String path = root + "." + id;
            if (!configuration.isSet(path)) continue;
            configuration.set(path, null);
            changed = true;
        }
        if (!changed) return;
        try {
            configuration.save(new File(plugin.getDataFolder(), resourceName));
            plugin.getLogger().info("Removed disabled custom mob entries from " + resourceName + ".");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove disabled custom mob entries from " + resourceName, exception);
        }
    }

    private void migrateBossRewards(FileConfiguration configuration) {
        ConfigurationSection sessions = configuration.getConfigurationSection("boss-sessions");
        if (sessions == null) return;
        boolean changed = false;
        for (String bossId : sessions.getKeys(false)) {
            ConfigurationSection boss = sessions.getConfigurationSection(bossId);
            if (boss == null) continue;
            ConfigurationSection rewards = boss.getConfigurationSection("rewards");
            ConfigurationSection first = boss.getConfigurationSection("first-clear-rewards");
            if (rewards == null && first != null) {
                copySection(configuration, first, "boss-sessions." + bossId + ".rewards");
                changed = true;
            }
            if (boss.isSet("first-clear-rewards")) {
                boss.set("first-clear-rewards", null);
                changed = true;
            }
            if (boss.isSet("repeat-rewards")) {
                boss.set("repeat-rewards", null);
                changed = true;
            }
            if (!boss.isSet("respawn-cooldown-seconds")) {
                boss.set("respawn-cooldown-seconds", bossId.equalsIgnoreCase("ender-dragon") ? 1800 : 900);
                changed = true;
            }
        }
        if (!changed) return;
        try {
            configuration.save(new File(plugin.getDataFolder(), "bosses.yml"));
            plugin.getLogger().info("Migrated boss rewards to fixed rewards and added respawn cooldown settings.");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate bosses.yml", exception);
        }
    }

    private void mergeMissingSections(FileConfiguration target, String root, String resourceName, Set<String> ids) {
        if (plugin.getResource(resourceName) == null) return;
        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to read custom mob defaults from " + resourceName, exception);
            return;
        }
        boolean changed = false;
        for (String id : ids) {
            String path = root + "." + id;
            if (target.getConfigurationSection(path) != null) continue;
            ConfigurationSection source = defaults.getConfigurationSection(path);
            if (source == null) continue;
            copySection(target, source, path);
            changed = true;
        }
        if (!changed) return;
        try {
            target.save(new File(plugin.getDataFolder(), resourceName));
            plugin.getLogger().info("Added missing custom mob defaults to " + resourceName + ".");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save custom mob defaults in " + resourceName, exception);
        }
    }

    private void mergeMissingValues(FileConfiguration target, String root, String resourceName, Set<String> keys) {
        if (plugin.getResource(resourceName) == null) return;
        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to read configuration defaults from " + resourceName, exception);
            return;
        }
        boolean changed = false;
        for (String key : keys) {
            String path = root + "." + key;
            if (target.isSet(path) || !defaults.isSet(path)) continue;
            target.set(path, defaults.get(path));
            changed = true;
        }
        if (!changed) return;
        try {
            target.save(new File(plugin.getDataFolder(), resourceName));
            plugin.getLogger().info("Added missing configuration defaults to " + resourceName + ".");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save configuration defaults in " + resourceName, exception);
        }
    }

    private void mergeMissingSectionKeys(FileConfiguration target, String root, String resourceName, Set<String> ids) {
        if (plugin.getResource(resourceName) == null) return;
        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to read nested configuration defaults from " + resourceName, exception);
            return;
        }

        boolean changed = false;
        for (String id : ids) {
            String targetPath = root + "." + id;
            ConfigurationSection source = defaults.getConfigurationSection(targetPath);
            ConfigurationSection existing = target.getConfigurationSection(targetPath);
            if (source == null || existing == null) continue;
            changed |= copyMissingSectionKeys(target, existing, source, targetPath);
        }
        if (!changed) return;
        try {
            target.save(new File(plugin.getDataFolder(), resourceName));
            plugin.getLogger().info("Added missing nested configuration defaults to " + resourceName + ".");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save nested configuration defaults in " + resourceName, exception);
        }
    }

    private boolean copyMissingSectionKeys(FileConfiguration target, ConfigurationSection existing,
                                           ConfigurationSection source, String targetPath) {
        boolean changed = false;
        for (String key : source.getKeys(false)) {
            String path = targetPath + "." + key;
            ConfigurationSection sourceNested = source.getConfigurationSection(key);
            if (sourceNested != null) {
                ConfigurationSection existingNested = existing.getConfigurationSection(key);
                if (existingNested != null) {
                    changed |= copyMissingSectionKeys(target, existingNested, sourceNested, path);
                }
                continue;
            }
            if (!target.isSet(path)) {
                target.set(path, source.get(key));
                changed = true;
            }
        }
        return changed;
    }

    private void copySection(FileConfiguration target, ConfigurationSection source, String targetPath) {
        for (String key : source.getKeys(false)) {
            String path = targetPath + "." + key;
            ConfigurationSection nested = source.getConfigurationSection(key);
            if (nested != null) {
                copySection(target, nested, path);
            } else {
                target.set(path, source.get(key));
            }
        }
    }

    private void mergeDefaultList(FileConfiguration config, String path, String resourceName) {
        if (plugin.getResource(resourceName) == null) return;
        List<String> current = new java.util.ArrayList<>(config.getStringList(path));
        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to read default list from " + resourceName, exception);
            return;
        }
        boolean changed = false;
        for (String value : defaults.getStringList(path)) {
            if (!current.contains(value)) {
                current.add(value);
                changed = true;
            }
        }
        if (!changed) return;
        config.set(path, current);
        try {
            config.save(new File(plugin.getDataFolder(), resourceName));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to merge default list in " + resourceName, exception);
        }
    }

    private Set<String> getKeys(FileConfiguration configuration, String path) {
        ConfigurationSection section = configuration.getConfigurationSection(path);
        return section == null ? Set.of() : section.getKeys(false);
    }
}
