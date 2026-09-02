package com.mira.kits.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitTextTest {
    @Test
    void normalizesNamesLikeTagCreation() {
        assertEquals("starter_kit", KitText.normalizeId("Starter Kit"));
        assertEquals("pvp_plus", KitText.normalizeId("  PvP Plus  "));
    }

    @Test
    void rendersRomanEnchantLevels() {
        assertEquals("V", KitText.roman(5));
        assertEquals("X", KitText.roman(10));
        assertEquals("XXV", KitText.roman(25));
    }
}
