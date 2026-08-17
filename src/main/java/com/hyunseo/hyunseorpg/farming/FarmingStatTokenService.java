package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Account-bound token consumption boundary. It records uses in the existing
 * farming profile; effect application remains a later stat integration point.
 */
public final class FarmingStatTokenService {
    private final ConfigService config;
    private final RPGItemService items;
    private final FarmingProfileService profiles;
    private final NamespacedKey ownerKey;
    private Map<String, Definition> definitions = Map.of();
    private boolean enabled;

    public FarmingStatTokenService(JavaPlugin plugin, ConfigService config,
                                   RPGItemService items, FarmingProfileService profiles) {
        this.config = config;
        this.items = items;
        this.profiles = profiles;
        this.ownerKey = new NamespacedKey(plugin, "farming_stat_token_owner");
    }

    public void load() {
        enabled = config.getFarmingStatTokensBoolean("enabled", false);
        if (!enabled) {
            definitions = Map.of();
            return;
        }
        Map<String, Definition> loaded = new LinkedHashMap<>();
        for (String rawId : config.getFarmingStatTokensKeys("tokens")) {
            String id = normalize(rawId);
            String path = "tokens." + rawId;
            String itemId = normalize(config.getFarmingStatTokensString(path + ".item-id", ""));
            int maximumUses = config.getFarmingStatTokensInt(path + ".maximum-uses", 0);
            String effect = normalize(config.getFarmingStatTokensString(path + ".effect", ""));
            double amount = config.getFarmingStatTokensDouble(path + ".effect-amount", 0.0D);
            boolean accountBound = config.getFarmingStatTokensBoolean(path + ".account-bound", true);
            if (id.isBlank() || itemId.isBlank() || maximumUses < 1 || !Double.isFinite(amount)) continue;
            if (items.getData(itemId).isEmpty()) continue;
            loaded.put(id, new Definition(id, itemId, maximumUses, effect, amount, accountBound));
        }
        definitions = Map.copyOf(loaded);
    }

    public Optional<Definition> definition(String tokenId) {
        return Optional.ofNullable(definitions.get(normalize(tokenId)));
    }

    public List<String> definitionIds() {
        return List.copyOf(definitions.keySet());
    }

    public boolean setUses(UUID playerId, String tokenId, int uses) {
        Definition definition = definition(tokenId).orElse(null);
        return definition != null && uses >= 0 && uses <= definition.maximumUses()
                && profiles.setStatTokenUses(playerId, definition.id(), uses);
    }

    public Optional<Definition> definitionForItem(ItemStack item) {
        String itemId = item == null ? "" : items.getItemId(item).orElse("");
        return definitions.values().stream().filter(value -> value.itemId().equals(itemId)).findFirst();
    }

    public UseResult use(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) return UseResult.INVALID;
        Definition definition = definitionForItem(item).orElse(null);
        if (definition == null) return UseResult.INVALID;
        if (item.getAmount() < 1) return UseResult.INVALID;
        if (!bindOrCheckOwner(player, item, definition.accountBound())) return UseResult.NOT_OWNER;
        if (!profiles.useStatToken(player.getUniqueId(), definition.id(), definition.maximumUses())) {
            return UseResult.LIMIT_REACHED;
        }
        item.setAmount(item.getAmount() - 1);
        player.updateInventory();
        return UseResult.SUCCESS;
    }

    private boolean bindOrCheckOwner(Player player, ItemStack item, boolean accountBound) {
        if (!accountBound) return true;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String owner = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        String playerId = player.getUniqueId().toString();
        if (owner != null && !owner.isBlank() && !owner.equals(playerId)) return false;
        if (owner == null || owner.isBlank()) {
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, playerId);
            item.setItemMeta(meta);
        }
        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Definition(String id, String itemId, int maximumUses,
                             String effect, double effectAmount, boolean accountBound) { }

    public enum UseResult { SUCCESS, INVALID, NOT_OWNER, LIMIT_REACHED }
}
