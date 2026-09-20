package com.hyunseo.hyunseorpg.player;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyQuestCompatibilityDataTest {
    @Test
    void legacyQuestYamlRoundTripsWithoutDomainInterpretation() throws Exception {
        YamlConfiguration source = new YamlConfiguration();
        source.loadFromString("""
                questStates:
                  first_trial: COMPLETED
                questProgress:
                  first_trial.kill: 7
                quests:
                  cooldown-until: 123456789
                  history:
                    completed: 4
                    failed: 2
                  active:
                    slot-1:
                      type: RETIRED_UNKNOWN_TYPE
                      status: FUTURE_LEGACY_STATUS
                      objectives:
                        objective-1:
                          target-source: REMOVED_SOURCE
                          progress: 3
                      unknown-list: [one, two]
                """);

        LegacyQuestCompatibilityData payload = LegacyQuestCompatibilityData.capture(source);
        YamlConfiguration saved = new YamlConfiguration();
        payload.writeTo(saved);
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(saved.saveToString());

        assertEquals("COMPLETED", reloaded.getString("questStates.first_trial"));
        assertEquals(7, reloaded.getInt("questProgress.first_trial.kill"));
        assertEquals(123456789L, reloaded.getLong("quests.cooldown-until"));
        assertEquals(4, reloaded.getInt("quests.history.completed"));
        assertEquals(2, reloaded.getInt("quests.history.failed"));
        assertEquals("RETIRED_UNKNOWN_TYPE", reloaded.getString("quests.active.slot-1.type"));
        assertEquals("FUTURE_LEGACY_STATUS", reloaded.getString("quests.active.slot-1.status"));
        assertEquals("REMOVED_SOURCE", reloaded.getString(
                "quests.active.slot-1.objectives.objective-1.target-source"));
        assertEquals(3, reloaded.getInt("quests.active.slot-1.objectives.objective-1.progress"));
        assertEquals(java.util.List.of("one", "two"),
                reloaded.getStringList("quests.active.slot-1.unknown-list"));
        assertFalse(saved.contains("baseLevel"), "compatibility payload must only write retired quest roots");
    }
}
