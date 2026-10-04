package com.poketechtest.infrastructure.pokeapi;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonSpecies;
import com.poketechtest.domain.model.PokemonStat;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiChainLink;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiEvolutionChainResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiFlavorText;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiMoveSlot;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiNamedResource;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPageResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPokemonResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiSpeciesResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiStatSlot;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiTypeSlot;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PokeApiMapper {

    String ENGLISH_LANGUAGE = "en";

    @Mapping(target = "names", source = "results")
    @Mapping(target = "totalCount", source = "count")
    PokemonCatalogPage toCatalogPage(PokeApiPageResponse response);

    @Mapping(target = "speciesName", source = "species.name")
    @Mapping(target = "spriteUrl", source = "sprites.frontDefault")
    @Mapping(target = "artworkUrl", source = "sprites.other.officialArtwork.frontDefault")
    @Mapping(target = "heightDecimetres", source = "height")
    @Mapping(target = "weightHectograms", source = "weight")
    Pokemon toPokemon(PokeApiPokemonResponse response);

    default PokemonSpecies toSpecies(PokeApiSpeciesResponse response) {
        return new PokemonSpecies(
                response.name(),
                latestEnglishDescription(response.flavorTextEntries()),
                idFromUrl(response.evolutionChain()));
    }

    default EvolutionNode toEvolutionChain(PokeApiEvolutionChainResponse response, String spriteBaseUrl) {
        return response.chain() == null ? null : toEvolutionNode(response.chain(), spriteBaseUrl);
    }

    default String toName(PokeApiNamedResource resource) {
        return resource == null ? null : resource.name();
    }

    default String toTypeName(PokeApiTypeSlot typeSlot) {
        return typeSlot == null ? null : toName(typeSlot.type());
    }

    default String toMoveName(PokeApiMoveSlot moveSlot) {
        return moveSlot == null ? null : toName(moveSlot.move());
    }

    default PokemonStat toStat(PokeApiStatSlot statSlot) {
        return statSlot == null ? null : new PokemonStat(toName(statSlot.stat()), statSlot.baseStat());
    }

    private EvolutionNode toEvolutionNode(PokeApiChainLink link, String spriteBaseUrl) {
        Integer speciesId = idFromUrl(link.species());
        if (speciesId == null) {
            throw new ExternalServiceException("PokeAPI evolution chain link without species");
        }
        List<EvolutionNode> evolvesTo = link.evolvesTo() == null
                ? List.of()
                : link.evolvesTo().stream().map(child -> toEvolutionNode(child, spriteBaseUrl)).toList();
        // Same URL pattern PokeAPI uses for sprites.front_default of the default form.
        return new EvolutionNode(speciesId, toName(link.species()), spriteBaseUrl + "/" + speciesId + ".png", evolvesTo);
    }

    // PokeAPI orders flavor texts by game version: the last English entry is the most recent one.
    private String latestEnglishDescription(List<PokeApiFlavorText> flavorTexts) {
        if (flavorTexts == null) {
            return null;
        }
        return flavorTexts.stream()
                .filter(Objects::nonNull)
                .filter(flavorText -> flavorText.flavorText() != null)
                .filter(flavorText -> ENGLISH_LANGUAGE.equals(toName(flavorText.language())))
                .reduce((previous, next) -> next)
                .map(flavorText -> flavorText.flavorText().replaceAll("\\s+", " ").trim())
                .orElse(null);
    }

    // PokeAPI resource URLs end with the id: https://pokeapi.co/api/v2/evolution-chain/67/
    private Integer idFromUrl(PokeApiNamedResource resource) {
        if (resource == null || resource.url() == null) {
            return null;
        }
        String url = resource.url().endsWith("/") ? resource.url().substring(0, resource.url().length() - 1) : resource.url();
        String lastSegment = url.substring(url.lastIndexOf('/') + 1);
        try {
            return Integer.valueOf(lastSegment);
        } catch (NumberFormatException exception) {
            throw new ExternalServiceException("PokeAPI returned a resource URL without id: " + resource.url(), exception);
        }
    }
}
