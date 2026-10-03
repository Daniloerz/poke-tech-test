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
import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonSpecies;
import com.poketechtest.infrastructure.config.PokeApiProperties;
import java.net.SocketTimeoutException;
import java.time.Duration;
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
    private static final String SPRITE_BASE_URL = "https://sprites.test/pokemon";

    private MockRestServiceServer server;
    private PokeApiClient pokeApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        PokeApiProperties pokeApiProperties = new PokeApiProperties(
                BASE_URL, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofHours(1), SPRITE_BASE_URL);
        pokeApiClient = new PokeApiClient(builder.build(), Mappers.getMapper(PokeApiMapper.class), pokeApiProperties);
    }

    @Test
    void findSpeciesCallsTheSpeciesEndpoint() {
        server.expect(requestTo(BASE_URL + "/pokemon-species/eevee"))
                .andRespond(withSuccess("""
                        {"name": "eevee", "color": {"name": "brown"},
                         "flavor_text_entries": [{"flavor_text": "Harbors the\\npotential.", "language": {"name": "en", "url": "u"},
                                                  "version": {"name": "sword", "url": "u"}}],
                         "evolution_chain": {"url": "https://pokeapi.co/api/v2/evolution-chain/67/"}}
                        """, MediaType.APPLICATION_JSON));

        Optional<PokemonSpecies> species = pokeApiClient.findSpecies("eevee");

        assertThat(species).contains(new PokemonSpecies("eevee", "Harbors the potential.", 67));
    }

    @Test
    void findSpeciesReturnsEmptyWhenPokeApiAnswersNotFound() {
        server.expect(requestTo(BASE_URL + "/pokemon-species/missingno")).andRespond(withResourceNotFound());

        assertThat(pokeApiClient.findSpecies("missingno")).isEmpty();
    }

    @Test
    void findEvolutionChainCallsTheChainEndpointAndBuildsNodeImages() {
        server.expect(requestTo(BASE_URL + "/evolution-chain/67"))
                .andRespond(withSuccess("""
                        {"id": 67, "baby_trigger_item": null,
                         "chain": {"is_baby": false, "evolution_details": [],
                                   "species": {"name": "eevee", "url": "https://pokeapi.co/api/v2/pokemon-species/133/"},
                                   "evolves_to": [{"is_baby": false, "evolution_details": [], "evolves_to": [],
                                                   "species": {"name": "vaporeon", "url": "https://pokeapi.co/api/v2/pokemon-species/134/"}}]}}
                        """, MediaType.APPLICATION_JSON));

        Optional<EvolutionNode> chain = pokeApiClient.findEvolutionChain(67);

        assertThat(chain).hasValueSatisfying(root -> {
            assertThat(root.speciesId()).isEqualTo(133);
            assertThat(root.imageUrl()).isEqualTo(SPRITE_BASE_URL + "/133.png");
            assertThat(root.evolvesTo()).extracting(EvolutionNode::name).containsExactly("vaporeon");
        });
    }

    @Test
    void findEvolutionChainServerErrorBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/evolution-chain/67")).andRespond(withServerError());

        assertThatThrownBy(() -> pokeApiClient.findEvolutionChain(67)).isInstanceOf(ExternalServiceException.class);
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
    void findByIdOrNameMapsThePokemonAndIgnoresUnknownFields() {
        server.expect(requestTo(BASE_URL + "/pokemon/bulbasaur"))
                .andRespond(withSuccess("""
                        {"id": 1, "name": "bulbasaur", "weight": 69, "height": 7, "base_experience": 64,
                         "sprites": {"front_default": "https://sprites.test/1.png", "back_default": null},
                         "types": [{"slot": 1, "type": {"name": "grass", "url": "u"}},
                                   {"slot": 2, "type": {"name": "poison", "url": "u"}}],
                         "moves": [{"move": {"name": "razor-wind", "url": "u"}, "version_group_details": []}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<Pokemon> pokemon = pokeApiClient.findByIdOrName("bulbasaur");

        assertThat(pokemon).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(1);
            assertThat(found.spriteUrl()).isEqualTo("https://sprites.test/1.png");
            assertThat(found.types()).containsExactly("grass", "poison");
            assertThat(found.weightHectograms()).isEqualTo(69);
            assertThat(found.moves()).containsExactly("razor-wind");
        });
    }

    @Test
    void findByIdOrNameReturnsEmptyWhenPokeApiAnswersNotFound() {
        server.expect(requestTo(BASE_URL + "/pokemon/missingno")).andRespond(withResourceNotFound());

        assertThat(pokeApiClient.findByIdOrName("missingno")).isEmpty();
    }

    @Test
    void serverErrorBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon?offset=0&limit=20")).andRespond(withServerError());

        assertThatThrownBy(() -> pokeApiClient.listPage(0, 20)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void timeoutBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon/bulbasaur")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> pokeApiClient.findByIdOrName("bulbasaur")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void invalidBodyBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon/bulbasaur")).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> pokeApiClient.findByIdOrName("bulbasaur")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void emptyBodyBecomesExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/pokemon?offset=0&limit=20")).andRespond(withSuccess());

        assertThatThrownBy(() -> pokeApiClient.listPage(0, 20)).isInstanceOf(ExternalServiceException.class);
    }
}
