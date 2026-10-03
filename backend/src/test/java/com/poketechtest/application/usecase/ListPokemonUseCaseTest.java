package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.application.port.out.PokemonCatalogPort;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonPage;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListPokemonUseCaseTest {

    private static final long TOTAL_COUNT = 1351;

    @Mock
    private PokemonCatalogPort pokemonCatalogPort;

    private ExecutorService executor;
    private ListPokemonUseCase listPokemonUseCase;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        listPokemonUseCase = new ListPokemonUseCase(pokemonCatalogPort, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void listsThePokemonOfTheRequestedPage() {
        when(pokemonCatalogPort.listPage(40, 2)).thenReturn(new PokemonCatalogPage(List.of("pidgey", "rattata"), TOTAL_COUNT));
        when(pokemonCatalogPort.findByName("pidgey")).thenReturn(Optional.of(pokemon(16, "pidgey")));
        when(pokemonCatalogPort.findByName("rattata")).thenReturn(Optional.of(pokemon(19, "rattata")));

        PokemonPage result = listPokemonUseCase.list(20, 2);

        assertThat(result.items()).extracting(Pokemon::name).containsExactly("pidgey", "rattata");
        assertThat(result.page()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(TOTAL_COUNT);
    }

    @Test
    void keepsTheCatalogOrderWhenCallsFinishInAnotherOrder() {
        when(pokemonCatalogPort.listPage(0, 3)).thenReturn(new PokemonCatalogPage(List.of("bulbasaur", "ivysaur", "venusaur"), TOTAL_COUNT));
        when(pokemonCatalogPort.findByName("bulbasaur")).thenAnswer(invocation -> {
            Thread.sleep(200);
            return Optional.of(pokemon(1, "bulbasaur"));
        });
        when(pokemonCatalogPort.findByName("ivysaur")).thenReturn(Optional.of(pokemon(2, "ivysaur")));
        when(pokemonCatalogPort.findByName("venusaur")).thenReturn(Optional.of(pokemon(3, "venusaur")));

        PokemonPage result = listPokemonUseCase.list(0, 3);

        assertThat(result.items()).extracting(Pokemon::id).containsExactly(1, 2, 3);
    }

    @Test
    void returnsAnEmptyPageAfterTheLastOne() {
        when(pokemonCatalogPort.listPage(100_000, 20)).thenReturn(new PokemonCatalogPage(List.of(), TOTAL_COUNT));

        PokemonPage result = listPokemonUseCase.list(5_000, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(TOTAL_COUNT);
        verify(pokemonCatalogPort, never()).findByName(anyString());
    }

    @Test
    void computesTheOffsetWithoutIntegerOverflow() {
        long expectedOffset = (long) Integer.MAX_VALUE * 50;
        when(pokemonCatalogPort.listPage(expectedOffset, 50)).thenReturn(new PokemonCatalogPage(List.of(), TOTAL_COUNT));

        PokemonPage result = listPokemonUseCase.list(Integer.MAX_VALUE, 50);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void propagatesTheErrorWhenOnePokemonCallFails() {
        when(pokemonCatalogPort.listPage(0, 2)).thenReturn(new PokemonCatalogPage(List.of("bulbasaur", "ivysaur"), TOTAL_COUNT));
        when(pokemonCatalogPort.findByName("bulbasaur")).thenReturn(Optional.of(pokemon(1, "bulbasaur")));
        when(pokemonCatalogPort.findByName("ivysaur")).thenThrow(new ExternalServiceException("PokeAPI is down"));

        assertThatThrownBy(() -> listPokemonUseCase.list(0, 2))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("PokeAPI is down");
    }

    @Test
    void failsWhenAListedPokemonIsNotFoundInTheCatalog() {
        when(pokemonCatalogPort.listPage(0, 1)).thenReturn(new PokemonCatalogPage(List.of("missingno"), TOTAL_COUNT));
        when(pokemonCatalogPort.findByName("missingno")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listPokemonUseCase.list(0, 1))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("missingno");
    }

    @Test
    void propagatesTheErrorWhenTheListCallFails() {
        when(pokemonCatalogPort.listPage(0, 20)).thenThrow(new ExternalServiceException("PokeAPI is down"));

        assertThatThrownBy(() -> listPokemonUseCase.list(0, 20)).isInstanceOf(ExternalServiceException.class);
    }

    private Pokemon pokemon(int id, String name) {
        return new Pokemon(id, name, null, List.of("normal"), 10, List.of("tackle"));
    }
}
