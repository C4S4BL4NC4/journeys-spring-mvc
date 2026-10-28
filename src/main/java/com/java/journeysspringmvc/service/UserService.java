/*
 * The contract for user profile logic, used by UserController and CurrentUserSyncFilter.
 *
 * Accounts live in Keycloak. This service reads the app's mirror of user profiles, keeps it
 * in sync with token claims (syncUser), and performs the account changes the API offers by
 * applying them in Keycloak first and then locally: changing a role, deactivating an
 * account, and setting the app-owned photo. Deactivated users behave as if they didn't exist.
 * As with TourService, "not found" is Optional.empty() or false.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserFilter;
import com.java.journeysspringmvc.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Page<UserDTO> listUsers(UserFilter filter, Pageable pageable);

    Optional<UserDTO> getUserById(UUID id);

    // Empty when the account was deactivated and the token predates that
    Optional<UserDTO> syncUser(UUID id, String name, String email, Role role, Instant tokenIssuedAt);

    Optional<UserDTO> changeRole(UUID id, Role role);

    boolean deactivateUser(UUID id);

    Optional<UserDTO> updatePhoto(UUID id, String photo);
}
