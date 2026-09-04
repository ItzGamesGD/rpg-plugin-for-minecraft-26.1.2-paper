package com.hyunseo.hyunseorpg.special.solaris;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.*;

/** Paper runtime adapter for Holy Sword: Solaris. All generated damage uses CombatService. */
public final class SolarisListener implements Listener {
    public static final String ID="solaris";
    private static final String POINT_CD="solaris:one_point", WHEEL_CD="solaris:heavenly_wheel", JUDGMENT_CD="solaris:judgment";
    private final JavaPlugin plugin; private final SpecialEquipmentService specials; private final EquipmentInstanceService instances;
    private final CombatService combat; private final CooldownService cooldown; private final SolarisConfig config;
    private final Map<UUID,Long> dawnOrigins=new HashMap<>(); private final Map<UUID,Deque<Long>> kills=new HashMap<>();
    private final Map<UUID,DamageMark> damageMarks=new HashMap<>(); private final Map<UUID,BukkitTask> holds=new HashMap<>();
    private final Map<UUID,Set<Session>> sessions=new HashMap<>();

    public SolarisListener(JavaPlugin plugin, ConfigService configs, SpecialEquipmentService specials,
                           EquipmentInstanceService instances, CombatService combat, CooldownService cooldown) {
        this.plugin=plugin; this.specials=specials; this.instances=instances; this.combat=combat; this.cooldown=cooldown;
        this.config=SolarisConfig.from(configs);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onF(PlayerSwapHandItemsEvent e){ if(!holding(e.getPlayer())) return; e.setCancelled(true); if(!e.getPlayer().isSneaking()) onePoint(e.getPlayer()); }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onRightClick(PlayerInteractEvent e){ if(e.getHand()!=EquipmentSlot.HAND || !holding(e.getPlayer())) return;
        if(e.getAction()!=Action.RIGHT_CLICK_AIR && e.getAction()!=Action.RIGHT_CLICK_BLOCK) return; e.setCancelled(true); judgment(e.getPlayer()); }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onMelee(EntityDamageByEntityEvent e){ if(combat.isInternalDamage() || !(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity t) || !holding(p)) return;
        mark(p,t); dawn(p,t); }

    private void dawn(Player owner, LivingEntity origin){
        long now=System.currentTimeMillis(); long reset=config.dawnResetTicks()*50L;
        ArrayDeque<LivingEntity> queue=new ArrayDeque<>(); if(SolarisLogic.claimDawn(dawnOrigins,origin.getUniqueId(),now,reset)) queue.add(origin);
        Set<UUID> propagation=new HashSet<>();
        while(!queue.isEmpty()) { LivingEntity center=queue.remove(); if(!propagation.add(center.getUniqueId())) continue;
            Location at=center.getLocation().add(0,.8,0); center.getWorld().spawnParticle(Particle.END_ROD,at,28,config.dawnRadius()/2,.5,config.dawnRadius()/2,.04);
            center.getWorld().playSound(at,Sound.BLOCK_AMETHYST_BLOCK_CHIME,.7f,1.7f);
            for(Entity entity:center.getWorld().getNearbyEntities(center.getLocation(),config.dawnRadius(),config.dawnRadius(),config.dawnRadius())) {
                if(!(entity instanceof LivingEntity target)||!validTarget(owner,target)||target==center) continue;
                mark(owner,target); combat.applyMultiHitDamage(owner,target,config.dawnDamage()); target.setFireTicks(Math.max(target.getFireTicks(),config.dawnFireTicks()));
                if(SolarisLogic.claimDawn(dawnOrigins,target.getUniqueId(),now,reset)) queue.add(target);
            }
        }
    }

    private void onePoint(Player owner){ UUID id=owner.getUniqueId(); if(cooldown.isOnCooldown(id,POINT_CD)) return;
        LivingEntity target=nearby(owner,config.pointRange()).stream().findFirst().orElse(null); if(target==null) return;
        cooldown.startCooldownTicks(id,POINT_CD,config.pointCooldownTicks()); Session s=session(owner);
        Location above=target.getLocation().add(0,5,0); target.getWorld().spawnParticle(Particle.WAX_ON,above,25,.7,.2,.7,.02);
        later(s,config.pointTelegraphTicks(),()->dropSword(s,target,1.8,config.pointFallTicks(),config.pointDamage()));
    }

    @EventHandler public void onSneak(PlayerToggleSneakEvent e){ UUID id=e.getPlayer().getUniqueId(); cancelHold(id);
        if(!e.isSneaking()||!holding(e.getPlayer())) return; UUID item=instances.ensure(e.getPlayer().getInventory().getItemInMainHand());
        holds.put(id,Bukkit.getScheduler().runTaskLater(plugin,()->{holds.remove(id); if(e.getPlayer().isSneaking()&&validOwner(e.getPlayer(),item)) heavenlyWheel(e.getPlayer());},config.wheelHoldTicks())); }

    private void heavenlyWheel(Player owner){ UUID id=owner.getUniqueId(); if(cooldown.isOnCooldown(id,WHEEL_CD)) return;
        Deque<Long> history=kills.computeIfAbsent(id,k->new ArrayDeque<>()); SolarisLogic.pruneKills(history,System.currentTimeMillis(),config.killWindowMillis());
        int count=SolarisLogic.orbCount(history.size(),config.orbMultiplier(),config.maximumOrbs()); if(count==0) return;
        cooldown.startCooldownTicks(id,WHEEL_CD,config.wheelCooldownTicks()); Session s=session(owner); List<BlockDisplay> orbs=new ArrayList<>();
        for(int n=0;n<count;n++){ BlockDisplay d=blockDisplay(owner.getLocation(),Material.OCHRE_FROGLIGHT,.35f); s.entities.add(d); orbs.add(d); }
        final int[] age={0}; BukkitTask orbit=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            if(!active(s)){cleanup(s);return;} double phase=age[0]*.18; Vector back=owner.getEyeLocation().getDirection().setY(0).normalize().multiply(-1.3);
            Location center=owner.getLocation().add(back).add(0,1.4,0); owner.getWorld().spawnParticle(Particle.END_ROD,center,4,.8,.8,.8,0);
            for(int n=0;n<orbs.size();n++){double a=phase+Math.PI*2*n/orbs.size(); orbs.get(n).teleport(center.clone().add(Math.cos(a)*1.4,Math.sin(a)*1.4,0));}
            if(++age[0]>=config.orbitTicks()){ if(s.tasks.removeIf(BukkitTask::isCancelled)){} startWheelStrikes(s,orbs); }
        },0,1); s.tasks.add(orbit); later(s,config.orbitTicks(),orbit::cancel);
    }

    private void startWheelStrikes(Session s,List<BlockDisplay> orbs){ List<SolarisLogic.Candidate<LivingEntity>> cs=nearby(s.owner,config.wheelRadius()).stream()
            .map(t->new SolarisLogic.Candidate<>(t,t.getLocation().distanceSquared(s.owner.getLocation()),validTarget(s.owner,t))).toList();
        List<LivingEntity> assigned=SolarisLogic.distribute(cs,orbs.size(),config.maximumHits());
        for(int n=0;n<orbs.size();n++){ int index=n; later(s,(long)n*config.swordIntervalTicks(),()->{ BlockDisplay orb=orbs.get(index); if(orb.isValid())orb.remove();
            if(index<assigned.size()) dropSword(s,assigned.get(index),.65,config.smallFallTicks(),config.smallSwordDamage()); }); }
        later(s,(long)orbs.size()*config.swordIntervalTicks()+config.smallFallTicks()+2,()->cleanup(s));
    }

    private void judgment(Player owner){ UUID id=owner.getUniqueId(); if(cooldown.isOnCooldown(id,JUDGMENT_CD))return;
        cooldown.startCooldownTicks(id,JUDGMENT_CD,config.judgmentCooldownTicks()); Session s=session(owner); Vector back=owner.getEyeLocation().getDirection().setY(0).normalize().multiply(-2);
        Location start=owner.getLocation().add(back).add(0,1,0); BlockDisplay sun=blockDisplay(start,Material.MAGMA_BLOCK,.4f); s.entities.add(sun);
        final int[] age={0}; BukkitTask rise=Bukkit.getScheduler().runTaskTimer(plugin,()->{if(!active(s)){cleanup(s);return;} double f=++age[0]/(double)config.sunRiseTicks();
            sun.teleport(start.clone().add(0,f*10,0)); scale(sun,(float)(.4+f*3)); owner.getWorld().spawnParticle(Particle.FLAME,sun.getLocation(),5,1,1,1,.01);},1,1); s.tasks.add(rise);
        later(s,config.sunRiseTicks(),()->{rise.cancel(); for(LivingEntity t:nearby(owner,config.judgmentRadius()))t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,config.showerTicks(),0)); startMeteors(s);});
    }

    private void startMeteors(Session s){ List<SolarisLogic.MeteorOffset> offsets=SolarisLogic.meteorOffsets(s.owner.getUniqueId().getLeastSignificantBits()^System.nanoTime(),config.meteorCount(),config.judgmentRadius());
        List<Integer> schedule=SolarisLogic.scheduleTicks(offsets.size(),config.showerTicks()); Location center=s.owner.getLocation();
        for(int n=0;n<offsets.size();n++){ SolarisLogic.MeteorOffset o=offsets.get(n); later(s,schedule.get(n),()->meteor(s,center.clone().add(o.x(),18,o.z()))); }
        later(s,config.showerTicks()+config.meteorFallTicks()+5,()->cleanup(s));
    }
    private void meteor(Session s,Location spawn){ if(!active(s))return; BlockDisplay meteor=blockDisplay(spawn,Material.MAGMA_BLOCK,.55f);s.entities.add(meteor);
        Location ground=spawn.clone(); while(ground.getY()>ground.getWorld().getMinHeight()+1 && ground.getBlock().isPassable())ground.subtract(0,1,0); ground.add(0,1,0);
        for(int i=1;i<=config.meteorFallTicks();i++){int step=i; later(s,i,()->{if(meteor.isValid())meteor.teleport(spawn.clone().add(0,(ground.getY()-spawn.getY())*step/config.meteorFallTicks(),0));});}
        later(s,config.meteorFallTicks(),()->{if(meteor.isValid())meteor.remove(); impact(s,ground);}); }
    private void impact(Session s,Location at){ World w=at.getWorld(); w.spawnParticle(Particle.EXPLOSION,at,3,.4,.2,.4,0);w.playSound(at,Sound.ENTITY_GENERIC_EXPLODE,1,.8f);
        for(Entity e:w.getNearbyEntities(at,config.impactRadius(),config.impactRadius(),config.impactRadius()))if(e instanceof LivingEntity t&&validTarget(s.owner,t)){
            double damage=t.getLocation().distanceSquared(at)<.8?config.meteorDirectDamage():config.explosionDamage();mark(s.owner,t);combat.applyMultiHitDamage(s.owner,t,damage);}
        for(int[] d:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1}}){Block b=at.clone().add(d[0],0,d[1]).getBlock();if(b.getType().isAir()&&!b.getRelative(0,-1,0).isPassable()){b.setType(Material.FIRE,false);s.fire.add(b);later(s,config.judgmentFireTicks(),()->{if(b.getType()==Material.FIRE)b.setType(Material.AIR,false);s.fire.remove(b);});}}
    }

    private void dropSword(Session s,LivingEntity target,double size,int fallTicks,double damage){if(!active(s)||!validTarget(s.owner,target))return; Location top=target.getLocation().add(0,5,0);
        ItemDisplay sword=top.getWorld().spawn(top,ItemDisplay.class,d->d.setItemStack(new ItemStack(Material.GOLDEN_SWORD)));scale(sword,(float)size);s.entities.add(sword);
        for(int i=1;i<=fallTicks;i++){int step=i;later(s,i,()->{if(sword.isValid())sword.teleport(top.clone().subtract(0,4.5*step/fallTicks,0));});}
        later(s,fallTicks,()->{if(sword.isValid())sword.remove();if(validTarget(s.owner,target)){mark(s.owner,target);combat.applySkillDamage(s.owner,target,damage);Location p=target.getLocation().add(0,.8,0);p.getWorld().spawnParticle(Particle.FLASH,p,1);p.getWorld().playSound(p,Sound.BLOCK_ANVIL_LAND,1,1.5f);}}); }

    @EventHandler(priority=EventPriority.MONITOR) public void onDeath(EntityDeathEvent e){ DamageMark m=damageMarks.remove(e.getEntity().getUniqueId()); Player killer=e.getEntity().getKiller();
        if(m!=null&&killer!=null&&killer.getUniqueId().equals(m.owner)&&System.currentTimeMillis()-m.at<5000&&instances.is(killer.getInventory().getItemInMainHand(),m.instance)) kills.computeIfAbsent(m.owner,k->new ArrayDeque<>()).addLast(System.currentTimeMillis()); }
    private void mark(Player p,LivingEntity t){damageMarks.put(t.getUniqueId(),new DamageMark(p.getUniqueId(),instances.ensure(p.getInventory().getItemInMainHand()),System.currentTimeMillis()));}
    private List<LivingEntity> nearby(Player p,double r){return p.getWorld().getNearbyEntities(p.getLocation(),r,r,r).stream().filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast).filter(t->validTarget(p,t)).sorted(Comparator.comparingDouble(t->t.getLocation().distanceSquared(p.getLocation()))).toList();}
    private boolean validTarget(Player p,LivingEntity t){return t!=p&&t.isValid()&&!t.isDead()&&t.getWorld()==p.getWorld();} private boolean holding(Player p){return specials.getSpecialId(p.getInventory().getItemInMainHand()).equals(ID);}
    private boolean validOwner(Player p,UUID item){return p.isOnline()&&!p.isDead()&&holding(p)&&instances.is(p.getInventory().getItemInMainHand(),item);} private boolean active(Session s){return !s.closed&&validOwner(s.owner,s.instance);}
    private Session session(Player p){Session s=new Session(p,instances.ensure(p.getInventory().getItemInMainHand()));sessions.computeIfAbsent(p.getUniqueId(),x->new HashSet<>()).add(s);return s;}
    private void later(Session s,long ticks,Runnable run){BukkitTask t=Bukkit.getScheduler().runTaskLater(plugin,()->{if(active(s))run.run();},Math.max(0,ticks));s.tasks.add(t);}
    private BlockDisplay blockDisplay(Location l,Material m,float scale){BlockDisplay d=l.getWorld().spawn(l,BlockDisplay.class,x->x.setBlock(m.createBlockData()));scale(d,scale);return d;}
    private void scale(Display d,float v){Transformation t=d.getTransformation();t.getScale().set(new Vector3f(v));d.setTransformation(t);}
    private void cancelHold(UUID id){BukkitTask t=holds.remove(id);if(t!=null)t.cancel();}
    private void cleanup(Session s){if(s.closed)return;s.closed=true;s.tasks.forEach(BukkitTask::cancel);s.entities.forEach(e->{if(e.isValid())e.remove();});s.fire.forEach(b->{if(b.getType()==Material.FIRE)b.setType(Material.AIR,false);});Set<Session> set=sessions.get(s.owner.getUniqueId());if(set!=null){set.remove(s);if(set.isEmpty())sessions.remove(s.owner.getUniqueId());}}
    public void cleanup(UUID id){cancelHold(id);Set<Session> set=sessions.get(id);if(set!=null)new HashSet<>(set).forEach(this::cleanup);}
    @EventHandler public void quit(PlayerQuitEvent e){cleanup(e.getPlayer().getUniqueId());} @EventHandler public void death(PlayerDeathEvent e){cleanup(e.getPlayer().getUniqueId());}
    @EventHandler public void world(PlayerChangedWorldEvent e){cleanup(e.getPlayer().getUniqueId());} @EventHandler public void held(PlayerItemHeldEvent e){cleanup(e.getPlayer().getUniqueId());}
    public void shutdown(){new HashSet<>(sessions.keySet()).forEach(this::cleanup);holds.values().forEach(BukkitTask::cancel);holds.clear();dawnOrigins.clear();damageMarks.clear();kills.clear();}
    private record DamageMark(UUID owner,UUID instance,long at){} private static final class Session{final Player owner;final UUID instance;final Set<Entity> entities=new HashSet<>();final Set<BukkitTask> tasks=new HashSet<>();final Set<Block> fire=new HashSet<>();boolean closed;Session(Player p,UUID i){owner=p;instance=i;}}
}
