package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {
    // The service hashes the raw password and sets it, so it is never mapped as-is
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "photo", defaultValue = User.DEFAULT_PHOTO)
    // Account state is internal and never set from client input
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "passwordResetToken", ignore = true)
    @Mapping(target = "passwordResetExpires", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "version", ignore = true)
    User userDTOToUser(UserDTO userDTO);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "passwordConfirm", ignore = true)
    UserDTO userToUserDTO(User user);
}
