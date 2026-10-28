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
 * GUIDES, STAFF and TOUR_MANAGERS name the groups the app checks, so each is defined once.
 * Requests may spell roles Natours-style ("lead-guide"); responses use the constant names.
 */
package com.java.journeysspringmvc.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.java.journeysspringmvc.util.Enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum Role {
    USER,
    GUIDE,
    LEAD_GUIDE,
    ADMIN;

    // Roles that can be assigned as a tour's guides
    public static final Set<Role> GUIDES = EnumSet.of(GUIDE, LEAD_GUIDE);
    // Staff who plan tour departures (e.g. the monthly plan)
    public static final Set<Role> STAFF = EnumSet.of(GUIDE, LEAD_GUIDE, ADMIN);
    // Roles that can create, edit and delete tours, and see secret ones
    public static final Set<Role> TOUR_MANAGERS = EnumSet.of(LEAD_GUIDE, ADMIN);

    // Keycloak also puts its own roles (e.g. offline_access) in tokens; those aren't app roles
    public static Optional<Role> fromName(String name) {
        return Arrays.stream(values()).filter(role -> role.name().equals(name)).findFirst();
    }

    // Accepts Natours-style "lead-guide" as well as "LEAD_GUIDE" in request bodies
    @JsonCreator
    public static Role fromJson(String value) {
        return Enums.lenientValueOf(Role.class, value);
    }

    public static Role highestOf(Collection<Role> roles) {
        return roles.stream().max(Comparator.naturalOrder()).orElse(USER);
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
