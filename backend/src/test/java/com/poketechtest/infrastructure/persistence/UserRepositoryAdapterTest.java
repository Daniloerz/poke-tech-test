package com.poketechtest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.poketechtest.application.exception.UsernameAlreadyExistsException;
import com.poketechtest.domain.model.User;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-03T20:00:00Z");

    @Mock
    private UserJpaRepository userJpaRepository;

    private UserRepositoryAdapter userRepositoryAdapter;

    @BeforeEach
    void setUp() {
        userRepositoryAdapter = new UserRepositoryAdapter(userJpaRepository, Mappers.getMapper(UserEntityMapper.class));
    }

    @Test
    void findByUsernameMapsTheEntityToTheDomain() {
        when(userJpaRepository.findByUsername("ash")).thenReturn(Optional.of(entity(1L, "ash", "hash")));

        assertThat(userRepositoryAdapter.findByUsername("ash")).contains(new User(1L, "ash", "hash", CREATED_AT));
    }

    @Test
    void saveReturnsTheStoredUserWithIdAndCreationTime() {
        when(userJpaRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity toSave = invocation.getArgument(0);
            return entity(3L, toSave.getUsername(), toSave.getPasswordHash());
        });

        User saved = userRepositoryAdapter.save(User.newUser("brock", "hash"));

        assertThat(saved).isEqualTo(new User(3L, "brock", "hash", CREATED_AT));
    }

    @Test
    void saveTranslatesTheUniqueUsernameViolation() {
        when(userJpaRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(constraintViolation(UserRepositoryAdapter.USERNAME_UNIQUE_CONSTRAINT));

        assertThatThrownBy(() -> userRepositoryAdapter.save(User.newUser("ash", "hash")))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void saveRethrowsOtherConstraintViolations() {
        DataIntegrityViolationException otherViolation = constraintViolation("ck_app_user_username_lower");
        when(userJpaRepository.saveAndFlush(any(UserEntity.class))).thenThrow(otherViolation);

        assertThatThrownBy(() -> userRepositoryAdapter.save(User.newUser("ash", "hash"))).isSameAs(otherViolation);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException("violation", new SQLException("violation"), constraintName);
        return new DataIntegrityViolationException("violation", cause);
    }

    private UserEntity entity(Long id, String username, String passwordHash) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        entity.setCreatedAt(CREATED_AT);
        return entity;
    }
}
