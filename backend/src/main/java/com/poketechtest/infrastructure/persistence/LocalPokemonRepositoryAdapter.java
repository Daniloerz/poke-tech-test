package com.poketechtest.infrastructure.persistence;

import com.poketechtest.application.exception.PokemonAlreadySyncedException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalPokemonRepositoryAdapter implements LocalPokemonRepository {

    static final String POKE_API_ID_UNIQUE_CONSTRAINT = "uk_local_pokemon_poke_api_id";

    private final LocalPokemonJpaRepository localPokemonJpaRepository;
    private final LocalPokemonEntityMapper localPokemonEntityMapper;

    @Override
    public Optional<LocalPokemon> findById(long id) {
        return localPokemonJpaRepository.findById(id).map(localPokemonEntityMapper::toDomain);
    }

    @Override
    public Optional<LocalPokemon> findByPokeApiId(int pokeApiId) {
        return localPokemonJpaRepository.findByPokeApiId(pokeApiId).map(localPokemonEntityMapper::toDomain);
    }

    @Override
    public LocalPokemon insert(LocalPokemon localPokemon) {
        try {
            LocalPokemonEntity saved = localPokemonJpaRepository.saveAndFlush(localPokemonEntityMapper.toEntity(localPokemon));
            return localPokemonEntityMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            // Two synchronizations of the same Pokemon at the same time: the unique constraint wins (US03 TDR-002).
            if (violates(exception, POKE_API_ID_UNIQUE_CONSTRAINT)) {
                throw new PokemonAlreadySyncedException(localPokemon.name(), null);
            }
            throw exception;
        }
    }

    private boolean violates(DataIntegrityViolationException exception, String constraintName) {
        return exception.getCause() instanceof ConstraintViolationException violation
                && constraintName.equalsIgnoreCase(violation.getConstraintName());
    }
}
