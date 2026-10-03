package com.poketechtest.interfaces.rest.mapper;

import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonPage;
import com.poketechtest.interfaces.rest.dto.PageResponse;
import com.poketechtest.interfaces.rest.dto.PokemonSummaryResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface PokemonRestMapper {

    @Mapping(target = "weightKg", expression = "java(pokemon.weightKg())")
    PokemonSummaryResponse toSummaryResponse(Pokemon pokemon);

    List<PokemonSummaryResponse> toSummaryResponses(List<Pokemon> pokemon);

    default PageResponse<PokemonSummaryResponse> toPageResponse(PokemonPage pokemonPage) {
        return new PageResponse<>(
                toSummaryResponses(pokemonPage.items()),
                pokemonPage.page(),
                pokemonPage.size(),
                pokemonPage.totalElements(),
                pokemonPage.totalPages());
    }
}
