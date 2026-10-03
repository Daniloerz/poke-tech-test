package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.InvalidCredentialsException;
import com.poketechtest.application.port.out.IssuedToken;
import com.poketechtest.application.port.out.PasswordHasher;
import com.poketechtest.application.port.out.TokenIssuer;
import com.poketechtest.application.port.out.UserRepository;
import com.poketechtest.domain.model.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final String DUMMY_HASH = "dummy-hash";
    private static final User ASH = new User(1L, "ash", "ash-hash", null);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        when(passwordHasher.hash(any())).thenReturn(DUMMY_HASH);
        loginUseCase = new LoginUseCase(userRepository, passwordHasher, tokenIssuer);
    }

    @Test
    void returnsATokenForValidCredentials() {
        IssuedToken token = new IssuedToken("jwt-value", 3600);
        when(userRepository.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(passwordHasher.matches("pikachu123", "ash-hash")).thenReturn(true);
        when(tokenIssuer.issue(ASH)).thenReturn(token);

        assertThat(loginUseCase.login("ash", "pikachu123")).isEqualTo(token);
    }

    @Test
    void matchesTheUsernameCaseInsensitively() {
        when(userRepository.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(passwordHasher.matches("pikachu123", "ash-hash")).thenReturn(true);
        when(tokenIssuer.issue(ASH)).thenReturn(new IssuedToken("jwt-value", 3600));

        assertThat(loginUseCase.login("  ASH ", "pikachu123").value()).isEqualTo("jwt-value");
    }

    @Test
    void rejectsAWrongPassword() {
        when(userRepository.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(passwordHasher.matches("wrong-password", "ash-hash")).thenReturn(false);

        assertThatThrownBy(() -> loginUseCase.login("ash", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password.");
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void rejectsAnUnknownUserWithTheSameErrorAfterCheckingTheDummyHash() {
        when(userRepository.findByUsername("gary")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginUseCase.login("gary", "eevee12345"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password.");
        // Same work as a wrong password, so the response time does not reveal if the user exists.
        verify(passwordHasher).matches("eevee12345", DUMMY_HASH);
        verify(tokenIssuer, never()).issue(any());
    }
}
