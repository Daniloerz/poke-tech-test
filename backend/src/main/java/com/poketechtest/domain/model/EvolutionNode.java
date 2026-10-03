package com.poketechtest.domain.model;

import java.util.List;

public record EvolutionNode(int speciesId, String name, String imageUrl, List<EvolutionNode> evolvesTo) {

    public EvolutionNode {
        evolvesTo = evolvesTo == null ? List.of() : List.copyOf(evolvesTo);
    }
}
