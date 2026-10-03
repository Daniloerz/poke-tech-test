package com.poketechtest.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PokemonTest {

    @ParameterizedTest
    @CsvSource({"69, 6.9", "0, 0.0", "1000, 100.0", "1, 0.1"})
    void weightKgConvertsHectogramsToKilograms(int weightHectograms, String expectedKg) {
        Pokemon pokemon = Pokemon.builder().weightHectograms(weightHectograms).build();

        assertThat(pokemon.weightKg()).isEqualByComparingTo(new BigDecimal(expectedKg));
    }

    @ParameterizedTest
    @CsvSource({"7, 0.7", "3, 0.3", "145, 14.5"})
    void heightMConvertsDecimetresToMetres(int heightDecimetres, String expectedMetres) {
        Pokemon pokemon = Pokemon.builder().heightDecimetres(heightDecimetres).build();

        assertThat(pokemon.heightM()).isEqualByComparingTo(new BigDecimal(expectedMetres));
    }

    @Test
    void imageUrlPrefersTheArtwork() {
        Pokemon pokemon = Pokemon.builder().spriteUrl("sprite.png").artworkUrl("artwork.png").build();

        assertThat(pokemon.imageUrl()).isEqualTo("artwork.png");
    }

    @Test
    void imageUrlFallsBackToTheSprite() {
        Pokemon pokemon = Pokemon.builder().spriteUrl("sprite.png").build();

        assertThat(pokemon.imageUrl()).isEqualTo("sprite.png");
    }

    @Test
    void imageUrlIsNullWithoutArtworkAndSprite() {
        assertThat(Pokemon.builder().build().imageUrl()).isNull();
    }

    @Test
    void nullListsBecomeEmptyLists() {
        Pokemon pokemon = Pokemon.builder().build();

        assertThat(pokemon.types()).isEmpty();
        assertThat(pokemon.stats()).isEmpty();
        assertThat(pokemon.moves()).isEmpty();
    }

    @Test
    void listsAreImmutableCopies() {
        List<String> types = new ArrayList<>(List.of("grass"));
        Pokemon pokemon = Pokemon.builder().types(types).build();

        types.add("poison");

        assertThat(pokemon.types()).containsExactly("grass");
        assertThatThrownBy(() -> pokemon.types().add("fire")).isInstanceOf(UnsupportedOperationException.class);
    }
}
