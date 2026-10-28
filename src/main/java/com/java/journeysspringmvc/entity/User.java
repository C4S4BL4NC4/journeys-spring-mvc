package com.java.journeysspringmvc.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
    public static final String DEFAULT_PHOTO = "default.jpg";

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(length = 36, columnDefinition = "varchar", updatable = false, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @NotBlank
    @Size(max = 50)
    @Column(length = 50, nullable = false)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(length = 254, nullable = false, unique = true)
    private String email;

    @Size(max = 255)
    @Builder.Default
    @Column(nullable = false)
    private String photo = DEFAULT_PHOTO;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    // Stores the password hash, never the raw password
    @NotBlank
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String password;

    private LocalDateTime passwordChangedAt;

    // SHA-256 hex of the reset token emailed to the user, never the token itself
    @Size(max = 64)
    @Column(length = 64)
    private String passwordResetToken;

    private LocalDateTime passwordResetExpires;

    // Deactivated accounts are kept rather than deleted, like Natours
    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastModifiedAt;

    @Version
    private Integer version;

    // Emails are case-insensitive, so they are stored lowercase like Natours does
    @PrePersist
    @PreUpdate
    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
