package com.poketechtest.application.port.out;

import com.poketechtest.domain.model.LocalPokemon;
import java.util.Optional;

public interface LocalPokemonRepository {

    Optional<LocalPokemon> findById(long id);

    Optional<LocalPokemon> findByPokeApiId(int pokeApiId);

    /** Throws PokemonAlreadySyncedException if the PokeAPI id is already stored (unique constraint). */
    LocalPokemon insert(LocalPokemon localPokemon);
}
