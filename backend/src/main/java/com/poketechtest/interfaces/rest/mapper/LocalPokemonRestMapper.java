package com.poketechtest.interfaces.rest.mapper;

import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.interfaces.rest.dto.LocalPokemonResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LocalPokemonRestMapper {

    @Mapping(target = "heightM", expression = "java(localPokemon.heightM())")
    @Mapping(target = "weightKg", expression = "java(localPokemon.weightKg())")
    LocalPokemonResponse toResponse(LocalPokemon localPokemon);
}
