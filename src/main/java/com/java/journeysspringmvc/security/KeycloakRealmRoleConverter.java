/*
 * Reads the app roles out of a Keycloak access token.
 *
 * Keycloak puts a user's realm roles in the token as
 *   "realm_access": { "roles": ["USER", "LEAD_GUIDE", "default-roles-journeys", "offline_access", ...] }
 * Spring Security doesn't know that layout, so this converter turns every role that matches a
 * Role constant into a "ROLE_<name>" authority, which hasRole("ADMIN") etc. then check.
 * Keycloak's own roles (default-roles-journeys, offline_access, uma_authorization) are ignored.
 */
package com.java.journeysspringmvc.security;

import com.java.journeysspringmvc.entity.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Deliberately not a @Component: SecurityConfig creates it, so MVC never registers it as a type converter
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return rolesOf(jwt).stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
    }

    public static List<Role> rolesOf(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(String::valueOf)
                .map(Role::fromName)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }
}
