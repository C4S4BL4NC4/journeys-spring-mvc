/*
 * The contract for user profile logic, used by UserController and CurrentUserSyncFilter.
 *
 * Accounts are created, edited and disabled in Keycloak, so there are no create/update/
 * delete methods here. The app only reads its mirror of user profiles and keeps it in sync
 * with token claims via syncUser. As with TourService, "not found" is Optional.empty().
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Role;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Page<UserDTO> listUsers(String name, Role role, String sortBy, Integer pageNumber, Integer pageSize);

    Optional<UserDTO> getUserById(UUID id);

    UserDTO syncUser(UUID id, String name, String email, Role role);
}
