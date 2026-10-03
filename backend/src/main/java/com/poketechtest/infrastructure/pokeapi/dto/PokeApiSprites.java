package com.poketechtest.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiSprites(@JsonProperty("front_default") String frontDefault, PokeApiOtherSprites other) {
}
