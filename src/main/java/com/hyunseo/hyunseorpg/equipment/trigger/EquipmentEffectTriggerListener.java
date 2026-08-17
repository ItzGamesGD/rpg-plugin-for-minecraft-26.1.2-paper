package com.hyunseo.hyunseorpg.equipment.trigger;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Converts Bukkit events into the shared equipment trigger context. */
public final class EquipmentEffectTriggerListener implements Listener {
    private final EquipmentEffectTriggerEngine engine;
    private final CombatService combat;
    private final EquipmentInstanceService equipmentInstances;

    public EquipmentEffectTriggerListener(EquipmentEffectTriggerEngine engine, CombatService combat,
                                          EquipmentInstanceService equipmentInstances) {
        this.engine = engine;
        this.combat = combat;
        this.equipmentInstances = equipmentInstances;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAttackAttempt(EntityDamageByEntityEvent event) {
        // CombatService marks plugin-generated damage as internal. It must not
        // become a new player attack input (notably for explosive mace chains).
        if (combat.isInternalDamage()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        equipmentInstances.ensure(held);
        engine.trigger(new TriggerContext(
                TriggerType.ATTACK_ATTEMPT, TriggerPhase.PRE, player, Instant.now(), event,
                UUID.randomUUID(), List.of(held), held, event.getEntity(),
                event.getEntity() instanceof LivingEntity living ? living : null,
                null, event.getDamage(), event.getFinalDamage(), combat.getActiveDamageContext()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttackHit(EntityDamageByEntityEvent event) {
        if (combat.isInternalDamage()) return;
        if (!(event.getDamager() instanceof Player player) || event.getFinalDamage() <= 0.0D) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        equipmentInstances.ensure(held);
        engine.trigger(new TriggerContext(
                TriggerType.ATTACK_HIT, TriggerPhase.CONFIRMED, player, Instant.now(), event,
                UUID.randomUUID(), List.of(held), held, event.getEntity(),
                event.getEntity() instanceof LivingEntity living ? living : null,
                null, event.getDamage(), event.getFinalDamage(), combat.getActiveDamageContext()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getFinalDamage() <= 0.0D) return;
        List<ItemStack> armor = new java.util.ArrayList<>(Arrays.stream(player.getInventory().getArmorContents()).toList());
        armor.add(player.getInventory().getItemInOffHand());
        armor.forEach(equipmentInstances::ensure);
        org.bukkit.entity.Entity attacker = event instanceof EntityDamageByEntityEvent byEntity
                ? byEntity.getDamager() : null;
        LivingEntity livingAttacker = attacker instanceof LivingEntity living ? living
                : attacker instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter ? shooter : null;
        engine.trigger(new TriggerContext(
                TriggerType.DAMAGED, TriggerPhase.CONFIRMED, player, Instant.now(), event,
                UUID.randomUUID(), armor, null,
                attacker, livingAttacker,
                null, event.getDamage(), event.getFinalDamage(), combat.getActiveDamageContext()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        ItemStack held = killer.getInventory().getItemInMainHand();
        equipmentInstances.ensure(held);
        engine.trigger(new TriggerContext(
                TriggerType.KILL, TriggerPhase.CONFIRMED, killer, Instant.now(), event,
                UUID.randomUUID(), List.of(held), held, event.getEntity(), event.getEntity(),
                null, 0.0D, 0.0D));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onGatherAttempt(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        equipmentInstances.ensure(held);
        engine.trigger(new TriggerContext(
                TriggerType.GATHER_ATTEMPT, TriggerPhase.PRE, player, Instant.now(), event,
                UUID.randomUUID(), List.of(held), held, null, null, event.getBlock(), 0.0D, 0.0D));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGatherSuccess(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        equipmentInstances.ensure(held);
        engine.trigger(new TriggerContext(
                TriggerType.GATHER_SUCCESS, TriggerPhase.CONFIRMED, player, Instant.now(), event,
                UUID.randomUUID(), List.of(held), held, null, null, event.getBlock(), 0.0D, 0.0D));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onElytraBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();
        ItemStack chest = player.getInventory().getChestplate();
        List<ItemStack> sources = chest == null ? List.of() : List.of(chest);
        equipmentInstances.ensure(chest);
        engine.trigger(new TriggerContext(
                TriggerType.ELYTRA_BOOST, TriggerPhase.CONFIRMED, player, Instant.now(), event,
                UUID.randomUUID(), sources, chest, event.getFirework(), null, null,
                0.0D, 0.0D));
    }
}
