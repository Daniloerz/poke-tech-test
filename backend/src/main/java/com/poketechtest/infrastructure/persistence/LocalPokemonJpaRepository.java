package com.poketechtest.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, Long> {

    Optional<LocalPokemonEntity> findByPokeApiId(int pokeApiId);
}
