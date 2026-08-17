package com.hyunseo.hyunseorpg.farming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropQualityCanonicalIdentityTest {
    @Test
    void canonicalQualityCropIdsResolveWithoutQualityYamlMapping() {
        CropQualityService service = new CropQualityService(null, null);

        assertEquals("corn", service.cropOfItem("crop_corn").orElseThrow());
        assertEquals("corn", service.cropOfItem("crop_corn_quality_supreme").orElseThrow());
        assertEquals(CropQuality.SUPREME,
                service.qualityOfItem("crop_corn_quality_supreme").orElseThrow());
        assertEquals("corn", service.cropOfItem("processed_corn_starch_supreme").orElseThrow());
        assertEquals(CropQuality.SUPREME,
                service.qualityOfItem("processed_corn_starch_supreme").orElseThrow());
    }
}
