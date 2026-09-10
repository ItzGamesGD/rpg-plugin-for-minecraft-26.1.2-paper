package com.hyunseo.hyunseorpg.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopDialogPriceTest {
    @Test
    void displaysExactPerItemEquivalentWithoutRounding() {
        assertEquals("125 코인 / 개", ShopGuiService.unitPrice(true, 500, 4, ""));
        assertEquals("1/3 코인 / 개", ShopGuiService.unitPrice(true, 1, 3, ""));
        assertEquals("5/2 rpg:token / 개", ShopGuiService.unitPrice(true, 10, 4, "rpg:token"));
        assertEquals("불가능", ShopGuiService.unitPrice(false, 500, 4, ""));
    }
}
