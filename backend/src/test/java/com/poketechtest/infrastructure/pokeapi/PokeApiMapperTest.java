package com.poketechtest.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiMoveSlot;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiNamedResource;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPageResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPokemonResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiSprites;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiTypeSlot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PokeApiMapperTest {

    private final PokeApiMapper pokeApiMapper = Mappers.getMapper(PokeApiMapper.class);

    @Test
    void mapsThePokemonFieldsUsedByTheList() {
        PokeApiPokemonResponse response = new PokeApiPokemonResponse(
                1,
                "bulbasaur",
                69,
                new PokeApiSprites("https://sprites.test/1.png"),
                List.of(typeSlot(1, "grass"), typeSlot(2, "poison")),
                List.of(moveSlot("razor-wind"), moveSlot("swords-dance")));

        Pokemon pokemon = pokeApiMapper.toPokemon(response);

        assertThat(pokemon.id()).isEqualTo(1);
        assertThat(pokemon.name()).isEqualTo("bulbasaur");
        assertThat(pokemon.spriteUrl()).isEqualTo("https://sprites.test/1.png");
        assertThat(pokemon.types()).containsExactly("grass", "poison");
        assertThat(pokemon.weightHectograms()).isEqualTo(69);
        assertThat(pokemon.moves()).containsExactly("razor-wind", "swords-dance");
    }

    @Test
    void mapsMissingSpritesAndListsWithoutFailing() {
        PokeApiPokemonResponse response = new PokeApiPokemonResponse(10001, "deoxys-attack", 608, null, null, null);

        Pokemon pokemon = pokeApiMapper.toPokemon(response);

        assertThat(pokemon.spriteUrl()).isNull();
        assertThat(pokemon.types()).isEmpty();
        assertThat(pokemon.moves()).isEmpty();
    }

    @Test
    void mapsThePageNamesAndTotalCount() {
        PokeApiPageResponse response = new PokeApiPageResponse(1351, List.of(
                new PokeApiNamedResource("bulbasaur", "https://pokeapi.test/pokemon/1/"),
                new PokeApiNamedResource("ivysaur", "https://pokeapi.test/pokemon/2/")));

        PokemonCatalogPage page = pokeApiMapper.toCatalogPage(response);

        assertThat(page.names()).containsExactly("bulbasaur", "ivysaur");
        assertThat(page.totalCount()).isEqualTo(1351);
    }

    private PokeApiTypeSlot typeSlot(int slot, String typeName) {
        return new PokeApiTypeSlot(slot, new PokeApiNamedResource(typeName, null));
    }

    private PokeApiMoveSlot moveSlot(String moveName) {
        return new PokeApiMoveSlot(new PokeApiNamedResource(moveName, null));
    }
}
