package com.poketechtest.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LocalPokemonResponse(
        Long id,
        int pokeApiId,
        String name,
        String spriteUrl,
        List<String> types,
        BigDecimal heightM,
        BigDecimal weightKg,
        String localizedName,
        String region,
        List<String> tags,
        Instant syncedAt,
        Instant updatedAt) {
}
