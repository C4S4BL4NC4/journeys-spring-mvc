package com.java.journeysspringmvc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.java.journeysspringmvc.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record UserDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        UUID id,

        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Email(message = "Enter a valid email!")
        @Size(max = 254)
        String email,

        @Size(max = 255)
        String photo,

        // Roles are assigned server-side, never by the client
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        Role role,

        // BCrypt only uses the first 72 bytes, hence the upper bound
        @NotBlank
        @Size(min = 8, max = 72)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,

        @NotBlank
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String passwordConfirm,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdAt,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        LocalDateTime lastModifiedAt
) {
    @JsonIgnore
    @AssertTrue(message = "Passwords do not match!")
    public boolean isPasswordConfirmed() {
        return Objects.equals(password, passwordConfirm);
    }
}
