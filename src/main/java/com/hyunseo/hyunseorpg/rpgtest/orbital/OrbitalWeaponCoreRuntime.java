package com.hyunseo.hyunseorpg.rpgtest.orbital;

import com.hyunseo.hyunseorpg.rpgtest.DisplayMotion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Visual-only armillary weapon core, deliberately independent from swarm combat actors. */
public final class OrbitalWeaponCoreRuntime {
    private static final List<Material> FAMILIES = List.of(Material.NETHERITE_HOE, Material.NETHERITE_AXE,
            Material.NETHERITE_SWORD, Material.MACE);
    private final Plugin plugin;
    private final List<ItemDisplay> weapons = new ArrayList<>();
    private BukkitTask task;

    public OrbitalWeaponCoreRuntime(Plugin plugin) { this.plugin = plugin; }

    public void start(Location core, int ringCount) {
        cleanup();
        int rings = Math.max(1, Math.min(FAMILIES.size(), ringCount));
        for (int ring = 0; ring < rings; ring++) {
            for (int slot = 0; slot < 4; slot++) {
                ItemDisplay display = core.getWorld().spawn(core, ItemDisplay.class, item -> {
                    item.setItemStack(new ItemStack(FAMILIES.get(weapons.size() / 4)));
                    item.setPersistent(false);
                    DisplayMotion.configure(item);
                });
                weapons.add(display);
            }
        }
        task = new BukkitRunnable() {
            int tick;
            @Override public void run() {
                if (weapons.stream().anyMatch(entity -> !entity.isValid())) { cleanup(); cancel(); return; }
                for (int index = 0; index < weapons.size(); index++) {
                    int ring = index / 4; int slot = index % 4;
                    double local = tick * (.045 + ring * .006) + slot * Math.PI / 2;
                    double plane = tick * (.008 + ring * .002) + ring * 1.17;
                    double radius = 2.6 + ring * .48;
                    Vector localPoint = new Vector(Math.cos(local) * radius, Math.sin(local) * radius, 0);
                    Vector aroundY = rotateY(localPoint, plane);
                    Vector tilted = rotateX(aroundY, .42 + ring * .31);
                    ItemDisplay weapon = weapons.get(index);
                    weapon.teleport(core.clone().add(tilted));
                    weapon.setTransformation(pose(.62F, (float) (local + plane), (float) (tick * .11D + ring),
                            (float) (Math.sin(local) * .5D)));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private Vector rotateY(Vector v, double a) { return new Vector(v.getX()*Math.cos(a)+v.getZ()*Math.sin(a), v.getY(), -v.getX()*Math.sin(a)+v.getZ()*Math.cos(a)); }
    private Vector rotateX(Vector v, double a) { return new Vector(v.getX(), v.getY()*Math.cos(a)-v.getZ()*Math.sin(a), v.getY()*Math.sin(a)+v.getZ()*Math.cos(a)); }
    private Transformation pose(float scale, float yaw, float pitch, float roll) {
        return new Transformation(new Vector3f(-scale / 2F), new Quaternionf().rotationYXZ(yaw, pitch, roll),
                new Vector3f(scale), new Quaternionf());
    }
    public void cleanup() { if (task != null) { task.cancel(); task = null; } weapons.forEach(Entity::remove); weapons.clear(); }
}
