package com.poketechtest.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Replaces the proprietary fields. The PokeAPI snapshot is not editable: any other field is rejected (US04 TDR-002). */
public record UpdateLocalPokemonRequest(
        @Schema(description = "Localized name; null clears it", example = "Pikachu", nullable = true)
        @Size(max = 100)
        @Pattern(regexp = NOT_BLANK, message = "must not be blank")
        String localizedName,

        @Schema(description = "Geographic metadata; null clears it", example = "Kanto", nullable = true)
        @Size(max = 100)
        @Pattern(regexp = NOT_BLANK, message = "must not be blank")
        String region,

        @Schema(description = "Internal tags (case-insensitive); [] clears them", example = "[\"electric-mouse\", \"mascot\"]")
        @NotNull
        @Size(max = 10)
        List<@NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{1,30}$", message = "must have 1 to 30 letters, digits or hyphens") String> tags) {

    // Null passes (the field is optional); any value must contain a non-space character.
    private static final String NOT_BLANK = "(?s).*\\S.*";
}
