package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.LocalPokemonNotFoundException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteLocalPokemonUseCaseTest {

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    @InjectMocks
    private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

    @Test
    void deletesAnExistingLocalPokemon() {
        when(localPokemonRepository.findById(4L)).thenReturn(Optional.of(LocalPokemon.builder().id(4L).name("pikachu").build()));

        deleteLocalPokemonUseCase.delete(4L);

        verify(localPokemonRepository).deleteById(4L);
    }

    @Test
    void throwsNotFoundForAnUnknownId() {
        when(localPokemonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteLocalPokemonUseCase.delete(99L)).isInstanceOf(LocalPokemonNotFoundException.class);
        verify(localPokemonRepository, never()).deleteById(anyLong());
    }
}
