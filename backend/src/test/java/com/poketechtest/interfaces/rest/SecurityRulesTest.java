package com.poketechtest.interfaces.rest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.poketechtest.application.usecase.GetCurrentUserUseCase;
import com.poketechtest.application.usecase.GetPokemonDetailUseCase;
import com.poketechtest.application.usecase.ListPokemonUseCase;
import com.poketechtest.application.usecase.LoginUseCase;
import com.poketechtest.application.usecase.RegisterUserUseCase;
import com.poketechtest.domain.model.PageResult;
import com.poketechtest.domain.model.User;
import com.poketechtest.infrastructure.security.JwtProperties;
import com.poketechtest.infrastructure.security.JwtTokenIssuer;
import com.poketechtest.interfaces.rest.mapper.PokemonRestMapperImpl;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {PokemonController.class, AuthController.class})
@WithSecurityConfig
@Import(PokemonRestMapperImpl.class)
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private ListPokemonUseCase listPokemonUseCase;

    @MockitoBean
    private GetPokemonDetailUseCase getPokemonDetailUseCase;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Test
    void theCatalogIsPublic() throws Exception {
        when(listPokemonUseCase.list(anyInt(), anyInt())).thenReturn(new PageResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/pokemon")).andExpect(status().isOk());
    }

    @Test
    void aProtectedRouteWithoutTokenReturnsUnauthorizedProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.AUTHENTICATION_REQUIRED_DETAIL));
    }

    @Test
    void aMalformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.AUTHENTICATION_REQUIRED_DETAIL));
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() throws Exception {
        JwtProperties otherProperties = new JwtProperties("another-secret-with-at-least-32-characters", Duration.ofHours(1), "poke-tech-test");
        SecretKeySpec otherKey = new SecretKeySpec(otherProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(otherKey).build();
        String foreignToken = new JwtTokenIssuer(otherEncoder, otherProperties, Clock.systemUTC()).issue(new User(1L, "ash", "hash", null)).value();

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenGivesAccessToAProtectedRoute() throws Exception {
        JwtProperties properties = new JwtProperties(WithSecurityConfig.TEST_JWT_SECRET, Duration.ofHours(1), "poke-tech-test");
        User ash = new User(1L, "ash", "hash", null);
        String token = new JwtTokenIssuer(jwtEncoder, properties, Clock.systemUTC()).issue(ash).value();
        when(getCurrentUserUseCase.get("ash")).thenReturn(ash);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ash"));
    }

    @Test
    void anyOtherRouteIsProtectedByDefault() throws Exception {
        mockMvc.perform(delete("/api/v1/pokemon/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/some-future-route")).andExpect(status().isUnauthorized());
    }
}
