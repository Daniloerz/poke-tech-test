package com.poketechtest.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    /**
     * Returns a copy with new proprietary fields; the PokeAPI snapshot is never changed.
     * Texts are trimmed (blank becomes null). Tags are case-insensitive: trimmed, lower-cased, without duplicates.
     */
    public LocalPokemon withProprietaryFields(String newLocalizedName, String newRegion, List<String> newTags) {
        return new LocalPokemon(id, pokeApiId, name, spriteUrl, types, heightDecimetres, weightHectograms,
                trimToNull(newLocalizedName), trimToNull(newRegion), normalizeTags(newTags), syncedAt, updatedAt);
    }

    public BigDecimal heightM() {
        return Measurements.fromTenths(heightDecimetres);
    }

    public BigDecimal weightKg() {
        return Measurements.fromTenths(weightHectograms);
    }

    private static String trimToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
    }
}
