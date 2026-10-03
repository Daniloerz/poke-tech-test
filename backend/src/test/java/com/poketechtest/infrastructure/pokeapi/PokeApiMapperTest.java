package com.poketechtest.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonSpecies;
import com.poketechtest.domain.model.PokemonStat;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiArtwork;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiChainLink;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiEvolutionChainResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiFlavorText;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiMoveSlot;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiNamedResource;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiOtherSprites;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPageResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPokemonResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiSpeciesResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiSprites;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiStatSlot;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiTypeSlot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PokeApiMapperTest {

    private static final String SPRITE_BASE_URL = "https://sprites.test/pokemon";

    private final PokeApiMapper pokeApiMapper = Mappers.getMapper(PokeApiMapper.class);

    @Test
    void mapsThePokemonFields() {
        PokeApiPokemonResponse response = new PokeApiPokemonResponse(
                1,
                "bulbasaur",
                7,
                69,
                resource("bulbasaur", "https://pokeapi.test/pokemon-species/1/"),
                new PokeApiSprites("https://sprites.test/1.png", new PokeApiOtherSprites(new PokeApiArtwork("https://artwork.test/1.png"))),
                List.of(typeSlot(1, "grass"), typeSlot(2, "poison")),
                List.of(statSlot("hp", 45), statSlot("attack", 49)),
                List.of(moveSlot("razor-wind"), moveSlot("swords-dance")));

        Pokemon pokemon = pokeApiMapper.toPokemon(response);

        assertThat(pokemon.id()).isEqualTo(1);
        assertThat(pokemon.name()).isEqualTo("bulbasaur");
        assertThat(pokemon.speciesName()).isEqualTo("bulbasaur");
        assertThat(pokemon.spriteUrl()).isEqualTo("https://sprites.test/1.png");
        assertThat(pokemon.artworkUrl()).isEqualTo("https://artwork.test/1.png");
        assertThat(pokemon.types()).containsExactly("grass", "poison");
        assertThat(pokemon.heightDecimetres()).isEqualTo(7);
        assertThat(pokemon.weightHectograms()).isEqualTo(69);
        assertThat(pokemon.stats()).containsExactly(new PokemonStat("hp", 45), new PokemonStat("attack", 49));
        assertThat(pokemon.moves()).containsExactly("razor-wind", "swords-dance");
    }

    @Test
    void mapsMissingSpritesAndListsWithoutFailing() {
        PokeApiPokemonResponse response = new PokeApiPokemonResponse(10001, "deoxys-attack", 17, 608, null, null, null, null, null);

        Pokemon pokemon = pokeApiMapper.toPokemon(response);

        assertThat(pokemon.spriteUrl()).isNull();
        assertThat(pokemon.artworkUrl()).isNull();
        assertThat(pokemon.speciesName()).isNull();
        assertThat(pokemon.types()).isEmpty();
        assertThat(pokemon.stats()).isEmpty();
        assertThat(pokemon.moves()).isEmpty();
    }

    @Test
    void mapsThePageNamesAndTotalCount() {
        PokeApiPageResponse response = new PokeApiPageResponse(1351, List.of(
                resource("bulbasaur", "https://pokeapi.test/pokemon/1/"),
                resource("ivysaur", "https://pokeapi.test/pokemon/2/")));

        PokemonCatalogPage page = pokeApiMapper.toCatalogPage(response);

        assertThat(page.names()).containsExactly("bulbasaur", "ivysaur");
        assertThat(page.totalCount()).isEqualTo(1351);
    }

    @Test
    void speciesUsesTheLastEnglishDescriptionWithNormalizedWhitespace() {
        PokeApiSpeciesResponse response = new PokeApiSpeciesResponse(
                "eevee",
                List.of(
                        flavorText("Its genetic code\nis irregular.", "en"),
                        flavorText("Son code génétique est instable.", "fr"),
                        flavorText("Harbors the potential\nto evolve\finto  manifold forms. ", "en"),
                        flavorText("Su código genético es irregular.", "es")),
                resource(null, "https://pokeapi.test/evolution-chain/67/"));

        PokemonSpecies species = pokeApiMapper.toSpecies(response);

        assertThat(species.name()).isEqualTo("eevee");
        assertThat(species.description()).isEqualTo("Harbors the potential to evolve into manifold forms.");
        assertThat(species.evolutionChainId()).isEqualTo(67);
    }

    @Test
    void speciesWithoutEnglishDescriptionOrChainHasNulls() {
        PokeApiSpeciesResponse response = new PokeApiSpeciesResponse("eevee", List.of(flavorText("Texte.", "fr")), null);

        PokemonSpecies species = pokeApiMapper.toSpecies(response);

        assertThat(species.description()).isNull();
        assertThat(species.evolutionChainId()).isNull();
    }

    @Test
    void speciesWithAnInvalidChainUrlIsAnExternalServiceError() {
        PokeApiSpeciesResponse response = new PokeApiSpeciesResponse("eevee", List.of(), resource(null, "https://pokeapi.test/evolution-chain/abc/"));

        assertThatThrownBy(() -> pokeApiMapper.toSpecies(response)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void evolutionChainKeepsBranchesAndNestedStages() {
        // wurmple → silcoon → beautifly, and wurmple → cascoon → dustox
        PokeApiChainLink chain = link("wurmple", 265,
                link("silcoon", 266, link("beautifly", 267)),
                link("cascoon", 268, link("dustox", 269)));

        EvolutionNode root = pokeApiMapper.toEvolutionChain(new PokeApiEvolutionChainResponse(135, chain), SPRITE_BASE_URL);

        assertThat(root.speciesId()).isEqualTo(265);
        assertThat(root.name()).isEqualTo("wurmple");
        assertThat(root.imageUrl()).isEqualTo(SPRITE_BASE_URL + "/265.png");
        assertThat(root.evolvesTo()).extracting(EvolutionNode::name).containsExactly("silcoon", "cascoon");
        assertThat(root.evolvesTo().get(1).evolvesTo()).extracting(EvolutionNode::name).containsExactly("dustox");
        assertThat(root.evolvesTo().get(1).evolvesTo().get(0).evolvesTo()).isEmpty();
    }

    @Test
    void evolutionChainWithoutChainIsNull() {
        assertThat(pokeApiMapper.toEvolutionChain(new PokeApiEvolutionChainResponse(1, null), SPRITE_BASE_URL)).isNull();
    }

    private PokeApiNamedResource resource(String name, String url) {
        return new PokeApiNamedResource(name, url);
    }

    private PokeApiTypeSlot typeSlot(int slot, String typeName) {
        return new PokeApiTypeSlot(slot, resource(typeName, null));
    }

    private PokeApiStatSlot statSlot(String statName, int baseStat) {
        return new PokeApiStatSlot(baseStat, resource(statName, null));
    }

    private PokeApiMoveSlot moveSlot(String moveName) {
        return new PokeApiMoveSlot(resource(moveName, null));
    }

    private PokeApiFlavorText flavorText(String text, String language) {
        return new PokeApiFlavorText(text, resource(language, null));
    }

    private PokeApiChainLink link(String speciesName, int speciesId, PokeApiChainLink... evolvesTo) {
        return new PokeApiChainLink(resource(speciesName, "https://pokeapi.test/pokemon-species/" + speciesId + "/"), List.of(evolvesTo));
    }
}
