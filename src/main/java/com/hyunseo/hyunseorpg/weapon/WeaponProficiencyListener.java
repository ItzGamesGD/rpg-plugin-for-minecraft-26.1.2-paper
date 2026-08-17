package com.hyunseo.hyunseorpg.weapon;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class WeaponProficiencyListener implements Listener {
    private final ConfigService configService;
    private final WeaponService weaponService;
    private final WeaponProficiencyService proficiencyService;

    public WeaponProficiencyListener(ConfigService configService, WeaponService weaponService, WeaponProficiencyService proficiencyService) {
        this.configService = configService;
        this.weaponService = weaponService;
        this.proficiencyService = proficiencyService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = getAttackingPlayer(event.getDamager());
        if (attacker == null || event.getEntity() instanceof Player) {
            return;
        }
        weaponService.getWeaponType(attacker.getInventory().getItemInMainHand()).ifPresent(weaponType -> {
            long experience = Math.max(0L, configService.getWeaponsInt("proficiency.combat-exp-per-hit", 1));
            WeaponProficiencyResult result = proficiencyService.addExperience(attacker, weaponType, experience);
            if (result.leveledUp()) {
                attacker.sendMessage(Component.text(weaponType.displayName() + " 숙련도가 Lv." + result.level() + "이 되었습니다.", NamedTextColor.GREEN));
            }
        });
    }

    private Player getAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            return source instanceof Player player ? player : null;
        }
        return null;
    }
}
