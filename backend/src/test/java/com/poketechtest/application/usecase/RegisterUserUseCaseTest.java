package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.UsernameAlreadyExistsException;
import com.poketechtest.application.port.out.PasswordHasher;
import com.poketechtest.application.port.out.UserRepository;
import com.poketechtest.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-03T20:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        registerUserUseCase = new RegisterUserUseCase(userRepository, passwordHasher);
    }

    @Test
    void savesTheUserWithNormalizedUsernameAndHashedPassword() {
        when(userRepository.findByUsername("brock")).thenReturn(Optional.empty());
        when(passwordHasher.hash("onix12345")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new User(3L, user.username(), user.passwordHash(), CREATED_AT);
        });

        User registered = registerUserUseCase.register("Brock", "onix12345");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().username()).isEqualTo("brock");
        assertThat(savedUser.getValue().passwordHash()).isEqualTo("hashed-password");
        assertThat(registered.id()).isEqualTo(3L);
        assertThat(registered.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void rejectsAUsernameThatAlreadyExistsInAnyCase() {
        when(userRepository.findByUsername("ash")).thenReturn(Optional.of(new User(1L, "ash", "hash", CREATED_AT)));

        assertThatThrownBy(() -> registerUserUseCase.register("ASH", "pikachu123"))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessage("Username already exists: ash");
        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    void propagatesTheConflictDetectedOnSave() {
        when(userRepository.findByUsername("brock")).thenReturn(Optional.empty());
        when(passwordHasher.hash("onix12345")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenThrow(new UsernameAlreadyExistsException("brock"));

        assertThatThrownBy(() -> registerUserUseCase.register("brock", "onix12345"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }
}
