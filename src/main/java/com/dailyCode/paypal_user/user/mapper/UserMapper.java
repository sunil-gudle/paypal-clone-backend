package com.dailyCode.paypal_user.user.mapper;

import com.dailyCode.paypal_user.user.dto.UserResponse;
import com.dailyCode.paypal_user.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
