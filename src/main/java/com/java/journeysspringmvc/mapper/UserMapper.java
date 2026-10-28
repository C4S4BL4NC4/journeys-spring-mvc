/*
 * Converts between the User entity and UserDTO / UserUpdateDTO. MapStruct generates the
 * implementation (UserMapperImpl) at compile time.
 *
 *   userDTOToUser      builds a new entity from a signup-style request
 *   updateUserFromDTO  applies an admin update (name, email, photo, role) to an existing user
 *   userToUserDTO      builds the response, leaving password fields empty
 * The password is never mapped: UserServiceImpl hashes it and sets it itself. Role and all
 * account-state fields (active, reset tokens, version...) are server-controlled and ignored
 * on input. A missing photo becomes User.DEFAULT_PHOTO.
 */
package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserUpdateDTO;
import com.java.journeysspringmvc.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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

    // Admin profile update; identity, password and account state stay as stored
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "photo", defaultValue = User.DEFAULT_PHOTO)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "passwordResetToken", ignore = true)
    @Mapping(target = "passwordResetExpires", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateUserFromDTO(UserUpdateDTO userUpdateDTO, @MappingTarget User user);
}
