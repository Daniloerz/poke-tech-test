package com.poketechtest.interfaces.rest;

import com.poketechtest.application.usecase.GetPokemonDetailUseCase;
import com.poketechtest.application.usecase.ListPokemonUseCase;
import com.poketechtest.interfaces.rest.dto.PageResponse;
import com.poketechtest.interfaces.rest.dto.PokemonDetailResponse;
import com.poketechtest.interfaces.rest.dto.PokemonSummaryResponse;
import com.poketechtest.interfaces.rest.mapper.PokemonRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pokemon")
@RequiredArgsConstructor
@Tag(name = "Pokemon catalog", description = "Pokemon read from PokeAPI")
public class PokemonController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String ID_OR_NAME_PATTERN = "^[A-Za-z0-9-]{1,50}$";
    private static final String ID_OR_NAME_MESSAGE = "must have 1 to 50 letters, digits or hyphens";

    private final ListPokemonUseCase listPokemonUseCase;
    private final GetPokemonDetailUseCase getPokemonDetailUseCase;
    private final PokemonRestMapper pokemonRestMapper;

    @GetMapping
    @Operation(summary = "List Pokemon by page", description = "Returns sprite, types, weight and moves of each Pokemon, in PokeAPI order.")
    @ApiResponse(responseCode = "200", description = "Page of Pokemon")
    @ApiResponse(responseCode = "400", description = "Invalid page or size",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "PokeAPI is not available",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<PokemonSummaryResponse> list(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 to " + MAX_PAGE_SIZE) @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return pokemonRestMapper.toPageResponse(listPokemonUseCase.list(page, size));
    }

    @GetMapping("/{idOrName}")
    @Operation(summary = "Get the detail of a Pokemon", description = "Returns image, stats, description and evolution chain. Accepts the PokeAPI id or name (case-insensitive).")
    @ApiResponse(responseCode = "200", description = "Pokemon detail")
    @ApiResponse(responseCode = "400", description = "Invalid identifier",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Pokemon not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "PokeAPI is not available",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PokemonDetailResponse getDetail(
            @Parameter(description = "PokeAPI id or name, for example 133 or eevee", example = "eevee")
            @PathVariable @Pattern(regexp = ID_OR_NAME_PATTERN, message = ID_OR_NAME_MESSAGE) String idOrName) {
        return pokemonRestMapper.toDetailResponse(getPokemonDetailUseCase.get(idOrName));
    }
}
