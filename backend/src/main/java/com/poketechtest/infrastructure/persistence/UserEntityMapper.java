package com.poketechtest.infrastructure.persistence;

import com.poketechtest.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserEntityMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User user);
}
