package com.poketechtest.application.port.out;

import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.PageResult;
import java.util.Optional;

public interface LocalPokemonRepository {

    Optional<LocalPokemon> findById(long id);

    Optional<LocalPokemon> findByPokeApiId(int pokeApiId);

    /** Ordered by local id. */
    PageResult<LocalPokemon> findPage(int page, int size);

    /** Throws PokemonAlreadySyncedException if the PokeAPI id is already stored (unique constraint). */
    LocalPokemon insert(LocalPokemon localPokemon);

    LocalPokemon update(LocalPokemon localPokemon);

    void deleteById(long id);
}
