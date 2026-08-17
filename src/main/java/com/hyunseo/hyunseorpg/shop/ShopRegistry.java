package com.hyunseo.hyunseorpg.shop;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** Loads and persists data-driven shops. Item prices remain configuration-only. */
public final class ShopRegistry {
    private final ConfigService configService;
    private final RPGItemService itemService;
    private final Logger logger;
    private final Map<String, ShopData> shops = new ConcurrentHashMap<>();

    public ShopRegistry(ConfigService configService, RPGItemService itemService, Logger logger) {
        this.configService = configService;
        this.itemService = itemService;
        this.logger = logger;
    }

    public void load() {
        shops.clear();
        for (String rawShopId : configService.getShopsKeys("shops")) {
            String shopId = normalize(rawShopId);
            String basePath = "shops." + rawShopId;
            String title = configService.getShopsString(basePath + ".title", shopId);
            if (shopId.equals("farming")) title = "농사";
            List<ShopItemData> products = new ArrayList<>();
            for (String rawProductId : configService.getShopsKeys(basePath + ".items")) {
                String productPath = basePath + ".items." + rawProductId;
                String promotionStoneId = configService.getEquipmentGrowthString(
                        "promotion.required-stone-item-id", "basic_promotion_stone");
                String configuredCustomId = configService.getShopsString(productPath + ".item.id", "");
                if (rawProductId.equalsIgnoreCase(promotionStoneId)
                        || configuredCustomId.equalsIgnoreCase(promotionStoneId)) {
                    logger.info("Skipping boss-loop promotion stone shop product: " + shopId + "/" + rawProductId);
                    continue;
                }
                ItemStack template = readTemplate(productPath);
                if (template == null || template.getType().isAir()) {
                    logger.warning("Skipping shop product without a valid item: " + shopId + "/" + rawProductId);
                    continue;
                }
                if (template.getType() == Material.DRAGON_EGG
                        || itemService.isItem(template, "magic_stone")
                        || itemService.isItem(template, "magic_stone_fragment")) {
                    logger.info("Skipping blocked economy product: " + shopId + "/" + rawProductId);
                    continue;
                }
                String canonicalItemId = itemService.getItemId(template).orElse("");
                boolean nonSellable = itemService.hasTag(canonicalItemId, "non-sellable");
                int amount = readAmount(productPath, rawProductId);
                products.add(new ShopItemData(
                        normalize(rawProductId),
                        template,
                        amount,
                        Math.max(0, (int) configService.getShopsLong(productPath + ".order", products.size())),
                        configService.getShopsLong(productPath + ".buy-price", 0L),
                        configService.getShopsLong(productPath + ".sell-price", 0L),
                        configService.getShopsBoolean(productPath + ".purchasable", false),
                        !nonSellable && configService.getShopsBoolean(productPath + ".sellable", false),
                        ShopMatchMode.fromConfig(configService.getShopsString(productPath + ".match-mode", "MATERIAL")),
                        configService.getShopsString(productPath + ".currency-item-id", ""),
                        configService.getShopsString(productPath + ".required-farming-crop", "")
                ));
            }
            shops.put(shopId, new ShopData(shopId, title, products));
        }
    }

    public void reload() {
        configService.reloadShopsConfig();
        load();
    }

    public Optional<ShopData> get(String shopId) {
        return Optional.ofNullable(shops.get(normalize(shopId)));
    }

    public List<ShopData> getAll() {
        return shops.values().stream().sorted(Comparator.comparing(ShopData::shopId)).toList();
    }

    public boolean save(ShopData shop) {
        String path = "shops." + shop.shopId();
        configService.setShopsValue(path, null);
        configService.setShopsValue(path + ".title", shop.title());
        for (ShopItemData product : shop.items()) {
            String productPath = path + ".items." + product.productId();
            writeTemplate(productPath, product.template());
            configService.setShopsValue(productPath + ".amount", product.amount());
            configService.setShopsValue(productPath + ".order", product.order());
            configService.setShopsValue(productPath + ".buy-price", product.buyPrice());
            configService.setShopsValue(productPath + ".sell-price", product.sellPrice());
            configService.setShopsValue(productPath + ".purchasable", product.purchasable());
            configService.setShopsValue(productPath + ".sellable", product.sellable());
            configService.setShopsValue(productPath + ".match-mode", product.matchMode().name());
            configService.setShopsValue(productPath + ".currency-item-id", product.currencyItemId());
            configService.setShopsValue(productPath + ".required-farming-crop", product.requiredFarmingCropId());
        }
        if (!configService.saveShopsConfig()) {
            return false;
        }
        shops.put(shop.shopId(), shop);
        return true;
    }

    private ItemStack readTemplate(String productPath) {
        String type = configService.getShopsString(productPath + ".item.type", "vanilla");
        if (type.equalsIgnoreCase("custom")) {
            ItemStack configuredItem = itemService.create(
                    configService.getShopsString(productPath + ".item.id", ""),
                    1
            ).orElse(null);

            // Custom-ID products must follow the current items.yml definition.
            // Serialized data is retained only for explicitly exact-match products.
            ShopMatchMode matchMode = ShopMatchMode.fromConfig(
                    configService.getShopsString(productPath + ".match-mode", "CUSTOM_ID")
            );
            if (configuredItem != null && matchMode != ShopMatchMode.EXACT) {
                return configuredItem;
            }

            ItemStack serialized = configService.getShopsItemStack(productPath + ".item.serialized");
            if (serialized != null && !serialized.getType().isAir()) {
                serialized.setAmount(1);
                return serialized;
            }
            return configuredItem;
        }

        ItemStack serialized = configService.getShopsItemStack(productPath + ".item.serialized");
        if (serialized != null && !serialized.getType().isAir()) {
            serialized.setAmount(1);
            return serialized;
        }
        String materialName = configService.getShopsString(productPath + ".item.material", "STONE");
        if (materialName.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())) {
            materialName = materialName.substring("minecraft:".length());
        }
        Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
        return material == null || !material.isItem() ? null : new ItemStack(material);
    }

    private int readAmount(String productPath, String productId) {
        var section = configService.getShopsSection(productPath);
        if (section != null && section.isSet("amount")) {
            long configured = configService.getShopsLong(productPath + ".amount", 1L);
            if (configured <= 0L) {
                logger.warning("Invalid shop amount; using 1: " + productPath + ".amount=" + configured);
                return 1;
            }
            if (configured > Integer.MAX_VALUE) {
                logger.warning("Shop amount is too large; using 1: " + productPath + ".amount=" + configured);
                return 1;
            }
            return (int) configured;
        }

        ItemStack serialized = configService.getShopsItemStack(productPath + ".item.serialized");
        int legacyAmount = serialized == null || serialized.getType().isAir()
                ? 1 : Math.max(1, serialized.getAmount());
        if (legacyAmount > 1) {
            logger.info("Using legacy serialized count as shop amount for " + productId + ": " + legacyAmount);
        }
        return legacyAmount;
    }

    private void writeTemplate(String productPath, ItemStack template) {
        configService.setShopsValue(productPath + ".item", null);
        ItemStack clean = template.clone();
        clean.setAmount(1);
        // Preserves enhancement, option, and other PDC data for EXACT matching.
        configService.setShopsValue(productPath + ".item.serialized", clean);
        String customId = itemService.getItemId(clean).orElse(null);
        if (customId != null) {
            configService.setShopsValue(productPath + ".item.type", "custom");
            configService.setShopsValue(productPath + ".item.id", customId);
        } else {
            configService.setShopsValue(productPath + ".item.type", "vanilla");
            configService.setShopsValue(productPath + ".item.material", clean.getType().name());
            configService.setShopsValue(productPath + ".item.serialized", clean);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
