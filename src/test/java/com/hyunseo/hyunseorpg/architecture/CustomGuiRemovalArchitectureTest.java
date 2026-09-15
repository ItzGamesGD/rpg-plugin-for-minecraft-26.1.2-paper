package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomGuiRemovalArchitectureTest {
    private static final Path JAVA = Path.of("src/main/java");

    @Test
    void knownGameplayGuiTypesAreAbsent() {
        for (String type : List.of(
                "crafting/CraftingGuiService.java", "farming/DeliveryGuiService.java",
                "farming/FarmingHubGuiService.java", "farming/CookingGuiService.java",
                "alchemy/EffectListGuiService.java", "alchemy/gui/AlchemyCatalystGuiService.java",
                "alchemy/gui/AlchemyGuiControllerService.java", "ui/StatGuiService.java",
                "ui/StatGuiListener.java", "special/SpecialEquipmentMenuService.java")) {
            assertFalse(Files.exists(JAVA.resolve("com/hyunseo/hyunseorpg").resolve(type)), type);
        }
    }

    @Test
    void productionCreatesNoPluginOwnedInventoriesOrHolders() throws IOException {
        String source = productionSource();
        assertFalse(source.contains("Bukkit.createInventory"));
        assertFalse(source.contains("createInventory("));
        assertFalse(source.contains("openInventory("));
        assertFalse(source.contains("implements InventoryHolder"));
    }

    @Test
    void remainingInventoryEventsOnlyObserveVanillaInventoryState() throws IOException {
        Set<String> actual;
        try (var files = Files.walk(JAVA)) {
            actual = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).matches("(?s).*(InventoryClickEvent|InventoryDragEvent|InventoryCloseEvent).*"))
                    .map(path -> JAVA.relativize(path).toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
        assertEquals(Set.of(
                "com/hyunseo/hyunseorpg/alchemy/AlchemyVanillaBypassListener.java",
                "com/hyunseo/hyunseorpg/crafting/SoulboundItemService.java",
                "com/hyunseo/hyunseorpg/enhancement/VanillaEnchantBlockListener.java",
                "com/hyunseo/hyunseorpg/equipment/EquipmentActualEffectListener.java",
                "com/hyunseo/hyunseorpg/exploration/listener/ExplorationChestLootListener.java",
                "com/hyunseo/hyunseorpg/item/VanillaStackingService.java",
                "com/hyunseo/hyunseorpg/special/flame/FlameAxeListener.java",
                "com/hyunseo/hyunseorpg/special/moonlit/MoonlitAfterglowListener.java",
                "com/hyunseo/hyunseorpg/special/solaris/SolarisListener.java",
                "com/hyunseo/hyunseorpg/special/thanatos/ThanatosMaceListener.java",
                "com/hyunseo/hyunseorpg/special/thunder/ThunderAxeListener.java",
                "com/hyunseo/hyunseorpg/special/water/WaterTridentListener.java"), actual);
    }

    private String productionSource() throws IOException {
        StringBuilder source = new StringBuilder();
        try (var files = Files.walk(JAVA)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".java")).toList()) {
                source.append(read(path));
            }
        }
        return source.toString();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
