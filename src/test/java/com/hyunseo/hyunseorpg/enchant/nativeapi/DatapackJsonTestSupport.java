package com.hyunseo.hyunseorpg.enchant.nativeapi;

import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class DatapackJsonTestSupport {
    private DatapackJsonTestSupport() { }

    static Set<String> tagValues(String namespace, String name) throws IOException {
        String path = "datapack/data/" + namespace + "/tags/enchantment/" + name + ".json";
        try (var input = DatapackJsonTestSupport.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> values = new LinkedHashSet<>();
            for (var value : root.getAsJsonArray("values")) values.add(value.getAsString());
            return Set.copyOf(values);
        }
    }
}
