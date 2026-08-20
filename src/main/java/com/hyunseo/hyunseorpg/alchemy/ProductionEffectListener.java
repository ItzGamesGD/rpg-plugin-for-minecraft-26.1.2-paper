package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.UUID;

/** Bridges Paper damage events to the registered production effect handlers. */
public final class ProductionEffectListener implements Listener {
    private final EffectService effects;

    public ProductionEffectListener(EffectService effects) { this.effects = effects; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        UUID sourceId = sourceId(event);
        double damage = effects.modifyDamage(sourceId, target.getUniqueId(), event.getDamage());
        if (Double.isFinite(damage) && damage >= 0.0D) event.setDamage(damage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        double dealt = event.getFinalDamage();
        if (!Double.isFinite(dealt) || dealt <= 0.0D) dealt = event.getDamage();
        effects.notifyDamage(sourceId(event), target.getUniqueId(), dealt,
                CombatEffectHandler.DamageKind.DIRECT);
    }

    private UUID sourceId(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return null;
        Entity damager = byEntity.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter.getUniqueId();
        }
        return damager.getUniqueId();
    }
}
