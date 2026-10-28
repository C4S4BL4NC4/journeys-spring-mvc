/*
 * Request body for an admin updating an existing user (PUT /api/v1/users/{userId}).
 *
 * Kept separate from UserDTO because an update must not require or change the password;
 * passwords will get their own change/reset flow, like in Natours. It's a full replace:
 * name, email and role are required, and a missing photo resets to the default photo.
 */
package com.java.journeysspringmvc.dto;

import com.java.journeysspringmvc.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Admin update of a user's profile and role; passwords change through their own flow, like Natours
public record UserUpdateDTO(
        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Email(message = "Enter a valid email!")
        @Size(max = 254)
        String email,

        @Size(max = 255)
        String photo,

        @NotNull
        Role role
) {
}
