package com.hyunseo.hyunseorpg.skill.swordmaster;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import org.bukkit.Particle;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SwordmasterBasicAttackListener implements Listener {
    private final ConfigService configService;
    private final WeaponService weaponService;
    private final CombatService combatService;
    private final Set<UUID> applyingSplashByPlayer = new HashSet<>();

    public SwordmasterBasicAttackListener(
            ConfigService configService,
            PlayerDataService playerDataService,
            WeaponService weaponService,
            CombatService combatService
    ) {
        this.configService = configService;
        this.weaponService = weaponService;
        this.combatService = combatService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwordmasterAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity primaryTarget)) {
            return;
        }
        if (!configService.getBoolean("swordmaster.basic-splash.enabled", true)) {
            return;
        }
        if (applyingSplashByPlayer.contains(player.getUniqueId())) {
            return;
        }
        if (!isValidSwordmasterAttack(player)) {
            return;
        }

        double radius = configService.getDouble("swordmaster.basic-splash.radius", 3.0D);
        double damage = configService.getDouble("swordmaster.basic-splash.damage", 4.0D);
        int maxTargets = (int) Math.max(1L, configService.getLong("swordmaster.basic-splash.max-targets", 8L));
        int hitCount = 0;

        applyingSplashByPlayer.add(player.getUniqueId());
        try {
            for (Entity nearbyEntity : primaryTarget.getWorld().getNearbyEntities(primaryTarget.getLocation(), radius, radius, radius)) {
                if (hitCount >= maxTargets) {
                    break;
                }
                if (!(nearbyEntity instanceof LivingEntity splashTarget)) {
                    continue;
                }
                if (splashTarget.equals(player) || splashTarget.equals(primaryTarget) || splashTarget.isDead()) {
                    continue;
                }
                if (!(splashTarget instanceof Enemy)) {
                    continue;
                }

                combatService.applyDirectDamage(player, splashTarget, damage);
                splashTarget.getWorld().spawnParticle(Particle.SWEEP_ATTACK, splashTarget.getLocation().add(0.0D, 1.0D, 0.0D), 1, 0.2D, 0.1D, 0.2D, 0.0D);
                hitCount++;
            }
        } finally {
            applyingSplashByPlayer.remove(player.getUniqueId());
        }
    }

    private boolean isValidSwordmasterAttack(Player player) {
        return weaponService.isWeaponType(player.getInventory().getItemInMainHand(), WeaponType.SWORD);
    }
}
