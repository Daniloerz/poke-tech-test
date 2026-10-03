package com.poketechtest.infrastructure.pokeapi;

import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiMoveSlot;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiNamedResource;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPageResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPokemonResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiTypeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface PokeApiMapper {

    @Mapping(target = "names", source = "results")
    @Mapping(target = "totalCount", source = "count")
    PokemonCatalogPage toCatalogPage(PokeApiPageResponse response);

    @Mapping(target = "spriteUrl", source = "sprites.frontDefault")
    @Mapping(target = "weightHectograms", source = "weight")
    Pokemon toPokemon(PokeApiPokemonResponse response);

    default String toName(PokeApiNamedResource resource) {
        return resource == null ? null : resource.name();
    }

    default String toTypeName(PokeApiTypeSlot typeSlot) {
        return typeSlot == null ? null : toName(typeSlot.type());
    }

    default String toMoveName(PokeApiMoveSlot moveSlot) {
        return moveSlot == null ? null : toName(moveSlot.move());
    }
}
