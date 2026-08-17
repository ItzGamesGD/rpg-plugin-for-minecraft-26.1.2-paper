package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.ui.KoreanDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Direct-submission delivery GUI. Only the player moves items into input slots. */
public final class DeliveryGuiService implements Listener {
    private static final List<Integer> INPUT_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25);
    private static final Set<Integer> INPUT_SLOT_SET = Set.copyOf(INPUT_SLOTS);
    private static final int INFO_SLOT = 4;
    private static final int SUBMIT_SLOT = 40;
    private static final int RETURN_SLOT = 45;
    private static final int CLOSE_SLOT = 49;

    private final JavaPlugin plugin;
    private final DeliveryService deliveries;
    private final DeliveryItemValidator validator;
    private final DeliveryRewardCalculator rewards;
    private final InventoryDeliveryService inventoryDelivery;
    private final RPGItemService items;
    private final FarmingProfileService profiles;
    private FarmingHoePromotionService hoePromotionService;
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> refreshTasks = new ConcurrentHashMap<>();

    public DeliveryGuiService(JavaPlugin plugin, DeliveryService deliveries,
                              DeliveryItemValidator validator, DeliveryRewardCalculator rewards,
                              InventoryDeliveryService inventoryDelivery, RPGItemService items) {
        this(plugin, deliveries, validator, rewards, inventoryDelivery, items, null);
    }

    public DeliveryGuiService(JavaPlugin plugin, DeliveryService deliveries,
                              DeliveryItemValidator validator, DeliveryRewardCalculator rewards,
                              InventoryDeliveryService inventoryDelivery, RPGItemService items,
                              FarmingProfileService profiles) {
        this.plugin = plugin;
        this.deliveries = deliveries;
        this.validator = validator;
        this.rewards = rewards;
        this.inventoryDelivery = inventoryDelivery;
        this.items = items;
        this.profiles = profiles;
    }

    public void setHoePromotionService(FarmingHoePromotionService hoePromotionService) {
        this.hoePromotionService = hoePromotionService;
    }

    public void open(Player player, DeliveryProvider provider) {
        if (player == null || provider == null) return;
        DeliverySession session = deliveries.getOrCreate(player.getUniqueId(), provider).orElse(null);
        if (session == null) {
            player.sendMessage(Component.text("\uD604\uC7AC \uC774\uC6A9 \uAC00\uB2A5\uD55C \uBC30\uB2EC\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.", NamedTextColor.YELLOW));
            return;
        }
        DeliveryMenuHolder holder = new DeliveryMenuHolder(player.getUniqueId(), provider, session.deliveryId(),
                bindHoe(player));
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("\uBC30\uB2EC: " + provider.displayName(), NamedTextColor.GOLD));
        holder.setInventory(inventory);
        if (session.status() == DeliveryStatus.ACTIVE) render(inventory, session, holder);
        else renderWaiting(inventory, session);
        player.openInventory(inventory);
        scheduleRefresh(player, holder);
    }

    public void returnOpenInputs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof DeliveryMenuHolder) {
                returnInputs(player, player.getOpenInventory().getTopInventory());
                cancelRefresh(player.getUniqueId());
                player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof DeliveryMenuHolder holder)) return;
        boolean alreadyCancelled = event.isCancelled();
        event.setCancelled(true);
        if (!holder.playerId().equals(player.getUniqueId())) return;
        if (alreadyCancelled || unsafe(event.getClick()) || event.isShiftClick()
                || !isSafeInputAction(event.getAction())) return;
        if (event.getClickedInventory() != top) {
            if (event.getClickedInventory() != null) event.setCancelled(false);
            return;
        }
        int slot = event.getRawSlot();
        if ((INPUT_SLOT_SET.contains(slot) || slot == SUBMIT_SLOT)
                && deliveries.current(holder.playerId(), holder.provider()).isEmpty()) return;
        if (INPUT_SLOT_SET.contains(slot)) {
            event.setCancelled(false);
            refreshLater(top, holder);
            return;
        }
        if (slot == SUBMIT_SLOT && event.isLeftClick()) submit(player, top, holder);
        else if (slot == RETURN_SLOT || slot == CLOSE_SLOT) player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof DeliveryMenuHolder holder)) return;
        boolean alreadyCancelled = event.isCancelled();
        event.setCancelled(true);
        if (alreadyCancelled || !allowsInputDrag(event.getRawSlots(), top.getSize())) return;
        event.setCancelled(false);
        refreshLater(top, holder);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof DeliveryMenuHolder) {
            returnInputs(player, event.getInventory());
            cancelRefresh(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof DeliveryMenuHolder) {
            returnInputs(player, player.getOpenInventory().getTopInventory());
            cancelRefresh(player.getUniqueId());
        }
    }

    private void scheduleRefresh(Player player, DeliveryMenuHolder holder) {
        cancelRefresh(player.getUniqueId());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof DeliveryMenuHolder current)
                    || !current.deliveryId().equals(holder.deliveryId())) {
                cancelRefresh(player.getUniqueId());
                return;
            }
            DeliverySession session = deliveries.getOrCreate(player.getUniqueId(), current.provider()).orElse(null);
            if (session == null) {
                returnInputs(player, player.getOpenInventory().getTopInventory());
                cancelRefresh(player.getUniqueId());
                player.sendMessage(Component.text("\uBC30\uB2EC \uC2DC\uAC04\uC774 \uB9CC\uB8CC\uB418\uC5B4 \uC785\uB825 \uC544\uC774\uD15C\uC744 \uBC18\uD658\uD588\uC2B5\uB2C8\uB2E4.", NamedTextColor.YELLOW));
                player.closeInventory();
                return;
            }
            if (!current.deliveryId().equals(session.deliveryId())) {
                returnInputs(player, player.getOpenInventory().getTopInventory());
                cancelRefresh(player.getUniqueId());
                player.closeInventory();
                open(player, current.provider());
                return;
            }
            if (session.status() == DeliveryStatus.ACTIVE) render(player.getOpenInventory().getTopInventory(), session, current);
            else {
                returnInputs(player, player.getOpenInventory().getTopInventory());
                renderWaiting(player.getOpenInventory().getTopInventory(), session);
            }
        }, 20L, 20L);
        refreshTasks.put(player.getUniqueId(), task);
    }

    private void cancelRefresh(UUID playerId) {
        BukkitTask task = refreshTasks.remove(playerId);
        if (task != null) task.cancel();
    }

    private void submit(Player player, Inventory inventory, DeliveryMenuHolder holder) {
        UUID uuid = player.getUniqueId();
        if (!processing.add(uuid)) return;
        ItemStack[] original = inputContents(inventory);
        try {
            DeliverySession session = deliveries.current(uuid, holder.provider()).orElse(null);
            if (session == null || !session.deliveryId().equals(holder.deliveryId())) {
                returnInputs(player, inventory);
                player.sendMessage(Component.text("\uBC30\uB2EC\uC774 \uB9CC\uB8CC\uB418\uC5C8\uAC70\uB098 \uBCC0\uACBD\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uC544\uC774\uD15C\uC744 \uBC18\uD658\uD569\uB2C8\uB2E4.", NamedTextColor.RED));
                return;
            }
            if (!hoeBindingMatches(player, holder.hoeBinding())) {
                player.sendMessage(Component.text("\uBC30\uB2EC\uC5D0 \uC5F0\uACB0\uB41C \uAD2D\uC774\uAC00 \uBCC0\uACBD\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uBA54\uB274\uB97C \uB2E4\uC2DC \uC5F4\uC5B4 \uC8FC\uC138\uC694.", NamedTextColor.RED));
                return;
            }
            Map<CropQuality, Integer> counts = new EnumMap<>(CropQuality.class);
            for (CropQuality quality : CropQuality.values()) counts.put(quality, 0);
            List<CropQuality> stackQualities = new ArrayList<>();
            int total = 0;
            for (ItemStack item : original) {
                if (item == null || item.getType().isAir()) {
                    stackQualities.add(null);
                    continue;
                }
                CropQuality quality = validator.qualityFor(item, session.requirement()).orElse(null);
                if (quality == null) {
                    player.sendMessage(Component.text("\uBC30\uB2EC \uC870\uAC74\uACFC \uB2E4\uB978 \uC544\uC774\uD15C\uC774 \uC788\uC2B5\uB2C8\uB2E4.", NamedTextColor.RED));
                    return;
                }
                stackQualities.add(quality);
                total += item.getAmount();
            }
            if (total < session.requirement().amount()) {
                player.sendMessage(Component.text("\uC81C\uCD9C \uC218\uB7C9\uC774 \uBD80\uC871\uD569\uB2C8\uB2E4: " + total + "/" + session.requirement().amount(), NamedTextColor.YELLOW));
                return;
            }
            int remainingForPreview = session.requirement().amount();
            for (int index = 0; index < original.length && remainingForPreview > 0; index++) {
                ItemStack item = original[index];
                if (item == null || item.getType().isAir()) continue;
                int taken = Math.min(remainingForPreview, item.getAmount());
                counts.merge(stackQualities.get(index), taken, Integer::sum);
                remainingForPreview -= taken;
            }
            long currentFavor = profiles == null ? 0L : profiles.getFavor(uuid, holder.provider());
            DeliveryPreview preview = rewards.preview(session, counts, currentFavor, holder.hoeBinding());
            ItemStack[] committed = consume(original, session.requirement().amount());
            if (!deliveries.complete(uuid, holder.provider(), holder.deliveryId(),
                    preview.finalPoints(), preview.favorIncrease())) {
                player.sendMessage(Component.text("\uBC30\uB2EC \uC800\uC7A5\uC5D0 \uC2E4\uD328\uD558\uC5EC \uC544\uC774\uD15C\uC744 \uBCF4\uC874\uD588\uC2B5\uB2C8\uB2E4.", NamedTextColor.RED));
                return;
            }
            apply(inventory, committed);
            returnRemaining(player, committed);
            player.sendMessage(Component.text("\uBC30\uB2EC \uC81C\uCD9C \uC644\uB8CC. \uD488\uC9C8 \uD3C9\uADE0: "
                    + String.format(java.util.Locale.ROOT, "%.1f", preview.quality().averageScore())
                    + " / \uBCF4\uC0C1 \uACC4\uC0B0\uC774 \uC801\uC6A9\uB429\uB2C8\uB2E4.", NamedTextColor.GREEN));
            DeliverySession completed = deliveries.getOrCreate(uuid, holder.provider()).orElse(null);
            if (completed != null && completed.status() != DeliveryStatus.ACTIVE) renderWaiting(inventory, completed);
            else renderCompleted(inventory, session, preview);
        } finally {
            processing.remove(uuid);
        }
    }

    private void render(Inventory inventory, DeliverySession session, DeliveryMenuHolder holder) {
        ItemStack[] current = inputContents(inventory);
        fill(inventory);
        apply(inventory, current);
        DeliveryPreview preview = previewForInputs(session, current, holder);
        renderStage9Info(inventory, preview);
        inventory.setItem(INFO_SLOT, icon(Material.BOOK, session.definition().displayName(), List.of(
                "\uC81C\uCD9C \uB300\uC0C1: " + KoreanDisplay.itemFamily(session.requirement().itemFamily()),
                "\uD544\uC694 \uC218\uB7C9: " + session.requirement().amount(),
                "\uCD5C\uC18C \uD488\uC9C8: " + session.requirement().minimumQuality().displayName(),
                "\uB0A8\uC740 \uC2DC\uAC04: " + session.remainingSeconds(System.currentTimeMillis()) + "\uCD08",
                "\uD604\uC7AC \uC81C\uCD9C \uC218\uB7C9: " + submittedAmount(current),
                "\uD488\uC9C8 \uBD84\uD3EC: " + qualityCounts(preview.quality().counts()),
                "\uD488\uC9C8 \uD3C9\uADE0 \uC810\uC218: " + String.format(java.util.Locale.ROOT, "%.1f", preview.quality().averageScore()),
                "\uAE30\uBCF8 \uD48D\uC694 \uD3EC\uC778\uD2B8: " + preview.basePoints(),
                "\uD604\uC7AC \uD638\uAC10\uB3C4 \uBC30\uC728: x" + preview.favorMultiplier(),
                "\uC608\uC0C1 \uCD5C\uC885 \uD3EC\uC778\uD2B8: " + preview.finalPoints(),
                "\uC77C\uBC18~\uCD5C\uACE0\uAE09 \uB4F1\uAE09\uAE4C\uC9C0 \uC81C\uCD9C \uAC00\uB2A5",
                "\uC544\uC774\uD15C ID\uC640 \uD488\uC9C8 PDC\uB97C \uAC80\uC0AC\uD569\uB2C8\uB2E4.")));
        inventory.setItem(SUBMIT_SLOT, icon(Material.LIME_DYE, "\uC81C\uCD9C", List.of("\uC88C\uD074\uB9AD\uC73C\uB85C \uCD5C\uC885 \uC81C\uCD9C")));
        inventory.setItem(RETURN_SLOT, icon(Material.ARROW, "\uC544\uC774\uD15C \uBC18\uD658", List.of("\uC785\uB825 \uC544\uC774\uD15C\uC744 \uBC18\uD658\uD558\uACE0 \uB2EB\uC2B5\uB2C8\uB2E4.")));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "\uB2EB\uAE30", List.of("\uC785\uB825 \uC544\uC774\uD15C\uC744 \uBC18\uD658\uD569\uB2C8\uB2E4.")));
    }

    private void renderCompleted(Inventory inventory, DeliverySession session, DeliveryPreview preview) {
        fill(inventory);
        inventory.setItem(INFO_SLOT, icon(Material.EMERALD, "\uC81C\uCD9C \uC644\uB8CC", List.of(
                "\uD488\uC9C8 \uD3C9\uADE0: " + String.format(java.util.Locale.ROOT, "%.1f", preview.quality().averageScore()),
                "\uC644\uB8CC \uC218\uB7C9: " + preview.quality().totalAmount(),
                "\uD604\uC7AC \uB2E8\uACC4\uC5D0\uC11C\uB294 \uCD94\uAC00 \uBCF4\uC0C1 \uC5C6\uC74C",
                "\uB2E4\uC74C \uAC31\uC2E0\uAE4C\uC9C0: " + session.remainingSeconds(System.currentTimeMillis()) + "\uCD08")));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "\uB2EB\uAE30", List.of()));
    }

    private void renderWaiting(Inventory inventory, DeliverySession session) {
        fill(inventory);
        String status = session.status() == DeliveryStatus.COMPLETED ? "\uBC30\uB2EC \uC644\uB8CC" : "\uBC30\uB2EC \uB9CC\uB8CC";
        inventory.setItem(INFO_SLOT, icon(Material.EMERALD, status, List.of(
                "\uC81C\uACF5\uC790: " + session.definition().displayName(),
                "\uB2E4\uC74C \uC758\uB8B0\uAE4C\uC9C0: " + session.remainingSeconds(System.currentTimeMillis()) + "\uCD08",
                "\uD604\uC7AC \uAC31\uC2E0 \uC124\uC815\uC740 \uB300\uAE30 \uC0C1\uD0DC\uC5D0 \uC801\uC6A9\uB429\uB2C8\uB2E4.")));
        inventory.setItem(RETURN_SLOT, icon(Material.ARROW, "\uB3CC\uC544\uAC00\uAE30", List.of("\uBA54\uB274\uB97C \uB2EB\uC2B5\uB2C8\uB2E4.")));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "\uB2EB\uAE30", List.of()));
    }

    private void renderStage9Info(Inventory inventory, DeliveryPreview preview) {
        inventory.setItem(5, icon(Material.PAPER, "\uBCF4\uC0C1 \uBBF8\uB9AC\uBCF4\uAE30", List.of(
                "\uAE30\uBCF8 \uD3EC\uC778\uD2B8: " + preview.basePoints(),
                "\uBC30\uB2EC \uBCF4\uC815: x" + formatMultiplier(preview.deliveryMultiplier()),
                "\uD488\uC9C8 \uBCF4\uC815: x" + formatMultiplier(preview.qualityMultiplier()),
                "\uAD2D\uC774 \uBCF4\uC815: x" + formatMultiplier(preview.hoeMultiplier()),
                "\uD488\uC9C8 \uAD6C\uAC04: " + qualityBand(preview.qualityBand()),
                "\uD638\uAC10\uB3C4: " + preview.currentFavor() + "/" + preview.maxFavor(),
                "\uD638\uAC10\uB3C4 \uBC30\uC728: x" + formatMultiplier(preview.favorMultiplier()),
                "\uCD5C\uC885 \uD3EC\uC778\uD2B8: " + preview.finalPoints(),
                "\uD638\uAC10\uB3C4 \uC99D\uAC00: " + preview.favorIncrease())));
    }

    private String qualityCounts(Map<CropQuality, Integer> counts) {
        List<String> parts = new ArrayList<>();
        for (CropQuality quality : CropQuality.values()) {
            int amount = counts == null ? 0 : counts.getOrDefault(quality, 0);
            if (amount > 0) parts.add(quality.displayName() + " " + amount);
        }
        return parts.isEmpty() ? "\uC5C6\uC74C" : String.join(", ", parts);
    }

    private String qualityBand(String band) {
        return switch (band == null ? "" : band.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "low" -> "\uB0AE\uC74C";
            case "medium" -> "\uBCF4\uD1B5";
            case "high" -> "\uB192\uC74C";
            case "supreme" -> "\uCD5C\uACE0\uAE09";
            default -> band == null || band.isBlank() ? "\uC5C6\uC74C" : band;
        };
    }

    private String formatMultiplier(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private void refreshLater(Inventory inventory, DeliveryMenuHolder holder) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inventory.getHolder() instanceof DeliveryMenuHolder current
                    && current.deliveryId().equals(holder.deliveryId())) {
                DeliverySession session = deliveries.getOrCreate(current.playerId(), current.provider()).orElse(null);
                if (session == null) {
                    Player player = Bukkit.getPlayer(current.playerId());
                    if (player != null) {
                        returnInputs(player, inventory);
                        player.sendMessage(Component.text("\uBC30\uB2EC \uC2DC\uAC04\uC774 \uB9CC\uB8CC\uB418\uC5B4 \uC785\uB825 \uC544\uC774\uD15C\uC744 \uBC18\uD658\uD588\uC2B5\uB2C8\uB2E4.", NamedTextColor.YELLOW));
                        player.closeInventory();
                    }
                    return;
                }
                Player player = Bukkit.getPlayer(current.playerId());
                if (player != null && !current.deliveryId().equals(session.deliveryId())) {
                    returnInputs(player, inventory);
                    cancelRefresh(current.playerId());
                    player.closeInventory();
                    open(player, current.provider());
                } else if (session.status() == DeliveryStatus.ACTIVE) {
                    render(inventory, session, holder);
                } else {
                    if (player != null) returnInputs(player, inventory);
                    renderWaiting(inventory, session);
                }
            }
        });
    }

    private void returnInputs(Player player, Inventory inventory) {
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            inventory.setItem(slot, null);
            inventoryDelivery.giveOrDiscard(player, item.clone());
        }
    }

    private void returnRemaining(Player player, ItemStack[] committed) {
        for (ItemStack remaining : committed) {
            if (remaining == null || remaining.getType().isAir() || remaining.getAmount() <= 0) continue;
            inventoryDelivery.giveOrDiscard(player, remaining.clone());
        }
    }

    private ItemStack[] inputContents(Inventory inventory) {
        ItemStack[] result = new ItemStack[INPUT_SLOTS.size()];
        for (int index = 0; index < INPUT_SLOTS.size(); index++) {
            ItemStack item = inventory.getItem(INPUT_SLOTS.get(index));
            result[index] = item == null ? null : item.clone();
        }
        return result;
    }

    private ItemStack[] consume(ItemStack[] original, int amount) {
        ItemStack[] result = cloneContents(original);
        int[] amounts = new int[result.length];
        for (int index = 0; index < result.length; index++) {
            ItemStack item = result[index];
            amounts[index] = item == null || item.getType().isAir() ? 0 : item.getAmount();
        }
        int[] remaining = remainingAmounts(amounts, amount);
        for (int index = 0; index < result.length; index++) {
            ItemStack item = result[index];
            if (item == null || item.getType().isAir() || remaining[index] <= 0) result[index] = null;
            else item.setAmount(remaining[index]);
        }
        return result;
    }

    static int[] remainingAmounts(int[] original, int required) {
        int[] result = original == null ? new int[0] : original.clone();
        int remaining = Math.max(0, required);
        for (int index = 0; index < result.length && remaining > 0; index++) {
            int available = Math.max(0, result[index]);
            int taken = Math.min(remaining, available);
            result[index] = available - taken;
            remaining -= taken;
        }
        for (int index = 0; index < result.length; index++) result[index] = Math.max(0, result[index]);
        return result;
    }

    private void apply(Inventory inventory, ItemStack[] contents) {
        for (int index = 0; index < INPUT_SLOTS.size(); index++) inventory.setItem(INPUT_SLOTS.get(index), contents[index]);
    }

    private ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] result = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++) result[index] = source[index] == null ? null : source[index].clone();
        return result;
    }

    private int submittedAmount(ItemStack[] contents) {
        int total = 0;
        for (ItemStack item : contents) if (item != null && !item.getType().isAir()) total += item.getAmount();
        return total;
    }

    private DeliveryPreview previewForInputs(DeliverySession session, ItemStack[] contents,
                                             DeliveryMenuHolder holder) {
        Map<CropQuality, Integer> counts = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) counts.put(quality, 0);
        for (ItemStack item : contents) {
            CropQuality quality = validator.qualityFor(item, session.requirement()).orElse(null);
            if (quality != null) counts.merge(quality, item.getAmount(), Integer::sum);
        }
        long currentFavor = profiles == null ? 0L : profiles.getFavor(holder.playerId(), holder.provider());
        return rewards.preview(session, counts, currentFavor, holder.hoeBinding());
    }

    private DeliveryHoeBinding bindHoe(Player player) {
        if (hoePromotionService == null) return DeliveryHoeBinding.neutral();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!hoePromotionService.isHoe(mainHand)) return DeliveryHoeBinding.neutral();
        FarmingHoePromotionService.HoeTierStar tierStar = hoePromotionService.read(mainHand);
        FarmingHoePromotionService.HoePassives passives = hoePromotionService.passives(tierStar.tier(), tierStar.star());
        return new DeliveryHoeBinding(tierStar.instanceId(), passives.abundancePointMultiplier());
    }

    private boolean hoeBindingMatches(Player player, DeliveryHoeBinding binding) {
        if (binding == null || !binding.isBound()) return true;
        return hoePromotionService != null
                && hoePromotionService.isSameInstance(player.getInventory().getItemInMainHand(), binding.instanceId());
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        for (int slot : INPUT_SLOTS) inventory.setItem(slot, null);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private boolean unsafe(ClickType click) {
        return click == ClickType.NUMBER_KEY || click == ClickType.DOUBLE_CLICK
                || click == ClickType.CREATIVE || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP || click == ClickType.SWAP_OFFHAND;
    }

    static boolean isSafeInputAction(InventoryAction action) {
        return action == InventoryAction.NOTHING
                || action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.PICKUP_HALF
                || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PICKUP_SOME
                || action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR;
    }

    static boolean allowsInputDrag(Set<Integer> rawSlots, int topSize) {
        return rawSlots != null && !rawSlots.isEmpty()
                && rawSlots.stream().allMatch(slot -> slot >= 0
                && slot < topSize && INPUT_SLOT_SET.contains(slot));
    }

    static boolean isInputSlot(int slot) {
        return INPUT_SLOT_SET.contains(slot);
    }
}
