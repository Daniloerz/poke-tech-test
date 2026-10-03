package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.application.port.out.PokemonCatalogPort;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonPage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListPokemonUseCase {

    private final PokemonCatalogPort pokemonCatalogPort;
    private final ExecutorService virtualThreadExecutor;

    public PokemonPage list(int page, int size) {
        long offset = (long) page * size;
        PokemonCatalogPage catalogPage = pokemonCatalogPort.listPage(offset, size);
        log.debug("Fetching {} Pokemon for page {} (size {})", catalogPage.names().size(), page, size);

        // One call per Pokemon, in parallel; joining in list order keeps the catalog order.
        List<CompletableFuture<Pokemon>> futures = catalogPage.names().stream()
                .map(name -> CompletableFuture.supplyAsync(() -> findListedPokemon(name), virtualThreadExecutor))
                .toList();
        List<Pokemon> items = futures.stream().map(this::join).toList();

        return new PokemonPage(items, page, size, catalogPage.totalCount());
    }

    private Pokemon findListedPokemon(String name) {
        return pokemonCatalogPort.findByIdOrName(name)
                .orElseThrow(() -> new ExternalServiceException("Pokemon listed by the catalog was not found: " + name));
    }

    private Pokemon join(CompletableFuture<Pokemon> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            // Unwraps CompletionException to rethrow the original RuntimeException for clean error handling and testing.
            if (exception.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw exception;
        }
    }
}
