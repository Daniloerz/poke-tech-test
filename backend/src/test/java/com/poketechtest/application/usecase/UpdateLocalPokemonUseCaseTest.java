package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.LocalPokemonNotFoundException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateLocalPokemonUseCaseTest {

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    @InjectMocks
    private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

    @Test
    void storesTheNormalizedProprietaryFieldsAndKeepsTheSnapshot() {
        LocalPokemon pikachu = LocalPokemon.builder().id(4L).pokeApiId(25).name("pikachu").types(List.of("electric")).weightHectograms(60).build();
        when(localPokemonRepository.findById(4L)).thenReturn(Optional.of(pikachu));
        when(localPokemonRepository.update(any(LocalPokemon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalPokemon updated = updateLocalPokemonUseCase.update(4L, " Pikachu ", "Kanto", List.of("Mascot", "mascot"));

        ArgumentCaptor<LocalPokemon> stored = ArgumentCaptor.forClass(LocalPokemon.class);
        verify(localPokemonRepository).update(stored.capture());
        assertThat(stored.getValue().localizedName()).isEqualTo("Pikachu");
        assertThat(stored.getValue().region()).isEqualTo("Kanto");
        assertThat(stored.getValue().tags()).containsExactly("mascot");
        assertThat(stored.getValue().name()).isEqualTo("pikachu");
        assertThat(stored.getValue().weightHectograms()).isEqualTo(60);
        assertThat(updated).isEqualTo(stored.getValue());
    }

    @Test
    void throwsNotFoundForAnUnknownId() {
        when(localPokemonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateLocalPokemonUseCase.update(99L, null, null, List.of()))
                .isInstanceOf(LocalPokemonNotFoundException.class);
        verify(localPokemonRepository, never()).update(any());
    }
}
