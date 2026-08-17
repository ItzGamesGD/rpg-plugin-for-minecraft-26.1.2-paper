package com.hyunseo.hyunseorpg.mob;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MobTagService {
    public static final String RPG_MOB_TAG = "rpg_mob";

    private final NamespacedKey rpgMobKey;
    private final NamespacedKey mobTagsKey;
    private final NamespacedKey mobIdKey;
    private final NamespacedKey mobLevelKey;
    private final NamespacedKey mobDisplayNameKey;
    private final NamespacedKey customMobKey;
    private final NamespacedKey customMobIdKey;
    private final NamespacedKey spawnSourceKey;
    private final NamespacedKey dropTableIdKey;

    public MobTagService(JavaPlugin plugin) {
        this.rpgMobKey = new NamespacedKey(plugin, "rpg_mob");
        this.mobTagsKey = new NamespacedKey(plugin, "mob_tags");
        this.mobIdKey = new NamespacedKey(plugin, "mob_id");
        this.mobLevelKey = new NamespacedKey(plugin, "mob_level");
        this.mobDisplayNameKey = new NamespacedKey(plugin, "mob_display_name");
        this.customMobKey = new NamespacedKey(plugin, "custom_mob");
        this.customMobIdKey = new NamespacedKey(plugin, "custom_mob_id");
        this.spawnSourceKey = new NamespacedKey(plugin, "spawn_source");
        this.dropTableIdKey = new NamespacedKey(plugin, "drop_table_id");
    }

    public boolean isRpgMob(LivingEntity entity) {
        Byte value = entity.getPersistentDataContainer().get(rpgMobKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public boolean hasTag(LivingEntity entity, String tag) {
        return getTags(entity).contains(normalizeTag(tag));
    }

    public void addTag(LivingEntity entity, String tag) {
        String normalizedTag = normalizeTag(tag);
        Set<String> tags = new LinkedHashSet<>(getTags(entity));
        tags.add(normalizedTag);
        if (normalizedTag.equals(RPG_MOB_TAG)) {
            entity.getPersistentDataContainer().set(rpgMobKey, PersistentDataType.BYTE, (byte) 1);
        }
        writeTags(entity, tags);
        syncScoreboardTags(entity);
        updateNameplate(entity);
    }

    public void removeTag(LivingEntity entity, String tag) {
        String normalizedTag = normalizeTag(tag);
        Set<String> tags = new LinkedHashSet<>(getTags(entity));
        tags.remove(normalizedTag);
        entity.removeScoreboardTag(normalizedTag);
        if (normalizedTag.equals(RPG_MOB_TAG)) {
            entity.getPersistentDataContainer().remove(rpgMobKey);
        }
        writeTags(entity, tags);
        syncScoreboardTags(entity);
        updateNameplate(entity);
    }

    public Set<String> getTags(LivingEntity entity) {
        String rawTags = entity.getPersistentDataContainer().get(mobTagsKey, PersistentDataType.STRING);
        if (rawTags == null || rawTags.isBlank()) {
            return Collections.emptySet();
        }

        return Arrays.stream(rawTags.split(","))
                .map(this::normalizeTag)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setMobId(LivingEntity entity, String mobId) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String normalizedMobId = mobId == null ? "" : mobId.trim().toLowerCase(Locale.ROOT);
        if (normalizedMobId.isEmpty()) {
            pdc.remove(mobIdKey);
            return;
        }
        pdc.set(mobIdKey, PersistentDataType.STRING, normalizedMobId);
        updateNameplate(entity);
    }

    public String getMobId(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(mobIdKey, PersistentDataType.STRING, "");
    }

    public void setMobLevel(LivingEntity entity, int level) {
        entity.getPersistentDataContainer().set(mobLevelKey, PersistentDataType.INTEGER, Math.max(1, level));
        updateNameplate(entity);
    }

    public int getMobLevel(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(mobLevelKey, PersistentDataType.INTEGER, 1);
    }

    public void markCustomMob(LivingEntity entity, String customMobId, String spawnSource, String dropTableId) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(customMobKey, PersistentDataType.BYTE, (byte) 1);
        setStringOrRemove(pdc, customMobIdKey, customMobId);
        setStringOrRemove(pdc, spawnSourceKey, spawnSource);
        setStringOrRemove(pdc, dropTableIdKey, dropTableId);
    }

    public boolean isCustomMob(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(customMobKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    public String getCustomMobId(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(customMobIdKey, PersistentDataType.STRING, "");
    }

    public String getSpawnSource(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(spawnSourceKey, PersistentDataType.STRING, "");
    }

    public String getDropTableId(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(dropTableIdKey, PersistentDataType.STRING, "");
    }

    public void setMobDisplayName(LivingEntity entity, String displayName) {
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        if (normalizedDisplayName.isEmpty()) {
            entity.getPersistentDataContainer().remove(mobDisplayNameKey);
        } else {
            entity.getPersistentDataContainer().set(mobDisplayNameKey, PersistentDataType.STRING, normalizedDisplayName);
        }
        updateNameplate(entity);
    }

    public String getMobDisplayName(LivingEntity entity) {
        String configuredName = entity.getPersistentDataContainer().get(mobDisplayNameKey, PersistentDataType.STRING);
        if (configuredName != null && !configuredName.isBlank()) {
            return configuredName;
        }
        return toDisplayName(entity.getType().name());
    }

    public void syncScoreboardTags(LivingEntity entity) {
        Set<String> pdcTags = getTags(entity);
        pdcTags.forEach(entity::addScoreboardTag);

        if (isRpgMob(entity)) {
            entity.addScoreboardTag(RPG_MOB_TAG);
        } else if (!pdcTags.contains(RPG_MOB_TAG)) {
            entity.removeScoreboardTag(RPG_MOB_TAG);
        }
        updateNameplate(entity);
    }

    public void updateNameplate(LivingEntity entity) {
        if (!isRpgMob(entity) && getTags(entity).isEmpty()) {
            entity.customName(null);
            entity.setCustomNameVisible(false);
            return;
        }

        entity.customName(Component.text("Lvl." + getMobLevel(entity) + " " + getMobDisplayName(entity), NamedTextColor.RED));
        entity.setCustomNameVisible(true);
    }

    private void writeTags(LivingEntity entity, Set<String> tags) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (tags.isEmpty()) {
            pdc.remove(mobTagsKey);
            return;
        }
        pdc.set(mobTagsKey, PersistentDataType.STRING, String.join(",", tags));
    }

    private void setStringOrRemove(PersistentDataContainer pdc, NamespacedKey key, String value) {
        if (value == null || value.isBlank()) {
            pdc.remove(key);
            return;
        }
        pdc.set(key, PersistentDataType.STRING, value.trim().toLowerCase(Locale.ROOT));
    }

    private String normalizeTag(String tag) {
        String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("tag must not be empty");
        }
        return normalized;
    }

    private String toDisplayName(String entityTypeName) {
        String lower = entityTypeName.toLowerCase(Locale.ROOT).replace("_", " ");
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
