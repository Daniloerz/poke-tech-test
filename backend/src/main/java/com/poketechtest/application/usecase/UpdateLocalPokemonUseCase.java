package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.LocalPokemonNotFoundException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateLocalPokemonUseCase {

    private final LocalPokemonRepository localPokemonRepository;

    @Transactional
    public LocalPokemon update(long id, String localizedName, String region, List<String> tags) {
        LocalPokemon current = localPokemonRepository.findById(id)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));

        LocalPokemon updated = localPokemonRepository.update(current.withProprietaryFields(localizedName, region, tags));
        log.info("Local Pokemon updated: localId={}, name={}", updated.id(), updated.name());
        return updated;
    }
}
