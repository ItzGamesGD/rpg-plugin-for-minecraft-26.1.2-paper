package com.hyunseo.hyunseorpg.vfx.lightning;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Thin core/glow Display lightning. This class creates visuals only and never queries or damages targets. */
public final class CustomLightningRenderer {
    private final JavaPlugin plugin;
    private final Map<UUID, Set<Visual>> visuals = new HashMap<>();

    public CustomLightningRenderer(JavaPlugin plugin) { this.plugin = plugin; }

    public void renderLightning(UUID owner, Location start, Location end,
            CustomLightningParameters parameters, long seed) {
        if (owner == null || start == null || end == null || start.getWorld() == null
                || start.getWorld() != end.getWorld()) return;
        List<CustomLightningGeometry.Segment> segments = CustomLightningGeometry.generate(
                start.toVector(), end.toVector(), parameters, seed);
        if (segments.isEmpty()) return;
        Set<BlockDisplay> displays = new HashSet<>();
        for (CustomLightningGeometry.Segment segment : segments) {
            double taper = segment.depth() == 0 ? 1 : Math.pow(.62, segment.depth());
            spawnSegment(start.getWorld(), segment, parameters.glowThickness() * taper,
                    Material.LIGHT_BLUE_STAINED_GLASS, displays);
            spawnSegment(start.getWorld(), segment, parameters.coreThickness() * taper,
                    Material.WHITE_CONCRETE, displays);
        }
        Visual visual = new Visual(displays);
        visuals.computeIfAbsent(owner, ignored -> new HashSet<>()).add(visual);
        visual.removalTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> remove(owner, visual), parameters.lifetimeTicks());
    }

    private void spawnSegment(World world, CustomLightningGeometry.Segment segment, double thickness,
            Material material, Set<BlockDisplay> displays) {
        Vector delta = segment.end().clone().subtract(segment.start());
        float length = (float) delta.length();
        if (length < .001f) return;
        Location midpoint = segment.start().clone().add(segment.end()).multiply(.5).toLocation(world);
        Quaternionf rotation = new Quaternionf().rotationTo(0, 0, 1,
                (float) (delta.getX() / length), (float) (delta.getY() / length), (float) (delta.getZ() / length));
        float width = (float) thickness;
        Vector3f centeredOffset = rotation.transform(new Vector3f(-width / 2, -width / 2, -length / 2));
        BlockDisplay display = world.spawn(midpoint, BlockDisplay.class, entity -> {
            entity.setBlock(material.createBlockData());
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setPersistent(false);
            entity.setTransformation(new Transformation(centeredOffset,
                    rotation, new Vector3f(width, width, length), new Quaternionf()));
        });
        displays.add(display);
    }

    public void cleanup(UUID owner) {
        Set<Visual> owned = visuals.remove(owner);
        if (owned != null) for (Visual visual : new HashSet<>(owned)) remove(owner, visual);
    }

    public void shutdown() {
        for (UUID owner : new HashSet<>(visuals.keySet())) cleanup(owner);
    }

    public int activeDisplayCount() {
        return visuals.values().stream().flatMap(Collection::stream).mapToInt(v -> v.displays.size()).sum();
    }

    private void remove(UUID owner, Visual visual) {
        if (visual.removalTask != null && !visual.removalTask.isCancelled()) visual.removalTask.cancel();
        for (BlockDisplay display : visual.displays) if (display.isValid() && !display.isDead()) display.remove();
        visual.displays.clear();
        Set<Visual> owned = visuals.get(owner);
        if (owned != null) { owned.remove(visual); if (owned.isEmpty()) visuals.remove(owner); }
    }

    private static final class Visual {
        private final Set<BlockDisplay> displays;
        private BukkitTask removalTask;
        private Visual(Set<BlockDisplay> displays) { this.displays = displays; }
    }
}
