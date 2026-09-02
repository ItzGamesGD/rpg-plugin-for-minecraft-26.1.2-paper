package com.hyunseo.hyunseorpg.special.thanatos;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Thanatos combat runtime. Timers/coordinates, never display collision, are gameplay authority. */
public final class ThanatosMaceListener implements Listener {
    public static final String ID = "thanatos_mace";
    private static final String OPPRESSION_CD="thanatos:oppression", SENTENCE_CD="thanatos:sentence", ULTIMATUM_CD="thanatos:ultimatum";
    private final JavaPlugin plugin; private final SpecialEquipmentService specials; private final EquipmentInstanceService instances;
    private final CombatService combat; private final CooldownService cooldowns; private final ThanatosConfig config;
    private final ThanatosState state = new ThanatosState(); private final Map<UUID,MortalRuntime> mortals=new HashMap<>();
    private final Map<UUID,UltimatumRuntime> ultimatums=new HashMap<>(); private final Map<UUID,Ground> grounds=new HashMap<>();
    private final Map<BlockKey,Mutation> mutations=new HashMap<>(); private long tick; private final BukkitTask ticker;

    public ThanatosMaceListener(JavaPlugin plugin, ConfigService configService, SpecialEquipmentService specials,
                                EquipmentInstanceService instances, CombatService combat, CooldownService cooldowns) {
        this.plugin=plugin; this.specials=specials; this.instances=instances; this.combat=combat; this.cooldowns=cooldowns;
        this.config=ThanatosConfig.from(configService); this.ticker=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
    }

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player owner) || !(event.getEntity() instanceof LivingEntity target)
                || !holding(owner) || combat.isInternalDamage()) return;
        applyMortal(owner,target); target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,target.getLocation().add(0,.8,0),5,.2,.25,.2,.01);
        target.getWorld().playSound(target.getLocation(),Sound.BLOCK_HEAVY_CORE_PLACE,.65f,.65f);
        // The resolved Paper API exposes the vanilla smash DamageType, but not a controllable/resizable client effect.
        if (event.getDamageSource().getDamageType().equals(DamageType.MACE_SMASH)
                && !cooldowns.isOnCooldown(owner.getUniqueId(),OPPRESSION_CD)) {
            cooldowns.startCooldownTicks(owner.getUniqueId(),OPPRESSION_CD,config.oppressionCooldownTicks());
            oppression(owner,target.getLocation(),config.oppressionRadius(),config.oppressionDamage());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void onF(PlayerSwapHandItemsEvent event) {
        Player owner=event.getPlayer(); if (owner.isSneaking() || !holding(owner)) return; event.setCancelled(true);
        if (cooldowns.isOnCooldown(owner.getUniqueId(),SENTENCE_CD)) return;
        LivingEntity target=selectTarget(owner); if (target==null) return;
        cooldowns.startCooldownTicks(owner.getUniqueId(),SENTENCE_CD,config.sentenceCooldownTicks());
        // Contract: impact center locks to the target's position at selection; later movement/teleport does not retarget it.
        Location center=target.getLocation().clone(); ItemDisplay display=spawnDisplay(center.clone().add(0,12,0),new ItemStack(Material.HEAVY_CORE),2.5f,false);
        BukkitTask task=new BukkitRunnable(){int age; public void run(){ age++; double p=Math.min(1,age/(double)config.sentenceFallTicks());
            if(display.isValid()) display.teleport(center.clone().add(0,12*(1-p)+.5,0));
            center.getWorld().spawnParticle(Particle.SOUL,center.clone().add(0,12*(1-p),0),3,.35,.2,.35,0);
            if(age>=config.sentenceFallTicks()){ cancel(); impactSentence(owner,center); }
        }}.runTaskTimer(plugin,1,1); sentenceTasks.put(task,display);
    }
    private final Map<BukkitTask,ItemDisplay> sentenceTasks=new HashMap<>();

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void onRightClick(PlayerInteractEvent event) {
        if(event.getHand()!=EquipmentSlot.HAND || (event.getAction()!=Action.RIGHT_CLICK_AIR&&event.getAction()!=Action.RIGHT_CLICK_BLOCK)) return;
        Player owner=event.getPlayer(); if(!holding(owner)) return; event.setCancelled(true); UUID id=owner.getUniqueId();
        if(cooldowns.isOnCooldown(id,ULTIMATUM_CD)||state.hasUltimatum(id)) return;
        UUID instance=instances.ensure(owner.getInventory().getItemInMainHand());
        if(state.beginUltimatum(id,owner.getWorld().getUID(),tick,config.chargeTicks())) {
            ultimatums.put(id,new UltimatumRuntime(owner,instance,owner.getWorld().getUID(),tick)); cooldowns.startCooldownTicks(id,ULTIMATUM_CD,config.ultimatumCooldownTicks());
        }
    }

    private void tick(){ tick++;
        for(MortalRuntime m:new ArrayList<>(mortals.values())) tickMortal(m);
        for(UltimatumRuntime u:new ArrayList<>(ultimatums.values())) tickUltimatum(u);
        for(Ground g:new ArrayList<>(grounds.values())) tickGround(g);
        for(var entry:new ArrayList<>(sentenceTasks.entrySet())) if(entry.getKey().isCancelled()) { if(entry.getValue().isValid())entry.getValue().remove(); sentenceTasks.remove(entry.getKey()); }
    }
    private void tickMortal(MortalRuntime m){ LivingEntity target=entity(m.target); if(target==null||target.isDead()||target.getWorld().getUID()!=m.world){cleanupMortal(m.target);return;}
        if(m.display!=null&&m.display.isValid()) m.display.teleport(target.getLocation().add(0,target.getHeight()+1.8,0));
        if(!state.mortalDue(m.target,tick)) return;
        Location impact=target.getLocation().add(0,target.getHeight()*.65,0); Player owner=Bukkit.getPlayer(m.owner);
        if(owner!=null&&owner.isOnline()) combat.applySkillDamage(owner,target,config.mortalDamage()); else target.damage(config.mortalDamage());
        impact.getWorld().spawnParticle(Particle.SQUID_INK,impact,18,.3,.4,.3,.04); impact.getWorld().playSound(impact,Sound.BLOCK_ANVIL_LAND,.9f,.55f); cleanupMortal(m.target);
    }
    private void applyMortal(Player owner,LivingEntity target){ if(!state.beginMortal(target.getUniqueId(),tick,config.mortalDelayTicks()))return;
        ItemStack sword=new ItemStack(Material.NETHERITE_SWORD); ItemMeta meta=sword.getItemMeta(); meta.setCustomModelData(config.mortalModel()); sword.setItemMeta(meta);
        ItemDisplay display=spawnDisplay(target.getLocation().add(0,target.getHeight()+1.8,0),sword,1f,true);
        mortals.put(target.getUniqueId(),new MortalRuntime(target.getUniqueId(),owner.getUniqueId(),target.getWorld().getUID(),display));
    }
    private void cleanupMortal(UUID id){ MortalRuntime m=mortals.remove(id); state.endMortal(id); if(m!=null&&m.display!=null&&m.display.isValid())m.display.remove(); }

    private void oppression(Player owner,Location center,double radius,double damage){ center.getWorld().playSound(center,Sound.ITEM_MACE_SMASH_GROUND_HEAVY,1.2f,.6f);
        center.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,center.clone().add(0,3,0),35,radius*.5,1,radius*.5,.04);
        for(LivingEntity target:hostiles(center,radius)){ if(damage>0)combat.applySkillDamage(owner,target,damage); applyPressure(target,config.oppressionDurationTicks(),center,null); }
    }
    private void applyPressure(LivingEntity target,int duration,Location center,Ground ground){ target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,duration,5,false,false,true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,duration,128,false,false,true)); if(target.getVelocity().getY()>-.65)target.setVelocity(target.getVelocity().setY(-.65));
        if(ground!=null) ground.targets.add(target.getUniqueId()); }

    private LivingEntity selectTarget(Player owner){ Location eye=owner.getEyeLocation(); List<ThanatosTargeting.Candidate<LivingEntity>> candidates=new ArrayList<>();
        for(Entity e:owner.getWorld().getNearbyEntities(owner.getLocation(),config.sentenceRange(),config.sentenceRange(),config.sentenceRange())) if(e instanceof LivingEntity l&&hostile(owner,l)){
            Vector d=l.getBoundingBox().getCenter().subtract(eye.toVector()); candidates.add(new ThanatosTargeting.Candidate<>(l,d.getX(),d.getY(),d.getZ()));}
        Vector f=eye.getDirection(); List<LivingEntity> valid=ThanatosTargeting.forward(candidates,f.getX(),f.getY(),f.getZ(),config.sentenceRange(),config.sentenceCosine());
        return ThanatosTargeting.random(valid,ThreadLocalRandom.current()); }
    private void impactSentence(Player owner,Location center){ center.getWorld().playSound(center,Sound.BLOCK_HEAVY_CORE_PLACE,2,.45f); center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER,center,1);
        Ground ground=createGround(center); for(LivingEntity target:hostiles(center,config.groundRadius())){ applyMortal(owner,target); target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,config.witherDurationTicks(),config.witherAmplifier())); applyPressure(target,config.groundDurationTicks(),center,ground); }
    }

    private Ground createGround(Location center){ Ground g=new Ground(UUID.randomUUID(),center.clone(),tick+config.groundDurationTicks()); grounds.put(g.id,g); int r=(int)Math.ceil(config.groundRadius());
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=config.groundRadius()*config.groundRadius()){
            Block b=center.getWorld().getHighestBlockAt(center.getBlockX()+x,center.getBlockZ()+z); if(Math.abs(b.getY()-center.getBlockY())>3)continue;
            BlockKey key=new BlockKey(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ()); Mutation m=mutations.get(key); if(m==null){m=new Mutation(b.getBlockData().clone(),1);mutations.put(key,m);b.setType(Material.SOUL_SAND,false);}else m.owners++; g.blocks.add(key); }
        return g; }
    private void tickGround(Ground g){ if(tick>=g.ends){cleanupGround(g.id);return;} for(UUID id:new HashSet<>(g.targets)){LivingEntity target=entity(id);if(target==null||target.getWorld()!=g.center.getWorld()){g.targets.remove(id);continue;}
        Vector horizontal=target.getLocation().toVector().subtract(g.center.toVector()).setY(0); if(horizontal.length()>config.groundRadius()*.82&&!target.isInsideVehicle()){
            Vector inward=horizontal.normalize().multiply(-.22); target.setVelocity(target.getVelocity().setX(inward.getX()).setZ(inward.getZ()).setY(Math.min(target.getVelocity().getY(),-.25))); }} }
    private void cleanupGround(UUID id){Ground g=grounds.remove(id);if(g==null)return;for(BlockKey key:g.blocks){Mutation m=mutations.get(key);if(m==null)continue;if(--m.owners>0)continue;mutations.remove(key);World w=Bukkit.getWorld(key.world);if(w!=null){Block b=w.getBlockAt(key.x,key.y,key.z);if(b.getType()==Material.SOUL_SAND)b.setBlockData(m.original,false);}}}

    private void tickUltimatum(UltimatumRuntime u){Player p=u.player;UUID id=p.getUniqueId();if(!valid(u)){cancel(id);return;} ThanatosState.UltimatumPhase phase=state.phase(id);
        if(phase==ThanatosState.UltimatumPhase.CHARGING){ inwardParticles(p);if(state.chargeDue(id,tick)){state.launch(id);p.getWorld().spawnParticle(Particle.GUST_EMITTER_SMALL,p.getLocation(),2);p.setVelocity(p.getVelocity().setY(config.launchVelocity()));}}
        else if(phase==ThanatosState.UltimatumPhase.LAUNCHED&&p.getVelocity().getY()<=0){state.beginFall(id);}
        else if(phase==ThanatosState.UltimatumPhase.FALLING&&state.land(id,p.getWorld().getUID(),p.isOnGround()||p.isInWater())){ultimatums.remove(id);ultimatumImpact(p);}}
    private void inwardParticles(Player p){for(int i=0;i<5;i++){double a=ThreadLocalRandom.current().nextDouble(Math.PI*2),r=2.2;Location l=p.getLocation().add(Math.cos(a)*r,ThreadLocalRandom.current().nextDouble(.2,2),Math.sin(a)*r);p.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,l,1,(p.getLocation().getX()-l.getX())*.12,(p.getLocation().getY()+1-l.getY())*.12,(p.getLocation().getZ()-l.getZ())*.12,1);}}
    private void ultimatumImpact(Player owner){Location c=owner.getLocation();c.getWorld().playSound(c,Sound.ITEM_MACE_SMASH_GROUND_HEAVY,2,.45f);c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER,c,3);
        for(int i=0;i<120;i++){double a=ThreadLocalRandom.current().nextDouble(Math.PI*2),r=ThreadLocalRandom.current().nextDouble(config.impactRadius());c.getWorld().spawnParticle(i%3==0?Particle.SOUL:Particle.SQUID_INK,c.clone().add(Math.cos(a)*r,.15,Math.sin(a)*r),1,0,0,0,0);}
        for(LivingEntity target:hostiles(c,config.impactRadius())){combat.applyUltimateDamage(owner,target,config.impactDamage());applyMortal(owner,target);applyPressure(target,config.oppressionDurationTicks(),c,null);}}

    private ItemDisplay spawnDisplay(Location at,ItemStack item,float scale,boolean sword){return at.getWorld().spawn(at,ItemDisplay.class,d->{d.setItemStack(item);d.setPersistent(false);d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);d.setBillboard(Display.Billboard.FIXED);d.setViewRange(48);Quaternionf q=new Quaternionf();if(sword)q.rotateZ((float)Math.PI);d.setTransformation(new Transformation(new Vector3f(),q,new Vector3f(scale),new Quaternionf()));});}
    private List<LivingEntity> hostiles(Location c,double r){List<LivingEntity> out=new ArrayList<>();for(Entity e:c.getWorld().getNearbyEntities(c,r,Math.max(4,r),r))if(e instanceof LivingEntity l&&!(l instanceof ArmorStand)&&!(l instanceof Player)&&!l.isDead())out.add(l);return out;}
    private boolean hostile(Player owner,LivingEntity target){return target!=owner&&!(target instanceof ArmorStand)&&!(target instanceof Player)&&!target.isDead()&&target.isValid();}
    private LivingEntity entity(UUID id){Entity e=Bukkit.getEntity(id);return e instanceof LivingEntity l&&l.isValid()?l:null;}
    private boolean holding(Player p){return specials.getSpecialId(p.getInventory().getItemInMainHand()).equals(ID);}
    private boolean valid(UltimatumRuntime u){return u.player.isOnline()&&!u.player.isDead()&&holding(u.player)&&instances.is(u.player.getInventory().getItemInMainHand(),u.instance)&&u.player.getWorld().getUID().equals(u.world)&&!u.player.isInsideVehicle();}
    private void cancel(UUID id){state.cancelUltimatum(id);ultimatums.remove(id);}
    @EventHandler public void quit(PlayerQuitEvent e){cancel(e.getPlayer().getUniqueId());} @EventHandler public void death(PlayerDeathEvent e){cancel(e.getPlayer().getUniqueId());}
    @EventHandler public void world(PlayerChangedWorldEvent e){cancel(e.getPlayer().getUniqueId());} @EventHandler public void held(PlayerItemHeldEvent e){Bukkit.getScheduler().runTask(plugin,()->{if(!holding(e.getPlayer()))cancel(e.getPlayer().getUniqueId());});}
    @EventHandler public void inventory(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p)Bukkit.getScheduler().runTask(plugin,()->{if(!holding(p))cancel(p.getUniqueId());});}
    public void shutdown(){ticker.cancel();new ArrayList<>(mortals.keySet()).forEach(this::cleanupMortal);new ArrayList<>(grounds.keySet()).forEach(this::cleanupGround);for(var e:sentenceTasks.entrySet()){e.getKey().cancel();if(e.getValue().isValid())e.getValue().remove();}sentenceTasks.clear();ultimatums.clear();state.clear();}

    private record MortalRuntime(UUID target,UUID owner,UUID world,ItemDisplay display){} private record UltimatumRuntime(Player player,UUID instance,UUID world,long start){}
    private static final class Ground{final UUID id;final Location center;final long ends;final Set<UUID>targets=new HashSet<>();final Set<BlockKey>blocks=new HashSet<>();Ground(UUID i,Location c,long e){id=i;center=c;ends=e;}}
    private record BlockKey(UUID world,int x,int y,int z){} private static final class Mutation{final BlockData original;int owners;Mutation(BlockData d,int o){original=d;owners=o;}}
}
