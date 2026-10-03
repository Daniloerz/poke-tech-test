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
        Pokemon pokemon = pokemonWithWeight(weightHectograms);

        assertThat(pokemon.weightKg()).isEqualByComparingTo(new BigDecimal(expectedKg));
    }

    @Test
    void nullListsBecomeEmptyLists() {
        Pokemon pokemon = new Pokemon(1, "bulbasaur", null, null, 69, null);

        assertThat(pokemon.types()).isEmpty();
        assertThat(pokemon.moves()).isEmpty();
    }

    @Test
    void listsAreImmutableCopies() {
        List<String> types = new ArrayList<>(List.of("grass"));
        Pokemon pokemon = new Pokemon(1, "bulbasaur", null, types, 69, List.of());

        types.add("poison");

        assertThat(pokemon.types()).containsExactly("grass");
        assertThatThrownBy(() -> pokemon.types().add("fire")).isInstanceOf(UnsupportedOperationException.class);
    }

    private Pokemon pokemonWithWeight(int weightHectograms) {
        return new Pokemon(1, "bulbasaur", null, List.of(), weightHectograms, List.of());
    }
}
