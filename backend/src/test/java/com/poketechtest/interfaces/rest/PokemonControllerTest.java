package com.poketechtest.interfaces.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.usecase.ListPokemonUseCase;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonPage;
import com.poketechtest.interfaces.rest.mapper.PokemonRestMapperImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
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

    @Test
    void returnsThePageWithTheListFields() throws Exception {
        Pokemon bulbasaur = new Pokemon(1, "bulbasaur", "https://sprites.test/1.png", List.of("grass", "poison"), 69, List.of("razor-wind"));
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
