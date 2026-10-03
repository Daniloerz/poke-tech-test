package com.poketechtest.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokeApiChainLink(PokeApiNamedResource species, @JsonProperty("evolves_to") List<PokeApiChainLink> evolvesTo) {
}
