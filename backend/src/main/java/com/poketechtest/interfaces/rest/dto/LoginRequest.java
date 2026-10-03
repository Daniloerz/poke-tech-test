package com.poketechtest.interfaces.rest.dto;

import com.poketechtest.interfaces.rest.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "ash") @NotBlank @Size(max = 30) String username,
        @Schema(example = "pikachu123") @NotBlank @MaxUtf8Bytes(72) String password) {

    // Keeps the password out of logs.
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + "]";
    }
}
