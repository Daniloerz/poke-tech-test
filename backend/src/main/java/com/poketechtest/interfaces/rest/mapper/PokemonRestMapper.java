package com.poketechtest.interfaces.rest.mapper;

import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonDetail;
import com.poketechtest.domain.model.PokemonPage;
import com.poketechtest.domain.model.PokemonStat;
import com.poketechtest.interfaces.rest.dto.EvolutionNodeResponse;
import com.poketechtest.interfaces.rest.dto.PageResponse;
import com.poketechtest.interfaces.rest.dto.PokemonDetailResponse;
import com.poketechtest.interfaces.rest.dto.PokemonStatResponse;
import com.poketechtest.interfaces.rest.dto.PokemonSummaryResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PokemonRestMapper {

    @Mapping(target = "weightKg", expression = "java(pokemon.weightKg())")
    PokemonSummaryResponse toSummaryResponse(Pokemon pokemon);

    List<PokemonSummaryResponse> toSummaryResponses(List<Pokemon> pokemon);

    @Mapping(target = "id", source = "pokemon.id")
    @Mapping(target = "name", source = "pokemon.name")
    @Mapping(target = "types", source = "pokemon.types")
    @Mapping(target = "stats", source = "pokemon.stats")
    @Mapping(target = "imageUrl", expression = "java(detail.pokemon().imageUrl())")
    @Mapping(target = "heightM", expression = "java(detail.pokemon().heightM())")
    @Mapping(target = "weightKg", expression = "java(detail.pokemon().weightKg())")
    PokemonDetailResponse toDetailResponse(PokemonDetail detail);

    PokemonStatResponse toStatResponse(PokemonStat stat);

    @Mapping(target = "id", source = "speciesId")
    EvolutionNodeResponse toEvolutionNodeResponse(EvolutionNode node);

    default PageResponse<PokemonSummaryResponse> toPageResponse(PokemonPage pokemonPage) {
        return new PageResponse<>(
                toSummaryResponses(pokemonPage.items()),
                pokemonPage.page(),
                pokemonPage.size(),
                pokemonPage.totalElements(),
                pokemonPage.totalPages());
    }
}
