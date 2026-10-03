package com.poketechtest.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiEvolutionChainResponse(int id, PokeApiChainLink chain) {
}
