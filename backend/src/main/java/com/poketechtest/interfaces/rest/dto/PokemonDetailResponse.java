package com.poketechtest.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record PokemonDetailResponse(
        int id,
        String name,
        String imageUrl,
        List<String> types,
        BigDecimal heightM,
        BigDecimal weightKg,
        List<PokemonStatResponse> stats,
        String description,
        EvolutionNodeResponse evolutionChain) {
}
