package com.poketechtest.domain.model;

import java.time.Instant;
import java.util.Locale;

public record User(Long id, String username, String passwordHash, Instant createdAt) {

    public static User newUser(String username, String passwordHash) {
        return new User(null, normalizeUsername(username), passwordHash, null);
    }

    /** Usernames are case-insensitive: they are always stored and compared trimmed and in lower case. */
    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    // Keeps the hash out of logs and error messages.
    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", createdAt=" + createdAt + "]";
    }
}
