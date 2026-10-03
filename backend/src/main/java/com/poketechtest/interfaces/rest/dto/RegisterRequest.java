package com.poketechtest.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "brock")
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "must contain only letters, digits, '.', '_' or '-'")
        String username,

        // 72 is the BCrypt input limit: longer passwords would be silently truncated.
        @Schema(example = "onix12345")
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {

    // Keeps the password out of logs.
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + "]";
    }
}
