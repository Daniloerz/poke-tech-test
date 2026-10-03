package com.poketechtest.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record PokemonSummaryResponse(
        int id,
        String name,
        String spriteUrl,
        List<String> types,
        BigDecimal weightKg,
        List<String> moves) {
}
