package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.PageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListLocalPokemonUseCaseTest {

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    @InjectMocks
    private ListLocalPokemonUseCase listLocalPokemonUseCase;

    @Test
    void returnsTheRequestedPage() {
        PageResult<LocalPokemon> page = new PageResult<>(List.of(LocalPokemon.builder().id(3L).name("squirtle").build()), 1, 2, 3);
        when(localPokemonRepository.findPage(1, 2)).thenReturn(page);

        assertThat(listLocalPokemonUseCase.list(1, 2)).isEqualTo(page);
    }
}
