package com.hyunseo.hyunseorpg.special.thanatos;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Presentation-only Thanatos display choreography. No display position is combat state. */
final class ThanatosVfx {
    private static final Particle CYAN = Particle.TRIAL_SPAWNER_DETECTION_OMINOUS;
    private static final double[] TRIANGLE_PHASES = {0.0D, Math.PI};
    static final int TRIANGLE_EDGES = 3;
    private final Set<Effect> effects = new HashSet<>();
    private final Map<Display, Vector3f> baseScales = new IdentityHashMap<>();
    private int mortalCount;

    MortalVisual spawnMortal(Location center, ItemStack sword) {
        MortalVisual visual = new MortalVisual(center, item(center, sword, 1.15F, true));
        for (int i = 0; i < 4; i++) visual.upper.add(block(center, Material.POLISHED_DEEPSLATE_SLAB,
                new Vector3f(0.78F, 0.18F, 0.42F)));
        for (int i = 0; i < 2; i++) visual.upper.add(block(center, Material.SOUL_LANTERN, new Vector3f(0.48F)));
        for (int i = 0; i < 3; i++) visual.lower.add(block(center, Material.NETHERITE_BLOCK,
                new Vector3f(1.08F, 0.09F, 0.30F)));
        for (int i = 0; i < 2; i++) visual.lanterns.add(item(center,
                new ItemStack(Material.SOUL_LANTERN), 0.72F, false));
        visual.cacheDisplays();
        effects.add(visual);
        mortalCount++;
        return visual;
    }

    void updateMortal(MortalVisual visual, Location target, double intensity, boolean falling, Location swordAt) {
        if (visual.exiting) return;
        visual.center = target.clone();
        visual.intensity = intensity;
        if (visual.sword.isValid()) visual.sword.teleport(swordAt);
        visual.position(falling);
    }

    void executeMortal(MortalVisual visual, Location impact) {
        visual.exiting = true;
        visual.exitAge = 0;
        visual.center = impact.clone();
        World world = impact.getWorld();
        world.playSound(impact, Sound.ITEM_TOTEM_USE, 1.15F, 0.72F);
        // A short column of real cyan particles creates the beam impression; no custom beam primitive is used.
        for (double y = -0.3D; y <= 4.5D; y += 0.28D) {
            world.spawnParticle(CYAN, impact.clone().add(0.0D, y, 0.0D), 1, 0.035D, 0.0D, 0.035D, 0.0D);
            if (((int) (y * 10)) % 6 == 0) world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                    impact.clone().add(0.0D, y, 0.0D), 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2.0D * i / 28.0D;
            world.spawnParticle(i % 3 == 0 ? Particle.SCULK_CHARGE_POP : Particle.SOUL_FIRE_FLAME,
                    impact, 1, Math.cos(angle) * 0.55D, 0.12D + (i % 4) * 0.04D,
                    Math.sin(angle) * 0.55D, 0.22D);
        }
    }

    void discard(MortalVisual visual) { remove(visual); }

    void spawnOppression(Location center, double radius, int durationTicks) {
        OppressionVisual visual = new OppressionVisual(center.clone(), Math.max(2.5D, radius), durationTicks);
        Material line = Material.POLISHED_DEEPSLATE;
        // Six edges for each of the two triangles, split into two segments per edge.
        for (int triangle = 0; triangle < TRIANGLE_PHASES.length; triangle++) {
            for (int edge = 0; edge < TRIANGLE_EDGES; edge++) {
                for (int part = 0; part < 2; part++) {
                    BlockDisplay display = block(center, line,
                            new Vector3f((float) (Math.sqrt(3.0D) * radius * 0.58D / 2.0D), 0.08F, 0.16F));
                    visual.star.add(new StarSegment(display, triangle, edge, (part + 0.5D) / 2.0D));
                }
            }
        }
        for (int i = 0; i < 16; i++) visual.ring.add(block(center, Material.POLISHED_DEEPSLATE_SLAB,
                new Vector3f((float) (radius * 0.36D), 0.08F, 0.18F)));
        for (int i = 0; i < 8; i++) visual.stones.add(block(center, Material.NETHERITE_BLOCK,
                new Vector3f(0.34F, 1.65F, 0.22F)));
        visual.cacheDisplays();
        effects.add(visual);
        center.getWorld().playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.25F, 0.58F);
    }

    SentenceVisual spawnSentence(Location center) {
        SentenceVisual visual = new SentenceVisual(center.clone());
        visual.core = item(center.clone().add(0.0D, 12.5D, 0.0D), new ItemStack(Material.HEAVY_CORE), 2.5F, false);
        for (int i = 0; i < 3; i++) visual.lanterns.add(item(center, new ItemStack(Material.SOUL_LANTERN), 0.6F, false));
        effects.add(visual);
        return visual;
    }

    void updateSentence(SentenceVisual visual, double progress) {
        if (visual.exiting) return;
        visual.progress = progress;
        visual.position();
    }

    void impactSentence(SentenceVisual visual) {
        visual.impacted = true;
        visual.age = 0;
        Location center = visual.center;
        for (int i = 0; i < 10; i++) visual.debris.add(block(center, i % 2 == 0
                ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE, new Vector3f(0.55F, 0.18F, 0.42F)));
        for (int i = 0; i < 12; i++) visual.shock.add(block(center, Material.POLISHED_DEEPSLATE_SLAB,
                new Vector3f(0.65F, 0.06F, 0.12F)));
        visual.cacheDisplays();
        center.getWorld().playSound(center, Sound.BLOCK_HEAVY_CORE_PLACE, 2.0F, 0.45F);
        center.getWorld().playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.3F, 0.55F);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2.0D * i / 48.0D;
            center.getWorld().spawnParticle(i % 3 == 0 ? Particle.SCULK_CHARGE_POP : CYAN,
                    center.clone().add(Math.cos(angle) * 0.6D, 0.15D, Math.sin(angle) * 0.6D), 1,
                    Math.cos(angle) * 0.32D, 0.12D, Math.sin(angle) * 0.32D, 0.12D);
        }
    }

    void discard(SentenceVisual visual) { remove(visual); }

    void tick() {
        for (Effect effect : new ArrayList<>(effects)) {
            if (!effect.tick()) remove(effect);
        }
    }

    void shutdown() {
        for (Effect effect : new ArrayList<>(effects)) remove(effect);
    }

    private void remove(Effect effect) {
        if (!effects.remove(effect)) return;
        if (effect instanceof MortalVisual) mortalCount--;
        for (Display display : effect.displays()) {
            baseScales.remove(display);
            if (display.isValid()) display.remove();
        }
    }

    private BlockDisplay block(Location location, Material material, Vector3f scale) {
        BlockData data = material.createBlockData();
        return location.getWorld().spawn(location, BlockDisplay.class, display -> {
            display.setBlock(data); configure(display);
            baseScales.put(display, new Vector3f(scale));
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale), new Quaternionf()));
        });
    }

    private ItemDisplay item(Location location, ItemStack stack, float scale, boolean sword) {
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(stack); configure(display);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            Quaternionf rotation = new Quaternionf();
            if (sword) rotation.rotateZ((float) Math.PI);
            baseScales.put(display, new Vector3f(scale));
            display.setTransformation(new Transformation(new Vector3f(), rotation,
                    new Vector3f(scale), new Quaternionf()));
        });
    }

    private void configure(Display display) {
        display.setPersistent(false);
        display.setBillboard(Display.Billboard.FIXED);
        display.setViewRange(48.0F);
        display.setTeleportDuration(2);
        display.setInterpolationDuration(2);
    }

    private static void place(Display display, Location center, double radius, double angle, double y) {
        if (!display.isValid()) return;
        display.teleport(center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
        display.setRotation((float) Math.toDegrees(-angle), 0.0F);
    }

    abstract static class Effect {
        abstract boolean tick();
        abstract List<? extends Display> displays();
    }

    final class MortalVisual extends Effect {
        private Location center;
        private final ItemDisplay sword;
        private final List<Display> upper = new ArrayList<>();
        private final List<BlockDisplay> lower = new ArrayList<>();
        private final List<ItemDisplay> lanterns = new ArrayList<>();
        private int age, exitAge;
        private double intensity;
        private boolean exiting;
        private List<Display> displays;

        private MortalVisual(Location center, ItemDisplay sword) { this.center = center.clone(); this.sword = sword; }
        private void position(boolean falling) {
            age++;
            double speed = 0.035D + intensity * 0.085D;
            double upperY = falling ? 2.5D : 4.0D;
            for (int i = 0; i < upper.size(); i++) {
                boolean lantern = i >= 4;
                double phase = lantern ? (i - 4) * Math.PI + Math.PI / 4.0D : i * Math.PI / 2.0D;
                place(upper.get(i), center, lantern ? 2.05D : 1.65D, age * speed + phase,
                        upperY + (lantern ? 0.36D : 0.0D));
            }
            for (int i = 0; i < lower.size(); i++) place(lower.get(i), center, 1.18D,
                    -age * speed * 0.82D + i * Math.PI * 2.0D / lower.size(), 2.25D);
            int particleInterval = mortalParticleInterval(mortalCount);
            for (int i = 0; i < lanterns.size(); i++) {
                double angle = age * speed * 1.12D + i * Math.PI * 2.0D / lanterns.size();
                place(lanterns.get(i), center, 2.05D, angle, 2.0D);
                if ((age + i) % particleInterval == 0) center.getWorld().spawnParticle(i == 1 ? Particle.SCULK_CHARGE_POP
                                : age % 3 == 0 ? CYAN : Particle.SOUL_FIRE_FLAME,
                        center.clone().add(Math.cos(angle) * 2.05D, 2.0D, Math.sin(angle) * 2.05D), 1,
                        0.02D, 0.03D, 0.02D, 0.0D);
            }
        }
        @Override boolean tick() {
            if (!exiting) return sword.isValid();
            exitAge++;
            double release = exitAge * 0.42D;
            for (int i = 1; i < displays.size(); i++) {
                double angle = i * 2.39996D + exitAge * 0.22D;
                place(displays.get(i), center, 1.5D + release, angle, 2.0D + Math.sin(angle) - exitAge * 0.06D);
                transform(displays.get(i), Math.max(0.05F, 1.0F - exitAge / 13.0F), angle, 0.0F);
            }
            if (sword.isValid()) sword.teleport(center.clone().add(0.0D, Math.max(-0.5D, 0.5D - exitAge * 0.12D), 0.0D));
            return exitAge < 12;
        }
        private void cacheDisplays() { displays = new ArrayList<>(12); displays.add(sword); displays.addAll(upper); displays.addAll(lower); displays.addAll(lanterns); }
        @Override List<? extends Display> displays() { return displays; }
    }

    private final class OppressionVisual extends Effect {
        private final Location center; private final double radius; private final int duration;
        private final List<StarSegment> star = new ArrayList<>();
        private final List<BlockDisplay> ring = new ArrayList<>(), stones = new ArrayList<>();
        private List<Display> displays;
        private int age;
        private OppressionVisual(Location center, double radius, int duration) { this.center = center; this.radius = radius; this.duration = duration; }
        @Override boolean tick() {
            age++; boolean exiting = age > duration; double shrink = exiting ? Math.max(0.05D, 1.0D - (age - duration) / 12.0D) : 1.0D;
            double rotation = age * 0.012D;
            for (StarSegment segment : star) {
                SegmentPose pose = starSegmentPose(segment.triangleIndex, segment.edgeIndex, segment.localT,
                        radius * 0.58D, rotation);
                segment.display.teleport(center.clone().add(pose.x, 0.08D - (exiting ? (age-duration)*.035D : 0), pose.z));
                segment.display.setRotation((float) Math.toDegrees(-pose.angle), 0.0F);
            }
            for (int i = 0; i < ring.size(); i++) place(ring.get(i), center, radius,
                    i * Math.PI * 2.0D / ring.size() - age * 0.016D, 0.09D - (exiting ? (age-duration)*.035D : 0));
            for (int i = 0; i < stones.size(); i++) {
                double angle = i * Math.PI * 2.0D / stones.size();
                place(stones.get(i), center, radius * 1.24D, angle,
                        Math.max(-0.7D, 0.85D - Math.min(age, 7)*.10D - (exiting ? (age-duration)*.12D : 0)));
                float inwardTilt = (float) Math.toRadians(6.0D + (i % 4) * 1.5D);
                transform(stones.get(i), (float) shrink, angle, inwardTilt);
            }
            if (!exiting && age % 2 == 0) {
                double angle = -age * .09D; center.getWorld().spawnParticle(CYAN,
                        center.clone().add(Math.cos(angle)*radius,.18D,Math.sin(angle)*radius),2,.04D,.02D,.04D,0);
                if (age < 8) for (int i=0;i<8;i++) center.getWorld().spawnParticle(Particle.SOUL,
                        center.clone().add(Math.cos(i*Math.PI/4)*radius,1.2D,Math.sin(i*Math.PI/4)*radius),1,
                        -Math.cos(i*Math.PI/4),-.18D,-Math.sin(i*Math.PI/4),.16D);
            }
            for (StarSegment segment : star) transform(segment.display, (float) shrink, 0.0D, 0.0F);
            for (Display display : ring) transform(display, (float) shrink, 0.0D, 0.0F);
            return age <= duration + 12;
        }
        private void cacheDisplays() { displays = new ArrayList<>(star.size()+ring.size()+stones.size()); for (StarSegment s:star) displays.add(s.display); displays.addAll(ring); displays.addAll(stones); }
        @Override List<? extends Display> displays() { return displays; }
    }

    final class SentenceVisual extends Effect {
        private final Location center; private ItemDisplay core;
        private final List<ItemDisplay> lanterns=new ArrayList<>(); private final List<BlockDisplay> debris=new ArrayList<>(),shock=new ArrayList<>();
        private double progress; private boolean impacted, exiting; private int age; private List<Display> displays;
        private SentenceVisual(Location center){this.center=center;}
        private void position(){
            double eased = exponentialIn(progress);
            double y=12.5D-12.0D*eased; core.teleport(center.clone().add(0,y,0));
            double radius=progress>.78D ? 1.2D-(progress-.78D)*2.0D : 1.45D;
            for(int i=0;i<lanterns.size();i++) place(lanterns.get(i),center,radius,progress*8+i*Math.PI*2/3,y);
            int trail=1+(int)Math.floor(progress*5); for(int i=0;i<trail;i++) center.getWorld().spawnParticle(i%2==0?Particle.SOUL:CYAN,
                    center.clone().add(0,y+.5D+i*.36D,0),1,.12D,.08D,.12D,0);
            if(progress>.80D) for(int i=0;i<3;i++){double a=i*Math.PI*2/3+progress*7; center.getWorld().spawnParticle(Particle.SOUL,
                    center.clone().add(Math.cos(a)*2.5D,.2D,Math.sin(a)*2.5D),1,-Math.cos(a),.02D,-Math.sin(a),.18D);}
            for(int i=0;i<12;i++){double a=i*Math.PI/6;center.getWorld().spawnParticle(CYAN,center.clone().add(Math.cos(a)*2.2D,.08D,Math.sin(a)*2.2D),1,0,0,0,0);}
        }
        @Override boolean tick(){
            if(!impacted) return core.isValid(); age++;
            if(core.isValid()) core.teleport(center.clone().add(0,.35D-Math.max(0,age-7)*.08D,0));
            for(int i=0;i<debris.size();i++){double a=i*Math.PI*2/debris.size();double r=.8D+age*.22D;place(debris.get(i),center,r,a,Math.max(-.2D,age*.16D-age*age*.012D));transform(debris.get(i),Math.max(.05F,1-age/20F),a+age*.22D,0.0F);}
            for(int i=0;i<shock.size();i++){double a=i*Math.PI*2/shock.size();place(shock.get(i),center,.5D+age*.34D,a,.08D);transform(shock.get(i),Math.max(.05F,1-age/14F),a,0.0F);}
            return age<18;
        }
        private void cacheDisplays(){displays=new ArrayList<>(1+lanterns.size()+debris.size()+shock.size());displays.add(core);displays.addAll(lanterns);displays.addAll(debris);displays.addAll(shock);}
        @Override List<? extends Display> displays(){if(displays==null)cacheDisplays();return displays;}
    }

    static double exponentialIn(double progress) {
        if (progress <= 0.0D) return 0.0D;
        if (progress >= 1.0D) return 1.0D;
        return Math.pow(2.0D, 10.0D * progress - 10.0D);
    }

    static SegmentPose starSegmentPose(int triangleIndex, int edgeIndex, double localT, double radius, double rotation) {
        double phase = trianglePhase(triangleIndex);
        double start = phase + edgeIndex * Math.PI * 2.0D / TRIANGLE_EDGES + rotation;
        double end = phase + (edgeIndex + 1) * Math.PI * 2.0D / TRIANGLE_EDGES + rotation;
        double x = (Math.cos(start) * (1.0D-localT) + Math.cos(end) * localT) * radius;
        double z = (Math.sin(start) * (1.0D-localT) + Math.sin(end) * localT) * radius;
        return new SegmentPose(x, z, Math.atan2(Math.sin(end)-Math.sin(start), Math.cos(end)-Math.cos(start)));
    }

    record SegmentPose(double x, double z, double angle) { }
    private record StarSegment(BlockDisplay display, int triangleIndex, int edgeIndex, double localT) { }

    static double trianglePhase(int triangleIndex) { return TRIANGLE_PHASES[triangleIndex]; }

    static int mortalParticleInterval(int activeMortals) {
        if (activeMortals <= 4) return 2;
        return activeMortals <= 8 ? 3 : 4;
    }

    private void transform(Display display, float multiplier, double tumble, float inwardTilt) {
        Vector3f base = baseScales.get(display);
        if (base == null) return;
        display.setTransformation(new Transformation(new Vector3f(),
                new Quaternionf().rotateXYZ(inwardTilt + (float)(tumble*.35D),(float)tumble,(float)(tumble*.18D)),
                new Vector3f(base).mul(multiplier), new Quaternionf()));
        display.setInterpolationDelay(0); display.setInterpolationDuration(2);
    }
}
