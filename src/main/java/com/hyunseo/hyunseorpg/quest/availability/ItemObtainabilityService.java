package com.hyunseo.hyunseorpg.quest.availability;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeData;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemData;
import com.hyunseo.hyunseorpg.item.RPGItemRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.mob.drop.MobDropEntry;
import com.hyunseo.hyunseorpg.mob.drop.MobDropRegistry;
import com.hyunseo.hyunseorpg.mob.drop.MobDropTable;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Determines whether an item is actually obtainable and safe for an automatic delivery quest. */
public final class ItemObtainabilityService {
    private final ConfigService config;
    private final RPGItemRegistry items;
    private final RPGItemService itemService;
    private final CraftingRecipeRegistry recipes;
    private final MobDropRegistry drops;
    private final PlayerDataService playerData;

    public ItemObtainabilityService(ConfigService config, RPGItemRegistry items, RPGItemService itemService,
                                    CraftingRecipeRegistry recipes, MobDropRegistry drops,
                                    PlayerDataService playerData) {
        this.config = config;
        this.items = items;
        this.itemService = itemService;
        this.recipes = recipes;
        this.drops = drops;
        this.playerData = playerData;
    }

    public ItemObtainabilityResult checkItem(Player player, String rawTarget, ItemObtainabilityContext context) {
        String target = normalize(rawTarget);
        if (target.startsWith("vanilla:")) return checkVanilla(target.substring("vanilla:".length()), context);
        return checkCustom(player, target, context);
    }

    public List<QuestTargetCandidate> eligibleItemTargets(Player player) {
        List<QuestTargetCandidate> candidates = new ArrayList<>();
        for (String rawMaterial : config.getQuestsStringList("auto.eligibility.vanilla-items")) {
            ItemObtainabilityResult result = checkVanilla(rawMaterial, ItemObtainabilityContext.QUEST_TARGET);
            if (result.obtainable()) candidates.add(new QuestTargetCandidate("vanilla:" + result.itemId(),
                    result.displayName(), result.source(), 1, result.detail()));
        }
        for (String itemId : configuredCustomCandidates()) {
            ItemObtainabilityResult result = checkCustom(player, itemId, ItemObtainabilityContext.QUEST_TARGET);
            if (result.obtainable()) candidates.add(new QuestTargetCandidate(result.itemId(), result.displayName(),
                    result.source(), 1, result.detail()));
        }
        return List.copyOf(candidates);
    }

    private ItemObtainabilityResult checkVanilla(String rawMaterial, ItemObtainabilityContext context) {
        String id = normalize(rawMaterial).replace("minecraft:", "");
        Material material = Material.matchMaterial(id.toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            return ItemObtainabilityResult.denied(AvailabilityReason.MISSING_DEFINITION, id, id,
                    QuestTargetSource.VANILLA_ITEM, Set.of(AcquisitionSource.NONE), "unknown vanilla material");
        }
        if (material.getMaxStackSize() == 1 || material.getMaxDurability() > 0) {
            return ItemObtainabilityResult.denied(AvailabilityReason.QUEST_EXCLUDED, id, display(material),
                    QuestTargetSource.VANILLA_ITEM, Set.of(AcquisitionSource.VANILLA), "equipment and tools are excluded");
        }
        if (context == ItemObtainabilityContext.QUEST_TARGET && !configuredVanillaItem(material)) {
            return ItemObtainabilityResult.denied(AvailabilityReason.QUEST_EXCLUDED, id, display(material),
                    QuestTargetSource.VANILLA_ITEM, Set.of(AcquisitionSource.VANILLA),
                    "not in auto.eligibility.vanilla-items");
        }
        return ItemObtainabilityResult.allowed(id, display(material), QuestTargetSource.VANILLA_ITEM,
                Set.of(AcquisitionSource.VANILLA), "configured vanilla material");
    }

    private ItemObtainabilityResult checkCustom(Player player, String id, ItemObtainabilityContext context) {
        RPGItemData item = items.get(id).orElse(null);
        if (item == null) {
            return ItemObtainabilityResult.denied(AvailabilityReason.MISSING_DEFINITION, id, id,
                    QuestTargetSource.HYUNSEORPG_CUSTOM_ITEM, Set.of(AcquisitionSource.NONE), "items.yml definition is missing");
        }
        Set<AcquisitionSource> sources = sourcesFor(id);
        Boolean override = questTargetOverride(id);
        if (Boolean.FALSE.equals(override)) {
            return deny(AvailabilityReason.QUEST_EXCLUDED, item, sources, "quest-target=false override");
        }
        String category = normalize(item.category()).toUpperCase(Locale.ROOT);
        if (context == ItemObtainabilityContext.QUEST_TARGET) {
            if (item.legacyProfessionItem() || category.startsWith("PROFESSION_")) return deny(AvailabilityReason.LEGACY, item, sources, "legacy item");
            if (category.contains("EQUIPMENT") || category.contains("WEAPON") || category.contains("ARMOR")) {
                return deny(AvailabilityReason.SPECIAL_EQUIPMENT, item, sources, "equipment category");
            }
            if (category.contains("BOSS")) return deny(AvailabilityReason.BOSS_MATERIAL, item, sources, "boss material");
            if (category.contains("ENHANCEMENT") || category.contains("ENCHANTMENT") || category.contains("PROMOTION")) {
                return deny(AvailabilityReason.UPGRADE_MATERIAL, item, sources, "growth material");
            }
            if (category.contains("SPECIAL") || category.contains("CONSUMABLE") || category.contains("BUFF")) {
                return deny(AvailabilityReason.SPECIAL_LOOT, item, sources, "special item category");
            }
            if (category.contains("QUEST")) return deny(AvailabilityReason.QUEST_REWARD_ONLY, item, sources, "quest category");
        }
        if (sources.isEmpty()) return deny(AvailabilityReason.NO_ACQUISITION_SOURCE, item, Set.of(AcquisitionSource.NONE), "no active recipe or drop source");
        boolean explicitlyAllowed = Boolean.TRUE.equals(override) || configuredCustomCandidates().stream()
                .anyMatch(value -> value.equalsIgnoreCase(id));
        if (context == ItemObtainabilityContext.QUEST_TARGET && !explicitlyAllowed) {
            return deny(AvailabilityReason.QUEST_EXCLUDED, item, sources, "custom item is not explicitly quest-enabled");
        }
        if (context == ItemObtainabilityContext.QUEST_TARGET
                && config.getQuestsBoolean("auto.eligibility.items.require-discovery", false)
                && player != null
                && !playerDataHasItemDiscovery(player, id)) {
            return deny(AvailabilityReason.NOT_DISCOVERED, item, sources, "item discovery required");
        }
        return ItemObtainabilityResult.allowed(id, item.displayName(), QuestTargetSource.HYUNSEORPG_CUSTOM_ITEM,
                sources, "explicit custom material target");
    }

    private boolean playerDataHasItemDiscovery(Player player, String itemId) {
        return playerData.getOrLoad(player).hasProgressionFlag("discovered_item_" + id(itemId));
    }

    private ItemObtainabilityResult deny(AvailabilityReason reason, RPGItemData item,
                                         Set<AcquisitionSource> sources, String detail) {
        return ItemObtainabilityResult.denied(reason, item.itemId(), item.displayName(),
                QuestTargetSource.HYUNSEORPG_CUSTOM_ITEM, sources, detail);
    }

    private Set<AcquisitionSource> sourcesFor(String itemId) {
        Set<AcquisitionSource> sources = EnumSet.noneOf(AcquisitionSource.class);
        for (CraftingRecipeData recipe : recipes.getAll()) {
            if (recipe.outputId().equalsIgnoreCase(itemId)) sources.add(AcquisitionSource.CRAFTING);
        }
        for (MobDropTable table : drops.getAll()) {
            for (MobDropEntry entry : table.entries()) {
                if (entry.itemId().equalsIgnoreCase(itemId)) sources.add(AcquisitionSource.CUSTOM_MOB_DROP);
            }
        }
        return Set.copyOf(sources);
    }

    private Boolean questTargetOverride(String itemId) {
        var section = config.getQuestsSection("auto.eligibility.items.overrides." + id(itemId));
        if (section == null || !section.isSet("quest-target")) return null;
        return section.getBoolean("quest-target");
    }

    private List<String> configuredCustomCandidates() {
        List<String> configured = config.getQuestsStringList("auto.eligibility.items.custom-item-ids");
        if (!configured.isEmpty()) return configured;
        return config.getQuestsStringList("auto.item.item-ids");
    }

    private boolean configuredVanillaItem(Material material) {
        return config.getQuestsStringList("auto.eligibility.vanilla-items").stream()
                .anyMatch(value -> value.equalsIgnoreCase(material.name()));
    }

    private String display(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String id(String value) {
        return normalize(value).replace("vanilla:", "");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
