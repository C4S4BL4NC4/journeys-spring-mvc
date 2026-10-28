/*
 * Converts the User entity to UserDTO for responses. MapStruct generates the
 * implementation (UserMapperImpl) at compile time.
 *
 * There's no DTO-to-entity mapping: users are never created from request bodies anymore.
 * UserServiceImpl builds and updates them from Keycloak token claims instead.
 */
package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {
    UserDTO userToUserDTO(User user);
}
