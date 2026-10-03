package com.poketechtest.infrastructure.persistence;

import com.poketechtest.domain.model.LocalPokemon;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LocalPokemonEntityMapper {

    LocalPokemon toDomain(LocalPokemonEntity entity);

    LocalPokemonEntity toEntity(LocalPokemon localPokemon);
}
