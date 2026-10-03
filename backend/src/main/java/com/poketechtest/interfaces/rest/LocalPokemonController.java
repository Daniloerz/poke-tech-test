package com.poketechtest.interfaces.rest;

import com.poketechtest.application.usecase.GetLocalPokemonUseCase;
import com.poketechtest.application.usecase.SyncPokemonUseCase;
import com.poketechtest.infrastructure.config.OpenApiConfig;
import com.poketechtest.interfaces.rest.dto.LocalPokemonResponse;
import com.poketechtest.interfaces.rest.dto.SyncPokemonRequest;
import com.poketechtest.interfaces.rest.mapper.LocalPokemonRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(LocalPokemonController.LOCAL_POKEMON_PATH)
@RequiredArgsConstructor
@Tag(name = "Local Pokemon", description = "Local, editable copy of PokeAPI Pokemon (authentication required)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class LocalPokemonController {

    static final String LOCAL_POKEMON_PATH = "/api/v1/local-pokemon";

    private final SyncPokemonUseCase syncPokemonUseCase;
    private final GetLocalPokemonUseCase getLocalPokemonUseCase;
    private final LocalPokemonRestMapper localPokemonRestMapper;

    @PostMapping
    @Operation(summary = "Synchronize a Pokemon from PokeAPI", description = "Copies the PokeAPI data into the local database. Proprietary fields start empty.")
    @ApiResponse(responseCode = "201", description = "Local copy created")
    @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Pokemon not found in PokeAPI",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Pokemon already synchronized",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "PokeAPI is not available",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<LocalPokemonResponse> sync(@Valid @RequestBody SyncPokemonRequest request) {
        LocalPokemonResponse synced = localPokemonRestMapper.toResponse(syncPokemonUseCase.sync(request.idOrName()));
        return ResponseEntity.created(URI.create(LOCAL_POKEMON_PATH + "/" + synced.id())).body(synced);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a local Pokemon")
    @ApiResponse(responseCode = "200", description = "Local Pokemon")
    @ApiResponse(responseCode = "400", description = "Non-numeric id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Local Pokemon not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public LocalPokemonResponse get(@Parameter(description = "Local id") @PathVariable long id) {
        return localPokemonRestMapper.toResponse(getLocalPokemonUseCase.get(id));
    }
}
