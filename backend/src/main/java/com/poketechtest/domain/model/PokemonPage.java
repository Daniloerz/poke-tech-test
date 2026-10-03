package com.poketechtest.domain.model;

import java.util.List;

public record PokemonPage(List<Pokemon> items, int page, int size, long totalElements) {

    public PokemonPage {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or positive");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
