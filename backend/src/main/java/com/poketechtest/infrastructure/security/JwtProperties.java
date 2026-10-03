package com.poketechtest.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        // HS256 needs a key of at least 256 bits (32 bytes).
        @NotBlank @Size(min = 32, message = "must have at least 32 characters (256 bits for HS256)") String secret,
        @NotNull Duration expiration,
        @NotBlank String issuer) {

    // Keeps the secret out of logs and startup error messages.
    @Override
    public String toString() {
        return "JwtProperties[expiration=" + expiration + ", issuer=" + issuer + "]";
    }
}
