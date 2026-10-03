package com.poketechtest.application.port.out;

import com.poketechtest.domain.model.Pokemon;
import java.util.Optional;

public interface PokemonCatalogPort {

    PokemonCatalogPage listPage(long offset, int limit);

    Optional<Pokemon> findByName(String name);
}
