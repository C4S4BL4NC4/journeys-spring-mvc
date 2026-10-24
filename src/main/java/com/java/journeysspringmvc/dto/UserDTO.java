package com.java.journeysspringmvc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,

        @Email(message = "Enter a valid email!")
        String email,

        String photo,
        String role,

        @Min(8)
        String password,

        @Min(8)
        String passwordConfirm,

        LocalDateTime createdAt,
        LocalDateTime lastModifiedAt
) {
}
