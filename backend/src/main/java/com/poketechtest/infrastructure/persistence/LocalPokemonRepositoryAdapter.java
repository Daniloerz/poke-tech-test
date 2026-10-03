package com.poketechtest.infrastructure.persistence;

import com.poketechtest.application.exception.LocalPokemonNotFoundException;
import com.poketechtest.application.exception.PokemonAlreadySyncedException;
import com.poketechtest.application.port.out.LocalPokemonRepository;
import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.PageResult;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalPokemonRepositoryAdapter implements LocalPokemonRepository {

    static final String POKE_API_ID_UNIQUE_CONSTRAINT = "uk_local_pokemon_poke_api_id";
    private static final String ID_PROPERTY = "id";

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
    public PageResult<LocalPokemon> findPage(int page, int size) {
        Page<LocalPokemonEntity> entities = localPokemonJpaRepository.findAll(PageRequest.of(page, size, Sort.by(ID_PROPERTY)));
        List<LocalPokemon> items = entities.getContent().stream().map(localPokemonEntityMapper::toDomain).toList();
        return new PageResult<>(items, page, size, entities.getTotalElements());
    }

    @Override
    public LocalPokemon update(LocalPokemon localPokemon) {
        try {
            // Merge of the full entity; synced_at is not updatable and updated_at is refreshed by Hibernate.
            LocalPokemonEntity saved = localPokemonJpaRepository.saveAndFlush(localPokemonEntityMapper.toEntity(localPokemon));
            return localPokemonEntityMapper.toDomain(saved);
        } catch (ObjectOptimisticLockingFailureException exception) {
            // The row was deleted by another request after it was read: for this client it no longer exists.
            throw new LocalPokemonNotFoundException(localPokemon.id());
        }
    }

    @Override
    public void deleteById(long id) {
        localPokemonJpaRepository.deleteById(id);
    }

    @Override
    public LocalPokemon insert(LocalPokemon localPokemon) {
        try {
            LocalPokemonEntity saved = localPokemonJpaRepository.saveAndFlush(localPokemonEntityMapper.toEntity(localPokemon));
            return localPokemonEntityMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            // Two synchronizations of the same Pokemon at the same time: the unique constraint wins (US03 TDR-002).
            if (ConstraintViolations.isViolationOf(exception, POKE_API_ID_UNIQUE_CONSTRAINT)) {
                throw new PokemonAlreadySyncedException(localPokemon.name(), null);
            }
            throw exception;
        }
    }
}
