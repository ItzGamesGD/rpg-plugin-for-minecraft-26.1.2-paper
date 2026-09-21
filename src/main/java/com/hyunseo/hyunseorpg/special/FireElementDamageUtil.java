package com.hyunseo.hyunseorpg.special;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared FIRE damage and temporary flame-buff calculation for special equipment. */
public final class FireElementDamageUtil {
    private final Map<UUID, FlameBuff> buffs = new ConcurrentHashMap<>();

    public FireElementDamageUtil() { }

    public double boost(Player player, ItemStack item, double damage) {
        if (damage <= 0.0D) return 0.0D;
        double bonus = 0.0D;
        FlameBuff buff = activeBuff(player);
        if (buff != null) bonus += buff.damageBonus();
        return damage * (1.0D + Math.max(0.0D, bonus));
    }

    public boolean activateBuff(Player player, long durationMillis, double damageBonus,
                                boolean allowRefresh, boolean allowStack) {
        if (player == null || durationMillis <= 0L) return false;
        UUID id = player.getUniqueId();
        FlameBuff current = activeBuff(player);
        if (current != null && !allowStack && !allowRefresh) return false;
        long end = System.currentTimeMillis() + durationMillis;
        if (current != null && allowStack) {
            damageBonus += current.damageBonus();
            end = Math.max(end, current.expiresAt());
        } else if (current != null && !allowRefresh) {
            return false;
        }
        buffs.put(id, new FlameBuff(end, Math.max(0.0D, damageBonus)));
        return true;
    }

    public boolean hasBuff(Player player) {
        return activeBuff(player) != null;
    }

    public void clear(Player player) {
        if (player != null) buffs.remove(player.getUniqueId());
    }

    public void clearAll() {
        buffs.clear();
    }

    private FlameBuff activeBuff(Player player) {
        if (player == null) return null;
        FlameBuff buff = buffs.get(player.getUniqueId());
        if (buff != null && buff.expiresAt() <= System.currentTimeMillis()) {
            buffs.remove(player.getUniqueId(), buff);
            return null;
        }
        return buff;
    }

    private record FlameBuff(long expiresAt, double damageBonus) { }
}
