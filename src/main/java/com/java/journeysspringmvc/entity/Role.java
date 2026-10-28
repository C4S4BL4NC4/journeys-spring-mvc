/*
 * The roles a user can have, as in Natours, listed from least to most privileged.
 *
 *   USER        a regular customer (every new account gets this role)
 *   GUIDE       can be assigned to lead tours
 *   LEAD_GUIDE  senior guide, also assignable to tours and allowed to manage them
 *   ADMIN       full access, including user management
 *
 * The roles are defined in Keycloak as realm roles with exactly these names (see
 * keycloak/journeys-realm.json) and arrive in each token's realm_access.roles claim.
 * A user can hold several; highestOf() picks the one mirrored into the users table.
 * Stored by name in the database. Only GUIDE and LEAD_GUIDE users can be a tour's guides.
 */
package com.java.journeysspringmvc.entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public enum Role {
    USER,
    GUIDE,
    LEAD_GUIDE,
    ADMIN;

    // Keycloak also puts its own roles (e.g. offline_access) in tokens; those aren't app roles
    public static Optional<Role> fromName(String name) {
        return Arrays.stream(values()).filter(role -> role.name().equals(name)).findFirst();
    }

    public static Role highestOf(Collection<Role> roles) {
        return roles.stream().max(Comparator.naturalOrder()).orElse(USER);
    }
}
