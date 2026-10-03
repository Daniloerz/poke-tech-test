package com.poketechtest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.UserNotFoundException;
import com.poketechtest.application.port.out.UserRepository;
import com.poketechtest.domain.model.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Test
    void returnsTheUserOfTheToken() {
        User ash = new User(1L, "ash", "hash", null);
        when(userRepository.findByUsername("ash")).thenReturn(Optional.of(ash));

        assertThat(getCurrentUserUseCase.get("ash")).isEqualTo(ash);
    }

    @Test
    void failsWhenTheUserOfTheTokenNoLongerExists() {
        when(userRepository.findByUsername("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getCurrentUserUseCase.get("gone")).isInstanceOf(UserNotFoundException.class);
    }
}
