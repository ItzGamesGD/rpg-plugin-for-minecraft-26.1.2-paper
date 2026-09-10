package com.hyunseo.hyunseorpg.rpgtest.basic;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BasicWeaponRuntime {
    private final Plugin plugin;
    private final List<ItemDisplay> displays = new ArrayList<>();

    public BasicWeaponRuntime(Plugin plugin) { this.plugin = plugin; }

    public void start(Player player, Location bossCore, Map<BasicWeaponPattern, Integer> selection) {
        selection.forEach((pattern, count) -> {
            for (int index = 0; index < count; index++) spawn(player, bossCore, pattern, index);
        });
    }

    private void spawn(Player target, Location core, BasicWeaponPattern pattern, int offset) {
        Location snapshot = target.getLocation().clone();
        Location start = pattern == BasicWeaponPattern.MACE_DROP
                ? snapshot.clone().add((offset - 1) * 1.4, 8.0, 0)
                : core.clone().add(Math.cos(offset * 2.1) * 2.0, 1.5, Math.sin(offset * 2.1) * 2.0);
        ItemDisplay display = start.getWorld().spawn(start, ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(material(pattern)));
            entity.setPersistent(false);
            Transformation transformation = entity.getTransformation();
            transformation.getScale().set(new Vector3f(1.2f));
            entity.setTransformation(transformation);
        });
        displays.add(display);
        new BukkitRunnable() {
            int age;
            final double orbitOffset = offset * 1.8;
            final Vector lockedLunge = snapshot.toVector().subtract(start.toVector()).normalize();
            @Override public void run() {
                if (!display.isValid() || !target.isOnline() || age++ > 160) { remove(display); cancel(); return; }
                if (pattern == BasicWeaponPattern.SHIELD_ORBIT) {
                    double angle = orbitOffset + age * 0.09;
                    display.teleport(core.clone().add(Math.cos(angle) * 3, 1.5, Math.sin(angle) * 3));
                    return;
                }
                Vector direction = switch (pattern) {
                    case MACE_DROP -> new Vector(0, -0.42, 0); // snapshot x/z is immutable
                    case SPEAR_LUNGE -> age < 25 ? new Vector() : lockedLunge.clone().multiply(0.42);
                    case TRIDENT_THROWER -> age < 30 ? display.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.10)
                            : lockedLunge.clone().multiply(0.35);
                    default -> target.getLocation().toVector().subtract(display.getLocation().toVector()).normalize().multiply(0.18);
                };
                display.teleport(display.getLocation().add(direction));
                if (display.getLocation().distanceSquared(target.getLocation()) < 1.8) {
                    double damage = pattern == BasicWeaponPattern.MACE_MELEE || pattern == BasicWeaponPattern.MACE_DROP ? 6.0 : 3.0;
                    target.damage(damage);
                    target.setVelocity(direction.clone().normalize().multiply(pattern.name().startsWith("MACE") ? 1.1 : 0.45).setY(0.25));
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1f, 0.8f);
                    remove(display); cancel();
                } else if (pattern == BasicWeaponPattern.MACE_DROP && display.getLocation().getBlock().getType().isSolid()) {
                    impact(display.getLocation()); remove(display); cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void impact(Location location) {
        World world = location.getWorld();
        world.spawnParticle(Particle.EXPLOSION, location, 2);
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
        // Visual-only blast: deliberately never calls World#createExplosion.
    }

    private Material material(BasicWeaponPattern pattern) {
        return switch (pattern) {
            case MACE_MELEE, MACE_DROP -> Material.MACE;
            case SPEAR_MELEE, SPEAR_LUNGE, TRIDENT_THROWER -> Material.TRIDENT;
            case AXE_MELEE -> Material.NETHERITE_AXE;
            case HOE_MELEE -> Material.NETHERITE_HOE;
            case SHIELD_ORBIT -> Material.SHIELD;
        };
    }

    private void remove(ItemDisplay display) { displays.remove(display); display.remove(); }
    public void cleanup() { List.copyOf(displays).forEach(ItemDisplay::remove); displays.clear(); }
}
