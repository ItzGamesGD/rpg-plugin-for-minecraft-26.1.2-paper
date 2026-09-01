package com.hyunseo.hyunseorpg.skill;

import com.hyunseo.hyunseorpg.alchemy.PaperAlchemyCombatAdapter;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class SkillInputListener implements Listener {
    private static final long SUPPRESS_LEFT_AFTER_DROP_MILLIS = 180L;

    private final JavaPlugin plugin;
    private final SkillService skillService;
    private final EquipmentInstanceService equipmentInstances;
    private final PaperAlchemyCombatAdapter alchemyCombat;
    private final Predicate<ItemStack> dedicatedInputOwner;
    private final Map<InputKey, Long> lastInputs = new HashMap<>();
    private final Map<UUID, Long> suppressLeftInputUntilByPlayer = new HashMap<>();
    private final Map<UUID, PendingDrop> pendingDrops = new HashMap<>();
    private final java.util.Set<UUID> sneakingPlayers = new java.util.HashSet<>();
    private final Map<UUID, Boolean> groundedByPlayer = new HashMap<>();

    public SkillInputListener(JavaPlugin plugin, SkillService skillService, EquipmentInstanceService equipmentInstances,
                              PaperAlchemyCombatAdapter alchemyCombat, Predicate<ItemStack> dedicatedInputOwner) {
        this.plugin = plugin;
        this.skillService = skillService;
        this.equipmentInstances = equipmentInstances;
        this.alchemyCombat = alchemyCombat;
        this.dedicatedInputOwner = dedicatedInputOwner == null ? item -> false : dedicatedInputOwner;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Shift+F belongs to the integrated menu and must never be consumed by combat inputs.
        if (player.isSneaking()) return;
        if (dedicatedInputOwner.test(player.getInventory().getItemInMainHand())) return;

        SkillInputResult result = processInputResult(player, SkillInputType.OFFHAND_QUICK, null);
        if (!result.accepted() && !skillService.shouldCancelVanillaActionForInput(player, SkillInputType.OFFHAND_QUICK)) {
            return;
        }
        if (result.cancelVanillaAction()
                || skillService.shouldCancelVanillaActionForInput(player, SkillInputType.OFFHAND_QUICK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (dedicatedInputOwner.test(event.getPlayer().getInventory().getItemInMainHand())) return;

        SkillInputType inputType = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> event.getPlayer().isSneaking()
                    ? SkillInputType.SHIFT_RIGHT_CLICK : SkillInputType.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> leftClickInput(event.getPlayer());
            default -> null;
        };
        if (inputType == null) {
            return;
        }

        boolean cancelVanilla = processInput(event.getPlayer(), inputType);
        if (cancelVanilla) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        processInput(event.getPlayer(), leftClickInput(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (alchemyCombat != null && alchemyCombat.blocksAnyAttack(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        SkillInputType inputType = leftClickInput(player);
        if (skillService.shouldCancelVanillaActionForInput(player, inputType)) {
            processInput(player, inputType);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedStack = event.getItemDrop().getItemStack();
        SkillInputResult result = processInput(event.getPlayer(), SkillInputType.DROP_KEY, droppedStack);
        if (result.accepted() && result.cancelVanillaAction()) {
            event.setCancelled(true);
            return;
        }
        if (!isProtectedWeapon(droppedStack)) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        boolean shiftDrop = event.getPlayer().isSneaking() || sneakingPlayers.contains(playerId);
        if (!shiftDrop) {
            pendingDrops.remove(playerId);
        }
        PendingDrop pending = pendingDrops.get(playerId);
        if (pending != null && pending.expiresAt() > System.currentTimeMillis()
                && pending.item().isSimilar(droppedStack)) {
            pendingDrops.remove(playerId);
            // This is the explicit second confirmation. Leave the event uncancelled.
            return;
        }
        if (shiftDrop && plugin.getConfig().getBoolean("item-drop-protection.enabled", true)
                && plugin.getConfig().getBoolean("item-drop-protection.require-shift", true)) {
            pendingDrops.put(playerId, new PendingDrop(droppedStack.clone(), System.currentTimeMillis() + 10_000L));
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                    "버리려면 쉬프트+버리기 키를 10초 안에 한 번 더 누르세요.", NamedTextColor.YELLOW));
            return;
        }

        suppressLeftInputUntilByPlayer.put(
                event.getPlayer().getUniqueId(),
                System.currentTimeMillis() + SUPPRESS_LEFT_AFTER_DROP_MILLIS
        );
        // A weapon without a DROP_KEY skill must still require confirmation before it is dropped.
        if (!event.isCancelled() && plugin.getConfig().getBoolean("item-drop-protection.enabled", true)) {
            event.setCancelled(true);
            if (event.isCancelled()) return;
            event.getPlayer().sendMessage(Component.text(
                    "버리려면 버리기 키를 10초 안에 한 번 더 누르세요.", NamedTextColor.YELLOW));
        }
    }

    private boolean isProtectedWeapon(ItemStack item) {
        if (plugin.getConfig().getBoolean("item-drop-protection.enabled", true)
                && skillService.isInputEquipment(item)) return true;
        if (!plugin.getConfig().getBoolean("item-drop-protection.protect-tools", false)) return false;
        if (item == null || item.getType().isAir()) return false;
        return switch (item.getType()) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD,
                    BOW, CROSSBOW, TRIDENT, WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE,
                    NETHERITE_AXE, BLAZE_ROD, STICK -> true;
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE,
                    NETHERITE_PICKAXE, WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL,
                    DIAMOND_SHOVEL, NETHERITE_SHOVEL, WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE,
                    DIAMOND_HOE, NETHERITE_HOE -> true;
            default -> false;
        };
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            sneakingPlayers.add(event.getPlayer().getUniqueId());
        } else {
            sneakingPlayers.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSneakingJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null) return;
        if (alchemyCombat != null && alchemyCombat.blocksAnyMovement(player.getUniqueId())) {
            event.setTo(event.getFrom());
            return;
        }
        UUID playerId = player.getUniqueId();
        boolean wasGrounded = groundedByPlayer.getOrDefault(playerId, player.isOnGround()
                || event.getFrom().clone().subtract(0.0D, 0.05D, 0.0D).getBlock().getType().isSolid());
        boolean rising = event.getTo().getY() > event.getFrom().getY() + 0.05D;
        boolean nowGrounded = player.isOnGround()
                || event.getTo().clone().subtract(0.0D, 0.05D, 0.0D).getBlock().getType().isSolid();
        groundedByPlayer.put(playerId, nowGrounded);
        if (!player.isSneaking() || !wasGrounded || !rising) {
            return;
        }
        if (!skillService.shouldCancelVanillaActionForInput(player, SkillInputType.SHIFT_JUMP)) return;
        processInput(player, SkillInputType.SHIFT_JUMP);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastInputs.entrySet().removeIf(entry -> entry.getKey().playerId().equals(event.getPlayer().getUniqueId()));
        suppressLeftInputUntilByPlayer.remove(event.getPlayer().getUniqueId());
        pendingDrops.remove(event.getPlayer().getUniqueId());
        sneakingPlayers.remove(event.getPlayer().getUniqueId());
        groundedByPlayer.remove(event.getPlayer().getUniqueId());
    }

    private boolean processInput(Player player, SkillInputType inputType) {
        return processInputResult(player, inputType, null).cancelVanillaAction();
    }

    private SkillInputResult processInput(Player player, SkillInputType inputType, ItemStack triggeringItem) {
        return processInputResult(player, inputType, triggeringItem);
    }

    private SkillInputResult processInputResult(Player player, SkillInputType inputType, ItemStack triggeringItem) {
        if (alchemyCombat != null && inputType != SkillInputType.DROP_KEY
                && alchemyCombat.blocksAnyActiveSkill(player.getUniqueId())) {
            return SkillInputResult.accepted(true);
        }
        if (isLeftClickInput(inputType) && isLeftInputSuppressed(player)) {
            return SkillInputResult.ignored();
        }

        if (isDuplicateInput(player, inputType, triggeringItem)) {
            return SkillInputResult.accepted(skillService.shouldCancelVanillaActionForInput(player, inputType));
        }

        SkillInputResult result = triggeringItem == null
                ? skillService.handleInput(player, inputType)
                : skillService.handleInput(player, inputType, triggeringItem);
        long suppressLeftMillis = skillService.getPostInputLeftSuppressMillis(player, inputType);
        if (suppressLeftMillis > 0L) {
            suppressLeftInputUntilByPlayer.put(player.getUniqueId(), System.currentTimeMillis() + suppressLeftMillis);
        }
        return result;
    }

    private SkillInputType leftClickInput(Player player) {
        return player.isSneaking() ? SkillInputType.SHIFT_LEFT_CLICK : SkillInputType.LEFT_CLICK;
    }

    private boolean isLeftClickInput(SkillInputType inputType) {
        return inputType == SkillInputType.LEFT_CLICK || inputType == SkillInputType.SHIFT_LEFT_CLICK;
    }

    private boolean isLeftInputSuppressed(Player player) {
        Long suppressUntil = suppressLeftInputUntilByPlayer.get(player.getUniqueId());
        if (suppressUntil == null) {
            return false;
        }
        if (System.currentTimeMillis() <= suppressUntil) {
            return true;
        }
        suppressLeftInputUntilByPlayer.remove(player.getUniqueId());
        return false;
    }

    private boolean isDuplicateInput(Player player, SkillInputType inputType, ItemStack triggeringItem) {
        long tick = plugin.getServer().getCurrentTick();
        UUID equipmentId = equipmentInstances.get(triggeringItem == null
                ? player.getInventory().getItemInMainHand() : triggeringItem).orElse(new UUID(0L, 0L));
        InputKey key = new InputKey(player.getUniqueId(), equipmentId, inputType, tick);
        lastInputs.entrySet().removeIf(entry -> entry.getKey().tick() < tick - 2L);
        return lastInputs.putIfAbsent(key, tick) != null;
    }

    private ItemStack cloneOrAir(ItemStack itemStack) {
        return itemStack == null ? new ItemStack(Material.AIR) : itemStack.clone();
    }

    private record PendingDrop(ItemStack item, long expiresAt) { }
    private record InputKey(UUID playerId, UUID equipmentId, SkillInputType inputType, long tick) { }
}
