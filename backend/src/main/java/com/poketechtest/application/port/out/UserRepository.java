package com.poketechtest.application.port.out;

import com.poketechtest.domain.model.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    /** Throws UsernameAlreadyExistsException if the username is taken (unique constraint). */
    User save(User user);
}
