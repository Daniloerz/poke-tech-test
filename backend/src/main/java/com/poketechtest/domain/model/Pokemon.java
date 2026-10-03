package com.poketechtest.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record Pokemon(
        int id,
        String name,
        String spriteUrl,
        List<String> types,
        int weightHectograms,
        List<String> moves) {

    private static final int HECTOGRAMS_SCALE = 1;

    public Pokemon {
        types = types == null ? List.of() : List.copyOf(types);
        moves = moves == null ? List.of() : List.copyOf(moves);
    }

    public BigDecimal weightKg() {
        return BigDecimal.valueOf(weightHectograms, HECTOGRAMS_SCALE);
    }
}
