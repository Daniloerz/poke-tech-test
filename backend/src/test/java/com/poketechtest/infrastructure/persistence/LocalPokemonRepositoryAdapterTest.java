package com.poketechtest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.PokemonAlreadySyncedException;
import com.poketechtest.domain.model.LocalPokemon;
import com.poketechtest.domain.model.PageResult;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class LocalPokemonRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-03T23:00:00Z");

    @Mock
    private LocalPokemonJpaRepository localPokemonJpaRepository;

    private LocalPokemonRepositoryAdapter localPokemonRepositoryAdapter;

    @BeforeEach
    void setUp() {
        localPokemonRepositoryAdapter = new LocalPokemonRepositoryAdapter(localPokemonJpaRepository, Mappers.getMapper(LocalPokemonEntityMapper.class));
    }

    @Test
    void findByPokeApiIdMapsAllFieldsToTheDomain() {
        when(localPokemonJpaRepository.findByPokeApiId(1)).thenReturn(Optional.of(bulbasaurEntity()));

        Optional<LocalPokemon> found = localPokemonRepositoryAdapter.findByPokeApiId(1);

        assertThat(found).hasValueSatisfying(bulbasaur -> {
            assertThat(bulbasaur.id()).isEqualTo(1L);
            assertThat(bulbasaur.types()).containsExactly("grass", "poison");
            assertThat(bulbasaur.localizedName()).isEqualTo("Fushigidane");
            assertThat(bulbasaur.region()).isEqualTo("Kanto");
            assertThat(bulbasaur.tags()).containsExactly("starter", "gen-1");
            assertThat(bulbasaur.syncedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void insertMapsTheDomainToTheEntityAndBack() {
        when(localPokemonJpaRepository.saveAndFlush(any(LocalPokemonEntity.class))).thenAnswer(invocation -> {
            LocalPokemonEntity entity = invocation.getArgument(0);
            entity.setId(4L);
            entity.setSyncedAt(NOW);
            entity.setUpdatedAt(NOW);
            return entity;
        });
        LocalPokemon pikachu = LocalPokemon.builder().pokeApiId(25).name("pikachu").types(List.of("electric")).heightDecimetres(4).weightHectograms(60).build();

        LocalPokemon inserted = localPokemonRepositoryAdapter.insert(pikachu);

        assertThat(inserted.id()).isEqualTo(4L);
        assertThat(inserted.pokeApiId()).isEqualTo(25);
        assertThat(inserted.types()).containsExactly("electric");
        assertThat(inserted.tags()).isEmpty();
        assertThat(inserted.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void findPageRequestsThePageOrderedByIdAndMapsIt() {
        PageRequest expectedRequest = PageRequest.of(1, 2, Sort.by("id"));
        when(localPokemonJpaRepository.findAll(expectedRequest)).thenReturn(new PageImpl<>(List.of(bulbasaurEntity()), expectedRequest, 3));

        PageResult<LocalPokemon> page = localPokemonRepositoryAdapter.findPage(1, 2);

        assertThat(page.items()).extracting(LocalPokemon::name).containsExactly("bulbasaur");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void updateSavesTheProprietaryFieldsAndReturnsTheStoredRecord() {
        when(localPokemonJpaRepository.saveAndFlush(any(LocalPokemonEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalPokemon changed = LocalPokemon.builder().id(1L).pokeApiId(1).name("bulbasaur").localizedName("Bisasam").tags(List.of("grass-starter")).build();

        LocalPokemon updated = localPokemonRepositoryAdapter.update(changed);

        assertThat(updated.id()).isEqualTo(1L);
        assertThat(updated.localizedName()).isEqualTo("Bisasam");
        assertThat(updated.tags()).containsExactly("grass-starter");
    }

    @Test
    void deleteByIdDelegatesToSpringData() {
        localPokemonRepositoryAdapter.deleteById(4L);

        verify(localPokemonJpaRepository).deleteById(4L);
    }

    @Test
    void insertTranslatesTheUniquePokeApiIdViolation() {
        when(localPokemonJpaRepository.saveAndFlush(any(LocalPokemonEntity.class)))
                .thenThrow(constraintViolation(LocalPokemonRepositoryAdapter.POKE_API_ID_UNIQUE_CONSTRAINT));

        assertThatThrownBy(() -> localPokemonRepositoryAdapter.insert(LocalPokemon.builder().pokeApiId(25).name("pikachu").build()))
                .isInstanceOf(PokemonAlreadySyncedException.class)
                .hasMessage("Pokemon already synchronized: pikachu");
    }

    @Test
    void insertRethrowsOtherConstraintViolations() {
        DataIntegrityViolationException otherViolation = constraintViolation("ck_local_pokemon_measures_not_negative");
        when(localPokemonJpaRepository.saveAndFlush(any(LocalPokemonEntity.class))).thenThrow(otherViolation);

        assertThatThrownBy(() -> localPokemonRepositoryAdapter.insert(LocalPokemon.builder().pokeApiId(25).name("pikachu").build()))
                .isSameAs(otherViolation);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException("violation", new SQLException("violation"), constraintName);
        return new DataIntegrityViolationException("violation", cause);
    }

    private LocalPokemonEntity bulbasaurEntity() {
        LocalPokemonEntity entity = new LocalPokemonEntity();
        entity.setId(1L);
        entity.setPokeApiId(1);
        entity.setName("bulbasaur");
        entity.setTypes(List.of("grass", "poison"));
        entity.setHeightDecimetres(7);
        entity.setWeightHectograms(69);
        entity.setLocalizedName("Fushigidane");
        entity.setRegion("Kanto");
        entity.setTags(List.of("starter", "gen-1"));
        entity.setSyncedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }
}
