package com.poketechtest.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalPokemonTest {

    @Test
    void fromCatalogCopiesTheSnapshotWithEmptyProprietaryFields() {
        Pokemon pikachu = Pokemon.builder()
                .id(25)
                .name("pikachu")
                .speciesName("pikachu")
                .spriteUrl("https://sprites.test/25.png")
                .artworkUrl("https://artwork.test/25.png")
                .types(List.of("electric"))
                .heightDecimetres(4)
                .weightHectograms(60)
                .stats(List.of(new PokemonStat("hp", 35)))
                .moves(List.of("thunder-shock"))
                .build();

        LocalPokemon localPokemon = LocalPokemon.fromCatalog(pikachu);

        assertThat(localPokemon.id()).isNull();
        assertThat(localPokemon.pokeApiId()).isEqualTo(25);
        assertThat(localPokemon.name()).isEqualTo("pikachu");
        assertThat(localPokemon.spriteUrl()).isEqualTo("https://sprites.test/25.png");
        assertThat(localPokemon.types()).containsExactly("electric");
        assertThat(localPokemon.heightDecimetres()).isEqualTo(4);
        assertThat(localPokemon.weightHectograms()).isEqualTo(60);
        assertThat(localPokemon.localizedName()).isNull();
        assertThat(localPokemon.region()).isNull();
        assertThat(localPokemon.tags()).isEmpty();
        assertThat(localPokemon.syncedAt()).isNull();
        assertThat(localPokemon.updatedAt()).isNull();
    }

    @Test
    void convertsMeasuresLikeTheCatalog() {
        LocalPokemon localPokemon = LocalPokemon.builder().heightDecimetres(4).weightHectograms(60).build();

        assertThat(localPokemon.heightM()).isEqualByComparingTo(new BigDecimal("0.4"));
        assertThat(localPokemon.weightKg()).isEqualByComparingTo(new BigDecimal("6.0"));
    }

    @Test
    void nullListsBecomeEmptyLists() {
        LocalPokemon localPokemon = LocalPokemon.builder().build();

        assertThat(localPokemon.types()).isEmpty();
        assertThat(localPokemon.tags()).isEmpty();
    }
}
