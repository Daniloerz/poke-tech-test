package com.poketechtest.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiPokemonResponse(
        int id,
        String name,
        int weight,
        PokeApiSprites sprites,
        List<PokeApiTypeSlot> types,
        List<PokeApiMoveSlot> moves) {
}
