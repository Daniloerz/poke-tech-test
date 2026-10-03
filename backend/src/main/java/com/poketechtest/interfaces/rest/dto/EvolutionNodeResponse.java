package com.poketechtest.interfaces.rest.dto;

import java.util.List;

public record EvolutionNodeResponse(int id, String name, String imageUrl, List<EvolutionNodeResponse> evolvesTo) {
}
