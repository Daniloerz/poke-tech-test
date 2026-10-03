package com.poketechtest.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher passwordHasher = new BcryptPasswordHasher(new SecurityConfig().passwordEncoder());

    @Test
    void hashIsSaltedAndNeverThePlainPassword() {
        String firstHash = passwordHasher.hash("pikachu123");
        String secondHash = passwordHasher.hash("pikachu123");

        assertThat(firstHash).isNotEqualTo("pikachu123").startsWith("$2a$10$");
        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @Test
    void matchesOnlyTheRightPassword() {
        String hash = passwordHasher.hash("pikachu123");

        assertThat(passwordHasher.matches("pikachu123", hash)).isTrue();
        assertThat(passwordHasher.matches("pikachu124", hash)).isFalse();
    }

    @Test
    void matchesTheSeedHashOfTheDemoUser() {
        // Same hash as the Liquibase seed changeset for ash / pikachu123.
        String seedHash = "$2a$10$7sltUls.nWf047ebQYCe4.x7lrLmYqCI3.a0Zjt9VQAKi0az9tKY6";

        assertThat(passwordHasher.matches("pikachu123", seedHash)).isTrue();
    }
}
