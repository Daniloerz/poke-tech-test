package com.poketechtest.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SyncPokemonRequest(
        @Schema(description = "PokeAPI id or name (case-insensitive)", example = "pikachu")
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9-]{1,50}$", message = "must have 1 to 50 letters, digits or hyphens")
        String idOrName) {
}
