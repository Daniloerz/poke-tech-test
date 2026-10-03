package com.poketechtest.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    @ParameterizedTest
    @ValueSource(strings = {"ash", "Ash", "ASH", "  ash  "})
    void normalizeUsernameTrimsAndLowerCases(String username) {
        assertThat(User.normalizeUsername(username)).isEqualTo("ash");
    }

    @Test
    void newUserHasNormalizedUsernameAndNoIdYet() {
        User user = User.newUser("Misty", "hash");

        assertThat(user.id()).isNull();
        assertThat(user.username()).isEqualTo("misty");
        assertThat(user.passwordHash()).isEqualTo("hash");
        assertThat(user.createdAt()).isNull();
    }

    @Test
    void toStringDoesNotExposeThePasswordHash() {
        User user = new User(1L, "ash", "$2a$10$secret-hash", null);

        assertThat(user.toString()).contains("ash").doesNotContain("secret-hash");
    }
}
