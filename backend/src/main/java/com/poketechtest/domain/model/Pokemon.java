package com.poketechtest.domain.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record Pokemon(
        int id,
        String name,
        String speciesName,
        String spriteUrl,
        String artworkUrl,
        List<String> types,
        int heightDecimetres,
        int weightHectograms,
        List<PokemonStat> stats,
        List<String> moves) {

    // PokeAPI gives height in decimetres and weight in hectograms: one decimal place in metres and kilograms.
    private static final int ONE_DECIMAL_SCALE = 1;

    public Pokemon {
        types = types == null ? List.of() : List.copyOf(types);
        stats = stats == null ? List.of() : List.copyOf(stats);
        moves = moves == null ? List.of() : List.copyOf(moves);
    }

    public BigDecimal weightKg() {
        return BigDecimal.valueOf(weightHectograms, ONE_DECIMAL_SCALE);
    }

    public BigDecimal heightM() {
        return BigDecimal.valueOf(heightDecimetres, ONE_DECIMAL_SCALE);
    }

    public String imageUrl() {
        return artworkUrl != null ? artworkUrl : spriteUrl;
    }
}
