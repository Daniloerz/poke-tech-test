package com.poketechtest.application.port.out;

import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonSpecies;
import java.util.Optional;

public interface PokemonCatalogPort {

    PokemonCatalogPage listPage(long offset, int limit);

    Optional<Pokemon> findByIdOrName(String idOrName);

    Optional<PokemonSpecies> findSpecies(String speciesName);

    Optional<EvolutionNode> findEvolutionChain(int evolutionChainId);
}
