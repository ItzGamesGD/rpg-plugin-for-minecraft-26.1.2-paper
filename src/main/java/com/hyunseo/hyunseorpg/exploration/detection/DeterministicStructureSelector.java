package com.hyunseo.hyunseorpg.exploration.detection;

import com.hyunseo.hyunseorpg.exploration.model.StructureCandidate;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.StructureVariantDefinition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/** Deterministic selection prevents a crash-before-save from rerolling RPG status or variant. */
public final class DeterministicStructureSelector {
    public UUID stableId(StructureCandidate candidate) {
        String source = candidate.worldId() + "|" + candidate.minecraftKey() + "|"
                + candidate.bounds().minX() + "," + candidate.bounds().minY() + "," + candidate.bounds().minZ() + "|"
                + candidate.bounds().maxX() + "," + candidate.bounds().maxY() + "," + candidate.bounds().maxZ();
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    public boolean selected(UUID structureId, double probability) {
        if (probability <= 0.0D) return false;
        if (probability >= 1.0D) return true;
        return unit(structureId.toString() + "|selected") < probability;
    }

    public StructureVariantDefinition chooseVariant(UUID structureId, ExplorationStructureDefinition definition) {
        List<StructureVariantDefinition> enabled = definition.variants().stream()
                .filter(StructureVariantDefinition::enabled)
                .filter(variant -> variant.weight() > 0.0D)
                .toList();
        double total = enabled.stream().mapToDouble(StructureVariantDefinition::weight).sum();
        if (enabled.isEmpty() || total <= 0.0D) throw new IllegalStateException("No enabled variant for " + definition.id());
        double roll = unit(structureId + "|variant") * total;
        for (StructureVariantDefinition variant : enabled) {
            roll -= variant.weight();
            if (roll <= 0.0D) return variant;
        }
        return enabled.get(enabled.size() - 1);
    }

    private double unit(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            long raw = 0L;
            for (int i = 0; i < 8; i++) raw = (raw << 8) | (hash[i] & 0xffL);
            return (raw >>> 1) / (double) Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
