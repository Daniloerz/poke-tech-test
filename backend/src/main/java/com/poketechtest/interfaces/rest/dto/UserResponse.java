package com.poketechtest.interfaces.rest.dto;

import com.poketechtest.domain.model.User;
import java.time.Instant;

public record UserResponse(Long id, String username, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.createdAt());
    }
}
