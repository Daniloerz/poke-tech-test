package com.poketechtest.interfaces.rest;

import com.poketechtest.application.usecase.GetCurrentUserUseCase;
import com.poketechtest.application.usecase.LoginUseCase;
import com.poketechtest.application.usecase.RegisterUserUseCase;
import com.poketechtest.infrastructure.config.OpenApiConfig;
import com.poketechtest.interfaces.rest.dto.LoginRequest;
import com.poketechtest.interfaces.rest.dto.RegisterRequest;
import com.poketechtest.interfaces.rest.dto.TokenResponse;
import com.poketechtest.interfaces.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login and current user")
public class AuthController {

    private static final URI CURRENT_USER_LOCATION = URI.create("/api/v1/auth/me");

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid username or password",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Username already exists",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = UserResponse.from(registerUserUseCase.register(request.username(), request.password()));
        return ResponseEntity.created(CURRENT_USER_LOCATION).body(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and get a JWT", description = "Demo users: ash / pikachu123, misty / starmie123.")
    @ApiResponse(responseCode = "200", description = "Bearer token")
    @ApiResponse(responseCode = "400", description = "Missing username or password",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Invalid username or password",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(loginUseCase.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponse(responseCode = "200", description = "Authenticated user")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(getCurrentUserUseCase.get(jwt.getSubject()));
    }
}
