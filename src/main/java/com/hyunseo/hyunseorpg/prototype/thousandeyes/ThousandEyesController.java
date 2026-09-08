package com.hyunseo.hyunseorpg.prototype.thousandeyes;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Debug-only boss prototype. One scheduler owns every skill and every tracked visual. */
public final class ThousandEyesController implements Listener {
    public enum Skill { CENTRAL_LASER, GATEWAY_BURST, SCATTER_LASERS, PATH_DASH }

    private final JavaPlugin plugin;
    private final List<BlockDisplay> inner = new ArrayList<>(), outer = new ArrayList<>(), gateways = new ArrayList<>();
    private final List<ItemDisplay> satellites = new ArrayList<>();
    private final List<Location> markers = new ArrayList<>(), scatterPositions = new ArrayList<>(), lockedTargets = new ArrayList<>();
    private List<Integer> scatterOrder = List.of();
    private final Set<UUID> dashHits = new HashSet<>();
    private ArmorStand anchor;
    private ItemDisplay centralEye;
    private Player target;
    private BukkitTask task;
    private ThousandEyesState state = ThousandEyesState.IDLE;
    private Vector forward = new Vector(0, 0, 1);
    private Location dashStart;
    private long seed;
    private Random random;
    private int age, skillAge, dashIndex;
    private double innerAngle, outerAngle, satelliteAngle;

    public ThousandEyesController(JavaPlugin plugin) { this.plugin = plugin; }

    public boolean spawn(Player owner, long fixedSeed) {
        remove();
        target = owner; seed = fixedSeed; random = new Random(seed);
        Location at = owner.getLocation().add(owner.getLocation().getDirection().setY(0).normalize().multiply(5)).add(0, 2.2, 0);
        anchor = at.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setInvisible(true); stand.setInvulnerable(true); stand.setGravity(false); stand.setMarker(true);
            stand.setPersistent(false); stand.addScoreboardTag("hyunseorpg:thousand_eyes");
        });
        centralEye = item(at, ThousandEyesTuning.CENTRAL_EYE_SCALE);
        for (int i = 0; i < ThousandEyesTuning.INNER_COUNT; i++) inner.add(block(at,
                i % 2 == 0 ? Material.END_STONE_BRICK_SLAB : Material.END_STONE_BRICK_WALL,
                i % 2 == 0 ? new Vector3f(1.05F, .30F, .55F) : new Vector3f(.65F, 1.15F, .45F)));
        for (int i = 0; i < ThousandEyesTuning.SATELLITE_COUNT; i++) satellites.add(item(at, ThousandEyesTuning.SATELLITE_SCALE));
        for (int i = 0; i < ThousandEyesTuning.OUTER_COUNT; i++) outer.add(block(at,
                i % 2 == 0 ? Material.END_STONE_BRICKS : Material.END_STONE_BRICK_WALL, new Vector3f(.55F, .85F, .35F)));
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::safeTick, 1L, 1L);
        return true;
    }

    public boolean start(Skill skill) {
        if (!active() || state != ThousandEyesState.IDLE) return false;
        skillAge = 0; markers.clear(); scatterPositions.clear(); lockedTargets.clear(); gateways.forEach(Entity::remove); gateways.clear();
        random = new Random(seed + skill.ordinal() * 7919L);
        state = switch (skill) {
            case CENTRAL_LASER -> ThousandEyesState.CENTRAL_LASER_CHARGE;
            case GATEWAY_BURST -> ThousandEyesState.GATEWAY_SEQUENCE;
            case SCATTER_LASERS -> ThousandEyesState.SCATTER_EYES;
            case PATH_DASH -> ThousandEyesState.PATH_RECORDING;
        };
        if (skill == Skill.SCATTER_LASERS) prepareScatter();
        return true;
    }

    public boolean active() { return anchor != null && anchor.isValid(); }
    public ThousandEyesState state() { return state; }
    public int trackedDisplayCount() { return (centralEye == null ? 0 : 1) + inner.size() + outer.size() + satellites.size() + gateways.size(); }

    private void safeTick() {
        try { tick(); } catch (RuntimeException exception) {
            plugin.getLogger().warning("Thousand Eyes prototype cancelled after exception: " + exception.getMessage()); remove();
        }
    }

    private void tick() {
        if (!active() || target == null || !target.isOnline() || target.isDead() || target.getWorld() != anchor.getWorld()) { remove(); return; }
        age++; skillAge++;
        if (state != ThousandEyesState.PATH_DASH) face(target.getEyeLocation().toVector().subtract(anchor.getLocation().toVector()));
        tickState(); updateRig();
    }

    private void tickState() {
        switch (state) {
            case CENTRAL_LASER_CHARGE -> {
                if (skillAge >= ThousandEyesTuning.CENTRAL_CHARGE_TICKS) {
                    state = ThousandEyesState.CENTRAL_LASER_RELEASE; skillAge = 0;
                    fireRay(centralEye.getLocation(), forward, ThousandEyesTuning.CENTRAL_LASER_RANGE, ThousandEyesTuning.CENTRAL_LASER_DAMAGE);
                    burst(anchor.getLocation(), ThousandEyesTuning.BURST_PARTICLES);
                }
            }
            case CENTRAL_LASER_RELEASE -> { if (skillAge >= ThousandEyesTuning.CENTRAL_RELEASE_TICKS) recover(); }
            case GATEWAY_SEQUENCE -> tickGateways();
            case SCATTER_EYES -> tickScatter();
            case PATH_RECORDING -> tickRecording();
            case PATH_DASH -> tickDash();
            case RECOVERING -> { if (skillAge >= ThousandEyesTuning.RETURN_TICKS) idle(); }
            default -> { }
        }
    }

    private void tickGateways() {
        int spawnWindow = ThousandEyesTuning.GATEWAY_COUNT * ThousandEyesTuning.GATEWAY_INTERVAL_TICKS;
        if (skillAge <= spawnWindow && (skillAge - 1) % ThousandEyesTuning.GATEWAY_INTERVAL_TICKS == 0 && gateways.size() < ThousandEyesTuning.GATEWAY_COUNT) {
            double angle = random.nextDouble() * Math.PI * 2, radius = 2.8 + random.nextDouble() * 4.2;
            Location at = target.getLocation().add(Math.cos(angle) * radius, .4 + random.nextDouble() * 2.8, Math.sin(angle) * radius);
            Material visual = ThousandEyesTuning.USE_END_GATEWAY_DISPLAY ? Material.END_GATEWAY : Material.CRYING_OBSIDIAN;
            BlockDisplay gateway = block(at, visual, new Vector3f(1.15F)); gateways.add(gateway);
            at.getWorld().playSound(at, Sound.BLOCK_END_PORTAL_FRAME_FILL, .8F, 1.15F);
            at.getWorld().spawnParticle(Particle.PORTAL, at, 8, .35, .35, .35, .1);
        }
        int telegraphAt = spawnWindow + ThousandEyesTuning.GATEWAY_DELAY_TICKS;
        if (skillAge >= telegraphAt && skillAge < telegraphAt + ThousandEyesTuning.GATEWAY_TELEGRAPH_TICKS) {
            for (BlockDisplay gateway : gateways) gateway.getWorld().spawnParticle(Particle.REVERSE_PORTAL, gateway.getLocation(), 2, .3, .3, .3, .03);
        }
        if (skillAge == telegraphAt + ThousandEyesTuning.GATEWAY_TELEGRAPH_TICKS) {
            for (BlockDisplay gateway : new ArrayList<>(gateways)) {
                damageSphere(gateway.getLocation(), ThousandEyesTuning.GATEWAY_RADIUS, ThousandEyesTuning.GATEWAY_DAMAGE);
                burst(gateway.getLocation(), ThousandEyesTuning.BURST_PARTICLES); gateway.remove();
            }
            gateways.clear(); recover();
        }
    }

    private void prepareScatter() {
        scatterOrder = ThousandEyesMath.shuffledOrder(random.nextLong(), ThousandEyesTuning.SATELLITE_COUNT);
        for (int i = 0; i < ThousandEyesTuning.SATELLITE_COUNT; i++) lockedTargets.add(null);
        for (int slot = 0; slot < ThousandEyesTuning.SATELLITE_COUNT; slot++) {
            int irregular = scatterOrder.get(slot); double angle = irregular * Math.PI * 2 / 9.0 + random.nextDouble() * .32;
            double radius = 4.0 + random.nextDouble() * 3.0, height = .7 + random.nextDouble() * 3.2;
            scatterPositions.add(target.getLocation().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius));
        }
    }

    private void tickScatter() {
        int firstLock = ThousandEyesTuning.SCATTER_MOVE_TICKS;
        for (int ordinal = 0; ordinal < satellites.size(); ordinal++) {
            int lockTick = firstLock + ordinal * ThousandEyesTuning.SCATTER_FIRE_SPACING;
            int slot = scatterOrder.get(ordinal);
            if (skillAge == lockTick) lockedTargets.set(slot, target.getLocation().add((random.nextDouble()-.5)*.25, .8+(random.nextDouble()-.5)*.2, (random.nextDouble()-.5)*.25));
        }
        for (int i = 0; i < lockedTargets.size(); i++) {
            Location locked = lockedTargets.get(i); if (locked == null) continue;
            int fireTick = firstLock + scatterOrder.indexOf(i) * ThousandEyesTuning.SCATTER_FIRE_SPACING + ThousandEyesTuning.SCATTER_CHARGE_TICKS;
            ItemDisplay eye = satellites.get(i);
            faceDisplay(eye, locked.toVector().subtract(eye.getLocation().toVector()));
            if (skillAge < fireTick) eye.getWorld().spawnParticle(Particle.WITCH, eye.getLocation(), 1, .06, .06, .06, 0);
            if (skillAge == fireTick) fireRay(eye.getLocation(), locked.toVector().subtract(eye.getLocation().toVector()), ThousandEyesTuning.SCATTER_RANGE, ThousandEyesTuning.SCATTER_DAMAGE);
        }
        int done = firstLock + 8 * ThousandEyesTuning.SCATTER_FIRE_SPACING + ThousandEyesTuning.SCATTER_CHARGE_TICKS + 2;
        if (skillAge >= done) recover();
    }

    private void tickRecording() {
        if ((skillAge - 1) % ThousandEyesTuning.MARKER_INTERVAL_TICKS == 0 && markers.size() < 9) {
            markers.add(target.getLocation().add(0, .75, 0)); burst(markers.getLast(), 5);
            if (markers.size() == 9) { state = ThousandEyesState.PATH_DASH; skillAge = 0; dashIndex = 0; dashStart = anchor.getLocation(); dashHits.clear(); }
        }
        if (skillAge % 5 == 0) for (Location marker : markers) hazard(marker);
        if (skillAge % 20 == 0) for (Location marker : markers) damageSphere(marker, ThousandEyesTuning.HAZARD_RADIUS, ThousandEyesTuning.HAZARD_DAMAGE);
    }

    private void tickDash() {
        if (dashIndex >= markers.size()) { recover(); return; }
        Location destination = markers.get(dashIndex); Vector segment = destination.toVector().subtract(dashStart.toVector()); face(segment);
        double p = Math.min(1.0, (skillAge + 1.0) / ThousandEyesTuning.DASH_SEGMENT_TICKS);
        Location next = dashStart.clone().add(segment.multiply(p));
        Location previous = anchor.getLocation(); anchor.teleport(next); damageCorridor(previous, next); next.getWorld().spawnParticle(Particle.DRAGON_BREATH, next, 2, .12, .12, .12, .01);
        for (int i = dashIndex; i < markers.size(); i++) if (skillAge % 5 == 0) hazard(markers.get(i));
        if (p >= 1.0) {
            burst(destination, 6); dashIndex++; skillAge = 0; dashStart = destination.clone(); dashHits.clear();
            if (dashIndex >= markers.size()) recover();
        }
    }

    private void updateRig() {
        Location center = anchor.getLocation(); Vector3f c = vector(center), f = vector(forward);
        double charge = state == ThousandEyesState.CENTRAL_LASER_CHARGE ? Math.min(1, skillAge / (double) ThousandEyesTuning.CENTRAL_CHARGE_TICKS) : 0;
        double release = state == ThousandEyesState.CENTRAL_LASER_RELEASE ? Math.min(1, skillAge / (double) ThousandEyesTuning.CENTRAL_RELEASE_TICKS) : 0;
        double innerSpeed = state == ThousandEyesState.CENTRAL_LASER_CHARGE ? ThousandEyesMath.lerp(ThousandEyesTuning.W_NORMAL, ThousandEyesTuning.W_CHARGE, charge)
                : state == ThousandEyesState.CENTRAL_LASER_RELEASE ? ThousandEyesMath.lerp(ThousandEyesTuning.W_RELEASE, ThousandEyesTuning.W_NORMAL, release) : ThousandEyesTuning.W_NORMAL;
        innerAngle += innerSpeed;
        centralEye.teleport(center); faceDisplay(centralEye, forward);
        for (int i = 0; i < inner.size(); i++) {
            double phase = i * Math.PI * 2 / inner.size(), radius = ThousandEyesMath.lerp(ThousandEyesTuning.INNER_RADIUS, .72, charge);
            Vector3f orbitCenter = new Vector3f(c).add(new Vector3f(f).mul((float) (ThousandEyesTuning.FRONT_OFFSET * charge)));
            Vector3f pose = ThousandEyesMath.orbit(orbitCenter, f, innerAngle + phase, radius, (i%3-1)*.28);
            BlockDisplay display = inner.get(i); display.teleport(location(center.getWorld(), pose));
            Vector3f normal = charge > .01 ? new Vector3f(f).lerp(new Vector3f(c).sub(pose).normalize(), (float)(1-charge)) : new Vector3f(c).sub(pose);
            transform(display, normal, i % 2 == 0);
        }
        double outerFocus = state == ThousandEyesState.CENTRAL_LASER_CHARGE ? Math.max(0, (charge-.65)/.35) : 0;
        double outerSpeed = ThousandEyesMath.lerp(ThousandEyesTuning.OUTER_W_NORMAL, ThousandEyesTuning.OUTER_W_CHARGE, outerFocus);
        if (state == ThousandEyesState.CENTRAL_LASER_RELEASE) outerSpeed = ThousandEyesMath.lerp(-.14, ThousandEyesTuning.OUTER_W_NORMAL, release);
        outerAngle += outerSpeed;
        for (int i=0;i<outer.size();i++) { double radius=ThousandEyesMath.lerp(ThousandEyesTuning.OUTER_RADIUS,2.55,outerFocus);
            Vector3f pose=ThousandEyesMath.orbit(c,f,outerAngle+i*Math.PI*2/outer.size(),radius,(i%2)*.55-.15); outer.get(i).teleport(location(center.getWorld(),pose)); transform(outer.get(i),new Vector3f(c).sub(pose),false); }
        updateSatellites(center, c, f, charge);
    }

    private void updateSatellites(Location center, Vector3f c, Vector3f f, double charge) {
        satelliteAngle += .035;
        for (int i=0;i<satellites.size();i++) {
            ItemDisplay eye=satellites.get(i); Location destination;
            if (state == ThousandEyesState.SCATTER_EYES) destination=scatterPositions.get(i);
            else if ((state==ThousandEyesState.PATH_RECORDING || state==ThousandEyesState.PATH_DASH) && i<markers.size()) destination=markers.get(i);
            else { double radius=(ThousandEyesTuning.SATELLITE_RADIUS+(i%3)*.22)*(1-.42*charge);
                Vector3f pose=ThousandEyesMath.orbit(c,f,satelliteAngle+i*Math.PI*2/9+i*.13,radius,(i%4-1.5)*.38); destination=location(center.getWorld(),pose); }
            Location now=eye.getLocation(); double factor=(state==ThousandEyesState.RECOVERING ? .24 : .32); eye.teleport(now.add(destination.toVector().subtract(now.toVector()).multiply(factor)));
            if (!(state==ThousandEyesState.SCATTER_EYES && i<lockedTargets.size() && lockedTargets.get(i)!=null)) faceDisplay(eye, state==ThousandEyesState.PATH_RECORDING||state==ThousandEyesState.PATH_DASH ? forward : target.getEyeLocation().toVector().subtract(eye.getLocation().toVector()));
            if (age % ThousandEyesTuning.ORBIT_PARTICLE_INTERVAL == i%ThousandEyesTuning.ORBIT_PARTICLE_INTERVAL) eye.getWorld().spawnParticle(Particle.PORTAL,eye.getLocation(),1,.03,.03,.03,0);
        }
    }

    private void recover(){ state=ThousandEyesState.RECOVERING; skillAge=0; gateways.forEach(Entity::remove); gateways.clear(); markers.clear(); }
    private void idle(){ state=ThousandEyesState.IDLE; skillAge=0; lockedTargets.clear(); scatterPositions.clear(); dashHits.clear(); }
    private void face(Vector value){ value.setY(0); if(value.lengthSquared()>.001) forward=value.normalize(); }

    private void fireRay(Location origin, Vector direction, double range, double damage) {
        Vector d=direction.clone().normalize(); World world=origin.getWorld(); world.playSound(origin,Sound.ENTITY_WARDEN_SONIC_BOOM,1.2F,.9F);
        for(double distance=.5;distance<=range;distance+=1.0) world.spawnParticle(Particle.SONIC_BOOM,origin.clone().add(d.clone().multiply(distance)),1,0,0,0,0);
        for(Player player:world.getPlayers()) if(distanceToSegment(player.getLocation().add(0,1,0).toVector(),origin.toVector(),origin.toVector().add(d.multiply(range)))<=ThousandEyesTuning.RAY_HIT_RADIUS) player.damage(damage,anchor);
    }
    private void damageSphere(Location center,double radius,double damage){ for(Player player:center.getWorld().getPlayers()) if(player.getLocation().distanceSquared(center)<=radius*radius) player.damage(damage,anchor); }
    private void damageCorridor(Location from,Location to){ for(Player player:from.getWorld().getPlayers()) if(!dashHits.contains(player.getUniqueId())&&distanceToSegment(player.getLocation().toVector(),from.toVector(),to.toVector())<=ThousandEyesTuning.DASH_HIT_RADIUS){ player.damage(ThousandEyesTuning.DASH_DAMAGE,anchor); dashHits.add(player.getUniqueId()); } }
    static double distanceToSegment(Vector p,Vector a,Vector b){ Vector ab=b.clone().subtract(a); if(ab.lengthSquared()<1e-8)return p.distance(a); double t=Math.max(0,Math.min(1,p.clone().subtract(a).dot(ab)/ab.lengthSquared())); return p.distance(a.clone().add(ab.multiply(t))); }
    private void hazard(Location at){ for(int i=0;i<ThousandEyesTuning.HAZARD_CIRCLE_POINTS;i++){double a=i*Math.PI*2/ThousandEyesTuning.HAZARD_CIRCLE_POINTS;at.getWorld().spawnParticle(Particle.DUST,at.clone().add(Math.cos(a)*ThousandEyesTuning.HAZARD_RADIUS,-.7,Math.sin(a)*ThousandEyesTuning.HAZARD_RADIUS),1,0,0,0,new Particle.DustOptions(Color.fromRGB(155,55,210),1));} }
    private void burst(Location at,int count){at.getWorld().spawnParticle(Particle.WITCH,at,count,.45,.45,.45,.08);}

    private ItemDisplay item(Location at,float scale){return at.getWorld().spawn(at,ItemDisplay.class,d->{d.setItemStack(new ItemStack(Material.ENDER_EYE));d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);configure(d);d.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale),new Quaternionf()));});}
    private BlockDisplay block(Location at,Material material,Vector3f scale){return at.getWorld().spawn(at,BlockDisplay.class,d->{d.setBlock(material.createBlockData());configure(d);d.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),scale,new Quaternionf()));});}
    private void configure(Display d){d.setPersistent(false);d.setBillboard(Display.Billboard.FIXED);d.setViewRange(48);d.setTeleportDuration(2);d.setInterpolationDuration(2);}
    private void faceDisplay(Display display,Vector direction){Transformation old=display.getTransformation();display.setTransformation(new Transformation(old.getTranslation(),ThousandEyesMath.facing(vector(direction),false),old.getScale(),old.getRightRotation()));}
    private void transform(BlockDisplay display,Vector3f normal,boolean slab){Transformation old=display.getTransformation();display.setTransformation(new Transformation(old.getTranslation(),ThousandEyesMath.facing(normal,slab),old.getScale(),old.getRightRotation()));}
    private static Vector3f vector(Location l){return new Vector3f((float)l.getX(),(float)l.getY(),(float)l.getZ());} private static Vector3f vector(Vector v){return new Vector3f((float)v.getX(),(float)v.getY(),(float)v.getZ());}
    private static Location location(World w,Vector3f v){return new Location(w,v.x,v.y,v.z);}

    public void remove(){if(task!=null){task.cancel();task=null;} List<Entity> all=new ArrayList<>();if(centralEye!=null)all.add(centralEye);all.addAll(inner);all.addAll(outer);all.addAll(satellites);all.addAll(gateways);if(anchor!=null)all.add(anchor);all.forEach(e->{if(e!=null&&e.isValid())e.remove();});inner.clear();outer.clear();satellites.clear();gateways.clear();markers.clear();lockedTargets.clear();scatterPositions.clear();anchor=null;centralEye=null;target=null;state=ThousandEyesState.IDLE;}
    @EventHandler public void onDeath(EntityDeathEvent event){if(anchor!=null&&(event.getEntity().equals(anchor)||event.getEntity().equals(target)))remove();}
    @EventHandler public void onWorldUnload(WorldUnloadEvent event){if(anchor!=null&&anchor.getWorld().equals(event.getWorld()))remove();}
}
