package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VanillaLikeEnchantFlowArchitectureTest {
    @Test
    void productionHasNoCustomEquipTransactionOrGeneratedNativeLore() throws Exception {
        String menu = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/ui/RPGMenuService.java"));
        String growth = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enhancement/EquipmentGrowthGuiService.java"));
        String listener = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enhancement/AnvilGrowthListener.java"));
        String service = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enchant/EnchantService.java"));
        assertFalse(menu.contains("growth.openEnchant"));
        assertFalse(growth.contains("public void openEnchant"));
        assertFalse(growth.contains("enchantService.equip"));
        assertFalse(listener.contains("guiService.openEnchant"));
        assertFalse(service.contains("public boolean equip("));
        assertTrue(service.contains("new ItemStack(Material.ENCHANTED_BOOK)"));
        assertTrue(service.contains("meta.displayName(null)"));
        assertTrue(service.contains("removeGeneratedLore(meta)"));
        assertFalse(service.contains(" 인챈트 북\""));
    }

    @Test
    void retiredRuntimeEffectsAndSwiftSneakBridgeAreAbsent() throws Exception {
        String content = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enchant/EquipmentEnchantContentService.java"));
        assertFalse(content.contains("swift_sneak_vanilla_sync"));
        assertFalse(content.contains("swift_sneak_original_level"));
        assertFalse(content.contains("hasActiveEquipped(boots, \"depth_strider\")"));
        assertFalse(content.contains("hasActiveEquipped(boots, \"frost_walker\")"));
        assertFalse(content.contains("hasActiveEquipped(item, \"unbreaking\")"));
        assertFalse(content.contains("case \"thorns\""));
    }
}
