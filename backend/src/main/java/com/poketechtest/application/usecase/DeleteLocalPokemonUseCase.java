package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.LocalPokemonNotFoundException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteLocalPokemonUseCase {

    private final LocalPokemonRepository localPokemonRepository;

    @Transactional
    public void delete(long id) {
        LocalPokemon existing = localPokemonRepository.findById(id)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));

        localPokemonRepository.deleteById(id);
        log.info("Local Pokemon deleted: localId={}, name={}", id, existing.name());
    }
}
