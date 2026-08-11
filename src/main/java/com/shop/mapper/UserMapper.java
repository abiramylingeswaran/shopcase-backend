package com.shop.mapper;

import com.shop.dto.UserResponse;
import com.shop.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
