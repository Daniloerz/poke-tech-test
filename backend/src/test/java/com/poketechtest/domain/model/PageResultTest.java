package com.poketechtest.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageResultTest {

    @ParameterizedTest
    @CsvSource({"1351, 20, 68", "40, 20, 2", "41, 20, 3", "0, 20, 0", "1, 50, 1"})
    void totalPagesRoundsUp(long totalElements, int size, int expectedTotalPages) {
        PageResult<Pokemon> page = new PageResult<>(List.of(), 0, size, totalElements);

        assertThat(page.totalPages()).isEqualTo(expectedTotalPages);
    }

    @Test
    void pageAfterTheLastOneKeepsTheRealTotal() {
        PageResult<Pokemon> page = new PageResult<>(List.of(), 100, 20, 1351);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(1351);
    }

    @Test
    void rejectsInvalidPageOrSize() {
        assertThatThrownBy(() -> new PageResult<Pokemon>(List.of(), -1, 20, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResult<Pokemon>(List.of(), 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
