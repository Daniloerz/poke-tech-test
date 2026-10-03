package com.poketechtest.application.port.out;

import java.util.List;

public record PokemonCatalogPage(List<String> names, long totalCount) {

    public PokemonCatalogPage {
        // Guarantees immutability and thread safety for cache sharing by creating a unmodifiable copy of the names list.
        names = names == null ? List.of() : List.copyOf(names);
    }
}
