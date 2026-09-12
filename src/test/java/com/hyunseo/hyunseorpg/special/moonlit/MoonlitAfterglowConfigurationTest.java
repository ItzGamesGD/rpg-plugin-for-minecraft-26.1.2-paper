package com.hyunseo.hyunseorpg.special.moonlit;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MoonlitAfterglowConfigurationTest {
    @Test void declaresStableIdentityAndAllTuningValues() {
        try (InputStream stream = getClass().getResourceAsStream("/special-equipment.yml")) {
            assertNotNull(stream);
            String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("moonlit_afterglow:"));
            assertTrue(yaml.contains("item-id: moonlit_afterglow"));
            for (String key : java.util.List.of("base-attack-speed:", "passive:", "yugwang:",
                    "moon-flash:", "moon-shadow:")) assertTrue(yaml.contains(key), key);
            String moonlit = yaml.substring(yaml.indexOf("    moonlit_afterglow:"));
            assertFalse(moonlit.contains("velocity:"), "teleport-based Yugwang must not advertise dead velocity config");
            assertFalse(moonlit.contains("sequence-duration-ticks:"), "count and cadence are authoritative");
        } catch (Exception exception) { fail(exception); }
    }
}
