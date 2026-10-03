package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.LocalPokemonNotFoundException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetLocalPokemonUseCase {

    private final LocalPokemonRepository localPokemonRepository;

    public LocalPokemon get(long id) {
        return localPokemonRepository.findById(id)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));
    }
}
