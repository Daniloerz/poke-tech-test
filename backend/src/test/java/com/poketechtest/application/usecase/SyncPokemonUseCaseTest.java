package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.exception.PokemonAlreadySyncedException;
import com.poketechtest.application.exception.PokemonNotFoundException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.application.port.out.PokemonCatalogPort;
import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.Pokemon;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncPokemonUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-03T23:00:00Z");

    @Mock
    private PokemonCatalogPort pokemonCatalogPort;

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    private SyncPokemonUseCase syncPokemonUseCase;

    @BeforeEach
    void setUp() {
        syncPokemonUseCase = new SyncPokemonUseCase(pokemonCatalogPort, localPokemonRepository);
    }

    @Test
    void storesTheCatalogSnapshotWithEmptyProprietaryFields() {
        when(pokemonCatalogPort.findByIdOrName("pikachu")).thenReturn(Optional.of(pikachu()));
        when(localPokemonRepository.findByPokeApiId(25)).thenReturn(Optional.empty());
        when(localPokemonRepository.insert(any(LocalPokemon.class))).thenAnswer(invocation -> withIdAndTimes(invocation.getArgument(0), 4L));

        LocalPokemon synced = syncPokemonUseCase.sync("pikachu");

        ArgumentCaptor<LocalPokemon> inserted = ArgumentCaptor.forClass(LocalPokemon.class);
        verify(localPokemonRepository).insert(inserted.capture());
        assertThat(inserted.getValue().pokeApiId()).isEqualTo(25);
        assertThat(inserted.getValue().name()).isEqualTo("pikachu");
        assertThat(inserted.getValue().types()).containsExactly("electric");
        assertThat(inserted.getValue().localizedName()).isNull();
        assertThat(inserted.getValue().tags()).isEmpty();
        assertThat(synced.id()).isEqualTo(4L);
        assertThat(synced.syncedAt()).isEqualTo(NOW);
    }

    @Test
    void normalizesTheIdentifier() {
        when(pokemonCatalogPort.findByIdOrName("pikachu")).thenReturn(Optional.of(pikachu()));
        when(localPokemonRepository.findByPokeApiId(25)).thenReturn(Optional.empty());
        when(localPokemonRepository.insert(any(LocalPokemon.class))).thenAnswer(invocation -> withIdAndTimes(invocation.getArgument(0), 4L));

        assertThat(syncPokemonUseCase.sync("  PikaChu ").name()).isEqualTo("pikachu");
    }

    @Test
    void rejectsAPokemonThatIsAlreadyLocalAlsoWhenRequestedById() {
        when(pokemonCatalogPort.findByIdOrName("25")).thenReturn(Optional.of(pikachu()));
        when(localPokemonRepository.findByPokeApiId(25)).thenReturn(Optional.of(withIdAndTimes(LocalPokemon.fromCatalog(pikachu()), 4L)));

        assertThatThrownBy(() -> syncPokemonUseCase.sync("25"))
                .isInstanceOf(PokemonAlreadySyncedException.class)
                .hasMessage("Pokemon already synchronized: pikachu (local id 4)");
        verify(localPokemonRepository, never()).insert(any());
    }

    @Test
    void propagatesTheConflictDetectedOnInsert() {
        when(pokemonCatalogPort.findByIdOrName("pikachu")).thenReturn(Optional.of(pikachu()));
        when(localPokemonRepository.findByPokeApiId(25)).thenReturn(Optional.empty());
        when(localPokemonRepository.insert(any(LocalPokemon.class))).thenThrow(new PokemonAlreadySyncedException("pikachu", null));

        assertThatThrownBy(() -> syncPokemonUseCase.sync("pikachu")).isInstanceOf(PokemonAlreadySyncedException.class);
    }

    @Test
    void throwsNotFoundWhenPokeApiDoesNotHaveThePokemon() {
        when(pokemonCatalogPort.findByIdOrName("missingno")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syncPokemonUseCase.sync("missingno")).isInstanceOf(PokemonNotFoundException.class);
        verifyNoInteractions(localPokemonRepository);
    }

    @Test
    void storesNothingWhenPokeApiIsNotAvailable() {
        when(pokemonCatalogPort.findByIdOrName("pikachu")).thenThrow(new ExternalServiceException("PokeAPI is not available"));

        assertThatThrownBy(() -> syncPokemonUseCase.sync("pikachu")).isInstanceOf(ExternalServiceException.class);
        verify(localPokemonRepository, never()).findByPokeApiId(anyInt());
        verify(localPokemonRepository, never()).insert(any());
    }

    private Pokemon pikachu() {
        return Pokemon.builder()
                .id(25)
                .name("pikachu")
                .speciesName("pikachu")
                .spriteUrl("https://sprites.test/25.png")
                .types(List.of("electric"))
                .heightDecimetres(4)
                .weightHectograms(60)
                .build();
    }

    private LocalPokemon withIdAndTimes(LocalPokemon localPokemon, Long id) {
        return LocalPokemon.builder()
                .id(id)
                .pokeApiId(localPokemon.pokeApiId())
                .name(localPokemon.name())
                .spriteUrl(localPokemon.spriteUrl())
                .types(localPokemon.types())
                .heightDecimetres(localPokemon.heightDecimetres())
                .weightHectograms(localPokemon.weightHectograms())
                .syncedAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
