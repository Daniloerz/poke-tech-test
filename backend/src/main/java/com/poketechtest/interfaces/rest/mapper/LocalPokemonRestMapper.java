package com.poketechtest.interfaces.rest.mapper;

import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.PageResult;
import com.poketechtest.interfaces.rest.dto.LocalPokemonResponse;
import com.poketechtest.interfaces.rest.dto.PageResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LocalPokemonRestMapper {

    @Mapping(target = "heightM", expression = "java(localPokemon.heightM())")
    @Mapping(target = "weightKg", expression = "java(localPokemon.weightKg())")
    LocalPokemonResponse toResponse(LocalPokemon localPokemon);

    List<LocalPokemonResponse> toResponses(List<LocalPokemon> localPokemon);

    default PageResponse<LocalPokemonResponse> toPageResponse(PageResult<LocalPokemon> page) {
        return new PageResponse<>(toResponses(page.items()), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
