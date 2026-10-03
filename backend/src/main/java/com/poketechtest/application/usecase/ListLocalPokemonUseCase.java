package com.poketechtest.application.usecase;

import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListLocalPokemonUseCase {

    private final LocalPokemonRepository localPokemonRepository;

    public PageResult<LocalPokemon> list(int page, int size) {
        return localPokemonRepository.findPage(page, size);
    }
}
