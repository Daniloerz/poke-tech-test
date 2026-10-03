package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.exception.PokemonNotFoundException;
import com.poketechtest.application.port.out.PokemonCatalogPort;
import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonDetail;
import com.poketechtest.domain.model.PokemonSpecies;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPokemonDetailUseCase {

    private final PokemonCatalogPort pokemonCatalogPort;

    public PokemonDetail get(String idOrName) {
        String identifier = Objects.requireNonNull(idOrName, "idOrName must not be null").trim().toLowerCase(Locale.ROOT);

        Pokemon pokemon = pokemonCatalogPort.findByIdOrName(identifier)
                .orElseThrow(() -> new PokemonNotFoundException(identifier));
        PokemonSpecies species = findSpecies(pokemon);

        return new PokemonDetail(pokemon, species.description(), findEvolutionChain(species));
    }

    private PokemonSpecies findSpecies(Pokemon pokemon) {
        return Optional.ofNullable(pokemon.speciesName())
                .flatMap(pokemonCatalogPort::findSpecies)
                .orElseThrow(() -> new ExternalServiceException("Species not found for Pokemon: " + pokemon.name()));
    }

    private EvolutionNode findEvolutionChain(PokemonSpecies species) {
        if (species.evolutionChainId() == null) {
            return null;
        }
        Optional<EvolutionNode> evolutionChain = pokemonCatalogPort.findEvolutionChain(species.evolutionChainId());
        if (evolutionChain.isEmpty()) {
            log.warn("Evolution chain {} of species {} was not found", species.evolutionChainId(), species.name());
        }
        return evolutionChain.orElse(null);
    }
}
