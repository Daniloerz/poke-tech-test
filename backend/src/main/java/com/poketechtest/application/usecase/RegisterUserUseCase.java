package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.UsernameAlreadyExistsException;
import com.poketechtest.application.port.out.PasswordHasher;
import com.poketechtest.application.port.out.UserRepository;
import com.poketechtest.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Transactional
    public User register(String username, String rawPassword) {
        String normalizedUsername = User.normalizeUsername(username);
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new UsernameAlreadyExistsException(normalizedUsername);
        }

        User registered = userRepository.save(User.newUser(normalizedUsername, passwordHasher.hash(rawPassword)));
        log.info("User registered: id={}, username={}", registered.id(), registered.username());
        return registered;
    }
}
