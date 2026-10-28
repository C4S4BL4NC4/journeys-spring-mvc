/*
 * Request body for PATCH /api/v1/users/{userId}: an admin changing a user's role.
 *
 * Only the role is managed through this API (it's applied in Keycloak and mirrored locally).
 * Name, email and password belong to Keycloak and are edited there. Roles may be written
 * Natours-style, e.g. {"role": "lead-guide"}.
 */
package com.java.journeysspringmvc.dto;

import com.java.journeysspringmvc.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UserUpdateDTO(
        @NotNull
        Role role
) {
}
