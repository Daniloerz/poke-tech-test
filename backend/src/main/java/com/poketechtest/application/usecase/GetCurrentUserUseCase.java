package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.UserNotFoundException;
import com.poketechtest.application.port.out.UserRepository;
import com.poketechtest.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public User get(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
