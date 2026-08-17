package com.hyunseo.hyunseorpg.alchemy.potion;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperPotionPdcContract implements PotionPdcContract<ItemStack> {
    private final NamespacedKey potion, version, catalyst, delivery, duration, amplifier, inverted, effect;
    public PaperPotionPdcContract(JavaPlugin plugin) {
        potion = new NamespacedKey(plugin, POTION_ID_KEY);
        version = new NamespacedKey(plugin, DATA_VERSION_KEY);
        catalyst = new NamespacedKey(plugin, CATALYST_KEY);
        delivery = new NamespacedKey(plugin, CATALYST_DELIVERY_KEY);
        duration = new NamespacedKey(plugin, CATALYST_DURATION_KEY);
        amplifier = new NamespacedKey(plugin, CATALYST_AMPLIFIER_KEY);
        inverted = new NamespacedKey(plugin, CATALYST_INVERTED_KEY);
        effect = new NamespacedKey(plugin, CATALYST_EFFECT_KEY);
    }
    @Override public void write(ItemStack item, PotionDefinition definition, String catalystId, int dataVersion) {
        if (item == null || definition == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta(); var pdc = meta.getPersistentDataContainer();
        pdc.set(potion, PersistentDataType.STRING, definition.id()); pdc.set(version, PersistentDataType.INTEGER, dataVersion);
        if (catalystId != null && !catalystId.isBlank()) pdc.set(catalyst, PersistentDataType.STRING, catalystId);
        item.setItemMeta(meta);
    }
    @Override public void writeTransformation(ItemStack item, String deliveryValue, int durationPercent,
                                               int amplifierDelta, boolean invertedValue, String effectId) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta(); var pdc = meta.getPersistentDataContainer();
        pdc.set(delivery, PersistentDataType.STRING, deliveryValue == null ? "ORIGINAL" : deliveryValue);
        pdc.set(duration, PersistentDataType.INTEGER, Math.max(1, durationPercent));
        pdc.set(amplifier, PersistentDataType.INTEGER, amplifierDelta);
        pdc.set(inverted, PersistentDataType.BYTE, invertedValue ? (byte) 1 : (byte) 0);
        if (effectId != null && !effectId.isBlank()) pdc.set(effect, PersistentDataType.STRING, effectId);
        else pdc.remove(effect);
        item.setItemMeta(meta);
    }
    @Override public String readPotionId(ItemStack item) { return read(item, potion); }
    @Override public String readCatalystId(ItemStack item) { return read(item, catalyst); }
    @Override public int readDataVersion(ItemStack item) { if (item == null || item.getItemMeta() == null) return 0; return item.getItemMeta().getPersistentDataContainer().getOrDefault(version, PersistentDataType.INTEGER, 0); }
    @Override public String readDelivery(ItemStack item) { return read(item, delivery); }
    @Override public int readDurationPercent(ItemStack item) { return readInt(item, duration, 100); }
    @Override public int readAmplifierDelta(ItemStack item) { return readInt(item, amplifier, 0); }
    @Override public boolean readInverted(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(inverted, PersistentDataType.BYTE, (byte) 0) != 0;
    }
    @Override public String readEffectOverride(ItemStack item) { return read(item, effect); }
    private int readInt(ItemStack item, NamespacedKey key, int fallback) {
        if (item == null || item.getItemMeta() == null) return fallback;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, fallback);
    }
    private String read(ItemStack item, NamespacedKey key) { if (item == null || item.getItemMeta() == null) return ""; return item.getItemMeta().getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, ""); }
}
