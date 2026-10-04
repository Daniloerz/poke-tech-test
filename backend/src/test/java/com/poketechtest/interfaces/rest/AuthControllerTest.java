package com.poketechtest.interfaces.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.poketechtest.application.exception.InvalidCredentialsException;
import com.poketechtest.application.exception.UsernameAlreadyExistsException;
import com.poketechtest.application.port.out.IssuedToken;
import com.poketechtest.application.usecase.GetCurrentUserUseCase;
import com.poketechtest.application.usecase.LoginUseCase;
import com.poketechtest.application.usecase.RegisterUserUseCase;
import com.poketechtest.domain.model.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AuthController.class)
@WithSecurityConfig
class AuthControllerTest {

    private static final String AUTH_PATH = "/api/v1/auth";
    private static final Instant CREATED_AT = Instant.parse("2026-10-03T20:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Test
    void registerReturnsCreatedWithoutThePassword() throws Exception {
        when(registerUserUseCase.register("Brock", "onix12345")).thenReturn(new User(3L, "brock", "hash", CREATED_AT));

        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "Brock", "password": "onix12345"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/auth/me"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("brock"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-03T20:00:00Z"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a-username-that-is-longer-than-30", "ash ketchum", "ash@kanto"})
    void registerRejectsAnInvalidUsername(String username) throws Exception {
        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "%s", "password": "pikachu123"}""".formatted(username)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("username")));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void validationMessagesAreInEnglishWhateverTheBrowserLanguage() throws Exception {
        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "ab", "password": "short"}""").header("Accept-Language", "es-ES,es;q=0.9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'username')].message").value("size must be between 3 and 30"))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message").value("must have at least 8 characters"));
    }

    @Test
    void registerRejectsAPasswordOutsideTheAllowedLength() throws Exception {
        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "brock", "password": "short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")));

        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "brock", "password": "%s"}""".formatted("a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")));
    }

    @Test
    void registerRejectsAPasswordLongerThan72BytesEvenWith72OrFewerCharacters() throws Exception {
        String fortyMultiByteCharacters = "ñ".repeat(40);

        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "brock", "password": "%s"}""".formatted(fortyMultiByteCharacters)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be longer than 72 bytes in UTF-8"));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void registerRejectsMissingFieldsAndMalformedJson() throws Exception {
        mockMvc.perform(json(post(AUTH_PATH + "/register"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("username")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")));

        mockMvc.perform(json(post(AUTH_PATH + "/register"), "{\"username\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.MALFORMED_BODY_DETAIL));
    }

    @Test
    void registerRejectsUnknownFields() throws Exception {
        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "brock", "password": "onix12345", "role": "admin"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"))
                .andExpect(jsonPath("$.errors[0].message").value("is not a recognized field"));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void registerReturnsConflictWhenTheUsernameExists() throws Exception {
        when(registerUserUseCase.register("ash", "pikachu123")).thenThrow(new UsernameAlreadyExistsException("ash"));

        mockMvc.perform(json(post(AUTH_PATH + "/register"), """
                        {"username": "ash", "password": "pikachu123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Username already exists: ash"));
    }

    @Test
    void loginReturnsABearerToken() throws Exception {
        when(loginUseCase.login("ash", "pikachu123")).thenReturn(new IssuedToken("jwt-value", 3600));

        mockMvc.perform(json(post(AUTH_PATH + "/login"), """
                        {"username": "ash", "password": "pikachu123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void loginReturnsUnauthorizedForBadCredentials() throws Exception {
        when(loginUseCase.login(anyString(), anyString())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(json(post(AUTH_PATH + "/login"), """
                        {"username": "ash", "password": "wrong-password"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    void loginRejectsMissingFields() throws Exception {
        mockMvc.perform(json(post(AUTH_PATH + "/login"), """
                        {"username": "ash"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")));

        verifyNoInteractions(loginUseCase);
    }

    @Test
    void meReturnsTheUserOfTheToken() throws Exception {
        when(getCurrentUserUseCase.get("ash")).thenReturn(new User(1L, "ash", "hash", CREATED_AT));

        mockMvc.perform(get(AUTH_PATH + "/me").with(jwt().jwt(token -> token.subject("ash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ash"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
