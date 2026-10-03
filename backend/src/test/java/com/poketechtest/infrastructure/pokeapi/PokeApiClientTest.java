package com.poketechtest.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.domain.model.Pokemon;
import java.net.SocketTimeoutException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PokeApiClientTest {

    private static final String BASE_URL = "https://pokeapi.test/api/v2";

    private MockRestServiceServer server;
    private PokeApiClient pokeApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        pokeApiClient = new PokeApiClient(builder.build(), Mappers.getMapper(PokeApiMapper.class));
    }

    @Test
    void listPageCallsTheListEndpointAndMapsTheNames() {
        server.expect(requestTo(BASE_URL + "/pokemon?offset=40&limit=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"count": 1351, "next": null, "previous": null, "results": [
                          {"name": "pidgey", "url": "https://pokeapi.co/api/v2/pokemon/16/"},
                          {"name": "rattata", "url": "https://pokeapi.co/api/v2/pokemon/19/"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        PokemonCatalogPage page = pokeApiClient.listPage(40, 2);

        assertThat(page.names()).containsExactly("pidgey", "rattata");
        assertThat(page.totalCount()).isEqualTo(1351);
        server.verify();
    }

    @Test
    void findByNameMapsThePokemonAndIgnoresUnknownFields() {
        server.expect(requestTo(BASE_URL + "/pokemon/bulbasaur"))
                .andRespond(withSuccess("""
                        {"id": 1, "name": "bulbasaur", "weight": 69, "height": 7, "base_experience": 64,
                         "sprites": {"front_default": "https://sprites.test/1.png", "back_default": null},
                         "types": [{"slot": 1, "type": {"name": "grass", "url": "u"}},
                                   {"slot": 2, "type": {"name": "poison", "url": "u"}}],
                         "moves": [{"move": {"name": "razor-wind", "url": "u"}, "version_group_details": []}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<Pokemon> pokemon = pokeApiClient.findByName("bulbasaur");

        assertThat(pokemon).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(1);
            assertThat(found.spriteUrl()).isEqualTo("https://sprites.test/1.png");
            assertThat(found.types()).containsExactly("grass", "poison");
            assertThat(found.weightHectograms()).isEqualTo(69);
            assertThat(found.moves()).containsExactly("razor-wind");
        });
    }

    @Test
    void findByNameReturnsEmptyWhenPokeApiAnswersNotFound() {
        server.expect(requestTo(BASE_URL + "/pokemon/missingno")).andRespond(withResourceNotFound());

        assertThat(pokeApiClient.findByName("missingno")).isEmpty();
    }

    @Test
    void serverErrorBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon?offset=0&limit=20")).andRespond(withServerError());

        assertThatThrownBy(() -> pokeApiClient.listPage(0, 20)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void timeoutBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon/bulbasaur")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> pokeApiClient.findByName("bulbasaur")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void invalidBodyBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon/bulbasaur")).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> pokeApiClient.findByName("bulbasaur")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void emptyBodyBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon?offset=0&limit=20")).andRespond(withSuccess());

        assertThatThrownBy(() -> pokeApiClient.listPage(0, 20)).isInstanceOf(ExternalServiceException.class);
    }
}
