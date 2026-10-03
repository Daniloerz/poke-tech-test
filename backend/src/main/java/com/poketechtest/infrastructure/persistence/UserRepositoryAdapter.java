package com.poketechtest.infrastructure.persistence;

import com.poketechtest.application.exception.UsernameAlreadyExistsException;
import com.poketechtest.application.port.out.UserRepository;
import com.poketechtest.domain.model.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserEntityMapper userEntityMapper;

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(userEntityMapper::toDomain);
    }

    @Override
    public User save(User user) {
        try {
            return userEntityMapper.toDomain(userJpaRepository.saveAndFlush(userEntityMapper.toEntity(user)));
        } catch (DataIntegrityViolationException exception) {
            // Two registrations with the same username at the same time: the unique constraint wins.
            throw new UsernameAlreadyExistsException(user.username());
        }
    }
}
