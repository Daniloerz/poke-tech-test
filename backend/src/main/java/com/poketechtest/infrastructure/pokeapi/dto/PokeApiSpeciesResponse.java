package com.poketechtest.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiSpeciesResponse(
        String name,
        @JsonProperty("flavor_text_entries") List<PokeApiFlavorText> flavorTextEntries,
        @JsonProperty("evolution_chain") PokeApiNamedResource evolutionChain) {
}
