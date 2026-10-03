package com.poketechtest.interfaces.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.exception.PokemonNotFoundException;
import com.poketechtest.application.usecase.GetPokemonDetailUseCase;
import com.poketechtest.application.usecase.ListPokemonUseCase;
import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonDetail;
import com.poketechtest.domain.model.PokemonPage;
import com.poketechtest.domain.model.PokemonStat;
import com.poketechtest.interfaces.rest.mapper.PokemonRestMapperImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PokemonController.class)
@Import(PokemonRestMapperImpl.class)
class PokemonControllerTest {

    private static final String POKEMON_PATH = "/api/v1/pokemon";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPokemonUseCase listPokemonUseCase;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetailUseCase;

    @Test
    void returnsTheDetailWithStatsDescriptionAndEvolutionTree() throws Exception {
        Pokemon eevee = Pokemon.builder()
                .id(133)
                .name("eevee")
                .speciesName("eevee")
                .spriteUrl("https://sprites.test/133.png")
                .artworkUrl("https://artwork.test/133.png")
                .types(List.of("normal"))
                .heightDecimetres(3)
                .weightHectograms(65)
                .stats(List.of(new PokemonStat("hp", 55), new PokemonStat("speed", 55)))
                .moves(List.of("tackle"))
                .build();
        EvolutionNode chain = new EvolutionNode(133, "eevee", "https://sprites.test/133.png",
                List.of(new EvolutionNode(134, "vaporeon", "https://sprites.test/134.png", List.of())));
        when(getPokemonDetailUseCase.get("eevee")).thenReturn(new PokemonDetail(eevee, "Evolves in many ways.", chain));

        mockMvc.perform(get(POKEMON_PATH + "/eevee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(133))
                .andExpect(jsonPath("$.name").value("eevee"))
                .andExpect(jsonPath("$.imageUrl").value("https://artwork.test/133.png"))
                .andExpect(jsonPath("$.types[0]").value("normal"))
                .andExpect(jsonPath("$.heightM").value(0.3))
                .andExpect(jsonPath("$.weightKg").value(6.5))
                .andExpect(jsonPath("$.stats[0].name").value("hp"))
                .andExpect(jsonPath("$.stats[0].baseStat").value(55))
                .andExpect(jsonPath("$.description").value("Evolves in many ways."))
                .andExpect(jsonPath("$.evolutionChain.id").value(133))
                .andExpect(jsonPath("$.evolutionChain.evolvesTo[0].name").value("vaporeon"))
                .andExpect(jsonPath("$.evolutionChain.evolvesTo[0].imageUrl").value("https://sprites.test/134.png"))
                .andExpect(jsonPath("$.evolutionChain.evolvesTo[0].evolvesTo").isEmpty())
                .andExpect(jsonPath("$.moves").doesNotExist());
    }

    @Test
    void returnsNullEvolutionChainWhenThereIsNone() throws Exception {
        Pokemon pokemon = Pokemon.builder().id(132).name("ditto").build();
        when(getPokemonDetailUseCase.get("132")).thenReturn(new PokemonDetail(pokemon, null, null));

        mockMvc.perform(get(POKEMON_PATH + "/132"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolutionChain").value(nullValue()))
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void returnsNotFoundForAnUnknownPokemon() throws Exception {
        when(getPokemonDetailUseCase.get("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

        mockMvc.perform(get(POKEMON_PATH + "/missingno"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Pokemon not found: missingno"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad_name", "mr.mime", "pika%20chu"})
    void rejectsAnIdentifierWithInvalidCharacters(String identifier) throws Exception {
        mockMvc.perform(get(POKEMON_PATH + "/" + identifier))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("idOrName"));

        verifyNoInteractions(getPokemonDetailUseCase);
    }

    @Test
    void rejectsATooLongIdentifier() throws Exception {
        mockMvc.perform(get(POKEMON_PATH + "/" + "a".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("idOrName"));

        verifyNoInteractions(getPokemonDetailUseCase);
    }

    @Test
    void returnsBadGatewayWhenPokeApiFailsOnTheDetail() throws Exception {
        when(getPokemonDetailUseCase.get(anyString())).thenThrow(new ExternalServiceException("PokeAPI is not available"));

        mockMvc.perform(get(POKEMON_PATH + "/eevee"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.EXTERNAL_SERVICE_DETAIL));
    }

    @Test
    void returnsThePageWithTheListFields() throws Exception {
        Pokemon bulbasaur = Pokemon.builder()
                .id(1)
                .name("bulbasaur")
                .spriteUrl("https://sprites.test/1.png")
                .types(List.of("grass", "poison"))
                .weightHectograms(69)
                .moves(List.of("razor-wind"))
                .build();
        when(listPokemonUseCase.list(0, 20)).thenReturn(new PokemonPage(List.of(bulbasaur), 0, 20, 1351));

        mockMvc.perform(get(POKEMON_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.content[0].spriteUrl").value("https://sprites.test/1.png"))
                .andExpect(jsonPath("$.content[0].types[1]").value("poison"))
                .andExpect(jsonPath("$.content[0].weightKg").value(6.9))
                .andExpect(jsonPath("$.content[0].moves[0]").value("razor-wind"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1351))
                .andExpect(jsonPath("$.totalPages").value(68));
    }

    @Test
    void usesTheRequestedPageAndSize() throws Exception {
        when(listPokemonUseCase.list(3, 50)).thenReturn(new PokemonPage(List.of(), 3, 50, 1351));

        mockMvc.perform(get(POKEMON_PATH).param("page", "3").param("size", "50"))
                .andExpect(status().isOk());

        verify(listPokemonUseCase).list(3, 50);
    }

    @Test
    void rejectsASizeOverTheLimit() throws Exception {
        mockMvc.perform(get(POKEMON_PATH).param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].field", hasItem("size")));

        verifyNoInteractions(listPokemonUseCase);
    }

    @Test
    void rejectsANegativePageAndAZeroSize() throws Exception {
        mockMvc.perform(get(POKEMON_PATH).param("page", "-1").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("page")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("size")));
    }

    @Test
    void rejectsANonNumericParameter() throws Exception {
        mockMvc.perform(get(POKEMON_PATH).param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));

        verifyNoInteractions(listPokemonUseCase);
    }

    @Test
    void returnsBadGatewayWhenPokeApiIsNotAvailable() throws Exception {
        when(listPokemonUseCase.list(anyInt(), anyInt())).thenThrow(new ExternalServiceException("PokeAPI is not available"));

        mockMvc.perform(get(POKEMON_PATH))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.EXTERNAL_SERVICE_DETAIL))
                .andExpect(jsonPath("$.instance").value(POKEMON_PATH));
    }

    @Test
    void hidesTheDetailsOfUnexpectedErrors() throws Exception {
        when(listPokemonUseCase.list(anyInt(), anyInt())).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get(POKEMON_PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.UNEXPECTED_ERROR_DETAIL));
    }
}
