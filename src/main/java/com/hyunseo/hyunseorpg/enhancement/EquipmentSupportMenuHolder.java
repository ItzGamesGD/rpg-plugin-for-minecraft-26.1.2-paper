package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Session-owned holder for equipment support screens. Display items are never source items. */
public final class EquipmentSupportMenuHolder implements InventoryHolder {
    public enum View { EXTRACTION_LIST, EXTRACTION_ENCHANTS, EXTRACTION_CONFIRM, FUTURE, FUTURE_DETAIL }

    private final View view;
    private final UUID sessionId = UUID.randomUUID();
    private final Map<Integer, UUID> sources = new LinkedHashMap<>();
    private final Map<Integer, String> choices = new LinkedHashMap<>();
    private final Map<Integer, String> featureIds = new LinkedHashMap<>();
    private UUID sourceId;
    private String selectedId;
    private String selectedFeatureId;
    private UUID playerId;
    private int sourceSlot = -1;
    private String sourceItemId = "";
    private EquipmentTierService.Category sourceCategory = EquipmentTierService.Category.UNSUPPORTED;
    private int sourceEnchantLevel;
    private Inventory inventory;
    private int page;

    public EquipmentSupportMenuHolder(View view, int page) {
        this.view = view;
        this.page = Math.max(0, page);
    }

    public View view() { return view; }
    public UUID sessionId() { return sessionId; }
    public Map<Integer, UUID> sources() { return sources; }
    public Map<Integer, String> choices() { return choices; }
    public Map<Integer, String> featureIds() { return featureIds; }
    public UUID sourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public String selectedId() { return selectedId; }
    public void setSelectedId(String selectedId) { this.selectedId = selectedId; }
    public String selectedFeatureId() { return selectedFeatureId; }
    public void setSelectedFeatureId(String selectedFeatureId) { this.selectedFeatureId = selectedFeatureId; }
    public UUID playerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    public int sourceSlot() { return sourceSlot; }
    public void setSourceSlot(int sourceSlot) { this.sourceSlot = sourceSlot; }
    public String sourceItemId() { return sourceItemId; }
    public void setSourceItemId(String sourceItemId) { this.sourceItemId = sourceItemId == null ? "" : sourceItemId; }
    public EquipmentTierService.Category sourceCategory() { return sourceCategory; }
    public void setSourceCategory(EquipmentTierService.Category sourceCategory) {
        this.sourceCategory = sourceCategory == null ? EquipmentTierService.Category.UNSUPPORTED : sourceCategory;
    }
    public int sourceEnchantLevel() { return sourceEnchantLevel; }
    public void setSourceEnchantLevel(int sourceEnchantLevel) { this.sourceEnchantLevel = Math.max(0, sourceEnchantLevel); }
    public int page() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
