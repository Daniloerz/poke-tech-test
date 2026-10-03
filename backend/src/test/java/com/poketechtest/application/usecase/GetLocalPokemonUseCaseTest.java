package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class GetLocalPokemonUseCaseTest {

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    @InjectMocks
    private GetLocalPokemonUseCase getLocalPokemonUseCase;

    @Test
    void returnsTheLocalPokemon() {
        LocalPokemon bulbasaur = LocalPokemon.builder().id(1L).pokeApiId(1).name("bulbasaur").build();
        when(localPokemonRepository.findById(1L)).thenReturn(Optional.of(bulbasaur));

        assertThat(getLocalPokemonUseCase.get(1L)).isEqualTo(bulbasaur);
    }

    @Test
    void throwsNotFoundForAnUnknownId() {
        when(localPokemonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getLocalPokemonUseCase.get(99L))
                .isInstanceOf(LocalPokemonNotFoundException.class)
                .hasMessage("Local Pokemon not found: 99");
    }
}
