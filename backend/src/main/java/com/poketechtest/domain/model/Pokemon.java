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

    public Pokemon {
        types = types == null ? List.of() : List.copyOf(types);
        stats = stats == null ? List.of() : List.copyOf(stats);
        moves = moves == null ? List.of() : List.copyOf(moves);
    }

    public BigDecimal weightKg() {
        return Measurements.fromTenths(weightHectograms);
    }

    public BigDecimal heightM() {
        return Measurements.fromTenths(heightDecimetres);
    }

    public String imageUrl() {
        return artworkUrl != null ? artworkUrl : spriteUrl;
    }
}
