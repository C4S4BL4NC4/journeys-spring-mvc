/*
 * JPA entity for a user's profile, mapped to the "users" table.
 *
 * Keycloak owns the account itself (login, password, password reset, disabling the account).
 * This table is a local mirror of each user's profile so the app can link users to tours as
 * guides and list them:
 *   - id is the user's Keycloak id (the "sub" claim of their tokens), not generated here
 *   - name, email and role are copied from the token claims by UserService.syncUser on the
 *     user's requests, so changes made in Keycloak show up on their next request
 *   - photo is app-owned data that Keycloak doesn't know about
 * Emails are trimmed and lowercased before every insert/update (normalizeEmail), so the
 * unique constraint treats emails case-insensitively. version provides optimistic locking.
 */
package com.java.journeysspringmvc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
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

    // Assigned from the Keycloak user id rather than generated
    @Id
    @Column(length = 36, columnDefinition = "varchar", updatable = false, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;
    @Column(length = 50, nullable = false)
    private String name;
    @Column(length = 254, nullable = false, unique = true)
    private String email;
    @Builder.Default
    @Column(nullable = false)
    private String photo = DEFAULT_PHOTO;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastModifiedAt;

    // Also tells Spring Data whether the entity is new, since the id is assigned rather than generated
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
