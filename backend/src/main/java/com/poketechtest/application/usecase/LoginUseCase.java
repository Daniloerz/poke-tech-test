package com.poketechtest.application.usecase;

import com.poketechtest.application.exception.InvalidCredentialsException;
import com.poketechtest.application.port.out.IssuedToken;
import com.poketechtest.application.port.out.PasswordHasher;
import com.poketechtest.application.port.out.TokenIssuer;
import com.poketechtest.application.port.out.UserRepository;
import com.poketechtest.domain.model.User;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginUseCase {

    private static final String DUMMY_PASSWORD = "dummy-password-for-unknown-users";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final String dummyPasswordHash;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.dummyPasswordHash = passwordHasher.hash(DUMMY_PASSWORD);
    }

    public IssuedToken login(String username, String rawPassword) {
        Optional<User> user = userRepository.findByUsername(User.normalizeUsername(username));

        // Unknown users are checked against a dummy hash, so both failures take the same time (no user enumeration).
        String passwordHash = user.map(User::passwordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordHasher.matches(rawPassword, passwordHash);

        User authenticated = user.filter(found -> passwordMatches).orElseThrow(() -> {
            log.info("Failed login attempt");
            return new InvalidCredentialsException();
        });
        log.debug("User logged in: id={}", authenticated.id());
        return tokenIssuer.issue(authenticated);
    }
}
