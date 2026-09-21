package com.hyunseo.hyunseorpg.special;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IntegratedWeaponConfigurationTest {
    @Test void allDedicatedWeaponsHaveResolvableItemsAndAbilities() {
        Map<String, Object> itemRoot = new Yaml().load(getClass().getResourceAsStream("/items.yml"));
        Map<String, Object> specialRoot = new Yaml().load(getClass().getResourceAsStream("/special-equipment.yml"));
        Map<?, ?> items = (Map<?, ?>) itemRoot.get("items");
        Map<?, ?> definitions = (Map<?, ?>) ((Map<?, ?>) specialRoot.get("special-equipment")).get("items");
        for (String id : DedicatedWeaponIds.all()) {
            String registryId = id.equals("poseidon_spear") ? "poseidons_spear" : id;
            Map<?, ?> definition = (Map<?, ?>) definitions.get(registryId);
            assertNotNull(definition, id);
            assertTrue(items.containsKey(definition.get("item-id")), id + " missing item");
            assertFalse(((Map<?, ?>) definition.get("abilities")).isEmpty(), id);
        }
    }
}
