package com.poketechtest.interfaces.rest.dto;

import com.poketechtest.interfaces.rest.validation.MaxUtf8Bytes;
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

        // BCrypt accepts at most 72 bytes; the limit is checked in bytes because "ñ" takes two.
        @Schema(example = "onix12345")
        @NotBlank
        @Size(min = 8)
        @MaxUtf8Bytes(72)
        String password) {

    // Keeps the password out of logs.
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + "]";
    }
}
