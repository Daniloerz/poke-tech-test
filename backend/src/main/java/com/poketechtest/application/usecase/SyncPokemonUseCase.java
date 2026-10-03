package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.PokemonAlreadySyncedException;
import com.poketechtest.application.exception.PokemonNotFoundException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.application.port.out.PokemonCatalogPort;
import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Not transactional on purpose: the PokeAPI call must not hold a database connection.
 * The insert has its own transaction and the unique constraint on poke_api_id handles concurrent requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncPokemonUseCase {

    private final PokemonCatalogPort pokemonCatalogPort;
    private final LocalPokemonRepository localPokemonRepository;

    public LocalPokemon sync(String idOrName) {
        String identifier = PokemonIdentifier.normalize(idOrName);
        Pokemon pokemon = pokemonCatalogPort.findByIdOrName(identifier)
                .orElseThrow(() -> new PokemonNotFoundException(identifier));

        localPokemonRepository.findByPokeApiId(pokemon.id()).ifPresent(existing -> {
            throw new PokemonAlreadySyncedException(existing.name(), existing.id());
        });

        LocalPokemon synced = localPokemonRepository.insert(LocalPokemon.fromCatalog(pokemon));
        log.info("Pokemon synchronized: localId={}, pokeApiId={}, name={}", synced.id(), synced.pokeApiId(), synced.name());
        return synced;
    }
}
