package com.poketechtest.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/** Local, editable copy of a PokeAPI Pokemon: a snapshot of the catalog data plus proprietary fields. */
@Builder
public record LocalPokemon(
        Long id,
        int pokeApiId,
        String name,
        String spriteUrl,
        List<String> types,
        int heightDecimetres,
        int weightHectograms,
        String localizedName,
        String region,
        List<String> tags,
        Instant syncedAt,
        Instant updatedAt) {

    public LocalPokemon {
        types = types == null ? List.of() : List.copyOf(types);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static LocalPokemon fromCatalog(Pokemon pokemon) {
        return LocalPokemon.builder()
                .pokeApiId(pokemon.id())
                .name(pokemon.name())
                .spriteUrl(pokemon.spriteUrl())
                .types(pokemon.types())
                .heightDecimetres(pokemon.heightDecimetres())
                .weightHectograms(pokemon.weightHectograms())
                .build();
    }

    public BigDecimal heightM() {
        return Measurements.fromTenths(heightDecimetres);
    }

    public BigDecimal weightKg() {
        return Measurements.fromTenths(weightHectograms);
    }
}
