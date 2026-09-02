package com.mira.kits.model;

import java.math.BigDecimal;

public record KitMeta(
        String id,
        String displayName,
        BigDecimal price,
        boolean visible,
        boolean enabled
) {
}
