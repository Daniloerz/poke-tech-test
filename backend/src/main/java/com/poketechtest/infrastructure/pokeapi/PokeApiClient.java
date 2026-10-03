package com.poketechtest.infrastructure.pokeapi;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.application.port.out.PokemonCatalogPort;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.infrastructure.config.CacheNames;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPageResponse;
import com.poketechtest.infrastructure.pokeapi.dto.PokeApiPokemonResponse;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PokeApiClient implements PokemonCatalogPort {

    private static final String UNAVAILABLE_MESSAGE = "PokeAPI is not available";

    private final RestClient pokeApiRestClient;
    private final PokeApiMapper pokeApiMapper;

    @Override
    @Cacheable(cacheNames = CacheNames.POKEAPI_PAGES, key = "#offset + '-' + #limit")
    public PokemonCatalogPage listPage(long offset, int limit) {
        String operation = "list offset=" + offset + " limit=" + limit;
        return get(operation, () -> pokeApiRestClient.get()
                        .uri("/pokemon?offset={offset}&limit={limit}", offset, limit)
                        .retrieve()
                        .body(PokeApiPageResponse.class))
                .map(pokeApiMapper::toCatalogPage)
                .orElseThrow(() -> unavailable(operation, "list endpoint answered 404"));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.POKEAPI_POKEMON, key = "#name", unless = "#result == null")
    public Optional<Pokemon> findByName(String name) {
        return get("pokemon name=" + name, () -> pokeApiRestClient.get()
                        .uri("/pokemon/{name}", name)
                        .retrieve()
                        .body(PokeApiPokemonResponse.class))
                .map(pokeApiMapper::toPokemon);
    }

    /** Returns empty on 404; any other failure means PokeAPI is not available. */
    private <T> Optional<T> get(String operation, Supplier<T> request) {
        log.debug("Calling PokeAPI: {}", operation);
        try {
            T body = request.get();
            if (body == null) {
                throw unavailable(operation, "empty body");
            }
            return Optional.of(body);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw unavailable(operation, exception.getMessage(), exception);
        }
    }

    private ExternalServiceException unavailable(String operation, String reason) {
        return unavailable(operation, reason, null);
    }

    private ExternalServiceException unavailable(String operation, String reason, Throwable cause) {
        log.warn("PokeAPI call failed: {} - {}", operation, reason);
        return new ExternalServiceException(UNAVAILABLE_MESSAGE, cause);
    }
}
