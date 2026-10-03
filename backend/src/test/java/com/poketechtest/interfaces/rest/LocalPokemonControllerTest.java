package com.poketechtest.interfaces.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.exception.LocalPokemonNotFoundException;
import com.poketechtest.application.exception.PokemonAlreadySyncedException;
import com.poketechtest.application.exception.PokemonNotFoundException;
import com.poketechtest.application.usecase.DeleteLocalPokemonUseCase;
import com.poketechtest.application.usecase.GetLocalPokemonUseCase;
import com.poketechtest.application.usecase.ListLocalPokemonUseCase;
import com.poketechtest.application.usecase.SyncPokemonUseCase;
import com.poketechtest.application.usecase.UpdateLocalPokemonUseCase;
import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.PageResult;
import com.poketechtest.interfaces.rest.mapper.LocalPokemonRestMapperImpl;
import java.time.Instant;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(LocalPokemonController.class)
@Import(LocalPokemonRestMapperImpl.class)
@WithSecurityConfig
class LocalPokemonControllerTest {

    private static final String LOCAL_POKEMON_PATH = "/api/v1/local-pokemon";
    private static final Instant NOW = Instant.parse("2026-10-03T23:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncPokemonUseCase syncPokemonUseCase;

    @MockitoBean
    private GetLocalPokemonUseCase getLocalPokemonUseCase;

    @MockitoBean
    private ListLocalPokemonUseCase listLocalPokemonUseCase;

    @MockitoBean
    private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

    @MockitoBean
    private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

    @Test
    void listReturnsThePageWithDefaults() throws Exception {
        when(listLocalPokemonUseCase.list(0, 20)).thenReturn(new PageResult<>(List.of(pikachu()), 0, 20, 1));

        mockMvc.perform(authenticated(get(LOCAL_POKEMON_PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.content[0].name").value("pikachu"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listRejectsInvalidPagination() throws Exception {
        mockMvc.perform(authenticated(get(LOCAL_POKEMON_PATH).param("page", "-1").param("size", "51")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("page")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("size")));

        verifyNoInteractions(listLocalPokemonUseCase);
    }

    @Test
    void updateReturnsTheUpdatedRecord() throws Exception {
        LocalPokemon updated = pikachu().withProprietaryFields("Pikachu", "Kanto", List.of("mascot"));
        when(updateLocalPokemonUseCase.update(4L, "Pikachu", "Kanto", List.of("Mascot"))).thenReturn(updated);

        mockMvc.perform(authenticated(updateRequest(4, """
                        {"localizedName": "Pikachu", "region": "Kanto", "tags": ["Mascot"]}""")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localizedName").value("Pikachu"))
                .andExpect(jsonPath("$.region").value("Kanto"))
                .andExpect(jsonPath("$.tags[0]").value("mascot"))
                .andExpect(jsonPath("$.name").value("pikachu"));
    }

    @Test
    void updateAcceptsNullTextsAndEmptyTagsToClearThem() throws Exception {
        when(updateLocalPokemonUseCase.update(4L, null, null, List.of())).thenReturn(pikachu());

        mockMvc.perform(authenticated(updateRequest(4, """
                        {"localizedName": null, "tags": []}""")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"localizedName\": \"   \", \"tags\": []}",
            "{\"region\": \"\", \"tags\": []}",
            "{\"tags\": null}",
            "{}",
            "{\"tags\": [\"bad tag\"]}",
            "{\"tags\": [\"\"]}",
            "{\"tags\": [\"a\", \"b\", \"c\", \"d\", \"e\", \"f\", \"g\", \"h\", \"i\", \"j\", \"k\"]}"
    })
    void updateRejectsInvalidProprietaryFields(String body) throws Exception {
        mockMvc.perform(authenticated(updateRequest(4, body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void updateRejectsTooLongTextsAndTags() throws Exception {
        mockMvc.perform(authenticated(updateRequest(4, "{\"localizedName\": \"%s\", \"tags\": []}".formatted("a".repeat(101)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("localizedName"));

        mockMvc.perform(authenticated(updateRequest(4, "{\"tags\": [\"%s\"]}".formatted("a".repeat(31)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tags[0]"));
    }

    @Test
    void updateRejectsSnapshotFieldsNamingTheField() throws Exception {
        mockMvc.perform(authenticated(updateRequest(4, """
                        {"name": "raichu", "tags": []}""")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("is not a recognized field"));

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void updateRejectsAMalformedBodyAndAWrongContentType() throws Exception {
        mockMvc.perform(authenticated(updateRequest(4, "{\"tags\": [")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.MALFORMED_BODY_DETAIL));

        mockMvc.perform(authenticated(put(LOCAL_POKEMON_PATH + "/4").contentType(MediaType.TEXT_PLAIN).content("tags")))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void updateReturnsNotFoundForAnUnknownId() throws Exception {
        when(updateLocalPokemonUseCase.update(99L, null, null, List.of())).thenThrow(new LocalPokemonNotFoundException(99L));

        mockMvc.perform(authenticated(updateRequest(99, "{\"tags\": []}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Local Pokemon not found: 99"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(authenticated(delete(LOCAL_POKEMON_PATH + "/4")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(deleteLocalPokemonUseCase).delete(4L);
    }

    @Test
    void deleteReturnsNotFoundForAnUnknownIdAndBadRequestForANonNumericId() throws Exception {
        doThrow(new LocalPokemonNotFoundException(99L)).when(deleteLocalPokemonUseCase).delete(99L);

        mockMvc.perform(authenticated(delete(LOCAL_POKEMON_PATH + "/99"))).andExpect(status().isNotFound());
        mockMvc.perform(authenticated(delete(LOCAL_POKEMON_PATH + "/abc"))).andExpect(status().isBadRequest());
    }

    @Test
    void managementRoutesNeedAToken() throws Exception {
        mockMvc.perform(get(LOCAL_POKEMON_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(updateRequest(4, "{\"tags\": []}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(LOCAL_POKEMON_PATH + "/4")).andExpect(status().isUnauthorized());

        verifyNoInteractions(listLocalPokemonUseCase, updateLocalPokemonUseCase, deleteLocalPokemonUseCase);
    }

    @Test
    void syncReturnsCreatedWithTheLocalRecordAndLocation() throws Exception {
        when(syncPokemonUseCase.sync("pikachu")).thenReturn(pikachu());

        mockMvc.perform(authenticated(syncRequest("""
                        {"idOrName": "pikachu"}""")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", LOCAL_POKEMON_PATH + "/4"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.pokeApiId").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.spriteUrl").value("https://sprites.test/25.png"))
                .andExpect(jsonPath("$.types[0]").value("electric"))
                .andExpect(jsonPath("$.heightM").value(0.4))
                .andExpect(jsonPath("$.weightKg").value(6.0))
                .andExpect(jsonPath("$.localizedName").isEmpty())
                .andExpect(jsonPath("$.region").isEmpty())
                .andExpect(jsonPath("$.tags").isEmpty())
                .andExpect(jsonPath("$.syncedAt").value("2026-10-03T23:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-03T23:00:00Z"));
    }

    @Test
    void syncReturnsConflictWhenThePokemonIsAlreadyLocal() throws Exception {
        when(syncPokemonUseCase.sync("pikachu")).thenThrow(new PokemonAlreadySyncedException("pikachu", 4L));

        mockMvc.perform(authenticated(syncRequest("""
                        {"idOrName": "pikachu"}""")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Pokemon already synchronized: pikachu (local id 4)"));
    }

    @Test
    void syncReturnsNotFoundWhenPokeApiDoesNotHaveThePokemon() throws Exception {
        when(syncPokemonUseCase.sync("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

        mockMvc.perform(authenticated(syncRequest("""
                        {"idOrName": "missingno"}""")))
                .andExpect(status().isNotFound());
    }

    @Test
    void syncReturnsBadGatewayWhenPokeApiIsNotAvailable() throws Exception {
        when(syncPokemonUseCase.sync(anyString())).thenThrow(new ExternalServiceException("PokeAPI is not available"));

        mockMvc.perform(authenticated(syncRequest("""
                        {"idOrName": "pikachu"}""")))
                .andExpect(status().isBadGateway());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"idOrName\": \"\"}", "{\"idOrName\": \"mr.mime\"}", "{\"idOrName\": \"pika chu\"}"})
    void syncRejectsAMissingOrInvalidIdentifier(String body) throws Exception {
        mockMvc.perform(authenticated(syncRequest(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("idOrName")));

        verifyNoInteractions(syncPokemonUseCase);
    }

    @Test
    void syncRejectsATooLongIdentifierAndAMalformedBody() throws Exception {
        mockMvc.perform(authenticated(syncRequest("{\"idOrName\": \"%s\"}".formatted("a".repeat(51)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("idOrName")));

        mockMvc.perform(authenticated(syncRequest("{\"idOrName\": ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.MALFORMED_BODY_DETAIL));

        verifyNoInteractions(syncPokemonUseCase);
    }

    @Test
    void getReturnsTheLocalRecord() throws Exception {
        when(getLocalPokemonUseCase.get(4L)).thenReturn(pikachu());

        mockMvc.perform(authenticated(get(LOCAL_POKEMON_PATH + "/4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("pikachu"));
    }

    @Test
    void getReturnsNotFoundForAnUnknownId() throws Exception {
        when(getLocalPokemonUseCase.get(99L)).thenThrow(new LocalPokemonNotFoundException(99L));

        mockMvc.perform(authenticated(get(LOCAL_POKEMON_PATH + "/99")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Local Pokemon not found: 99"));
    }

    @Test
    void getRejectsANonNumericId() throws Exception {
        mockMvc.perform(authenticated(get(LOCAL_POKEMON_PATH + "/pikachu")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void everyRouteNeedsAToken() throws Exception {
        mockMvc.perform(syncRequest("{\"idOrName\": \"pikachu\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(LOCAL_POKEMON_PATH + "/4")).andExpect(status().isUnauthorized());

        verifyNoInteractions(syncPokemonUseCase, getLocalPokemonUseCase);
    }

    private MockHttpServletRequestBuilder updateRequest(long id, String body) {
        return put(LOCAL_POKEMON_PATH + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder syncRequest(String body) {
        return post(LOCAL_POKEMON_PATH).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(token -> token.subject("ash")));
    }

    private LocalPokemon pikachu() {
        return LocalPokemon.builder()
                .id(4L)
                .pokeApiId(25)
                .name("pikachu")
                .spriteUrl("https://sprites.test/25.png")
                .types(List.of("electric"))
                .heightDecimetres(4)
                .weightHectograms(60)
                .syncedAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
