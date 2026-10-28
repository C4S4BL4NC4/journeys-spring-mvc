/*
 * The JSON shape of a user in responses from the user endpoints.
 *
 * Read-only: accounts are created and edited in Keycloak (registration page, account
 * console, admin console), and the app mirrors them. There are no password fields because
 * the app never sees passwords.
 */
package com.java.journeysspringmvc.dto;

import com.java.journeysspringmvc.entity.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        String email,
        String photo,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime lastModifiedAt
) {
}
