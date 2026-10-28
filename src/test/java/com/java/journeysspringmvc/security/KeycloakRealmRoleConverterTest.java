/*
 * Unit tests for KeycloakRealmRoleConverter and Role.highestOf (no Spring context).
 *
 * Checks that app roles in realm_access.roles become ROLE_* authorities, that Keycloak's own
 * roles are ignored, that tokens without realm roles get no authorities, and that the highest
 * role is picked for the local profile.
 */
package com.java.journeysspringmvc.security;

import com.java.journeysspringmvc.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    private Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("someone");
        claims.forEach(builder::claim);
        return builder.build();
    }

    private Jwt tokenWithRoles(String... roles) {
        return token(Map.of("realm_access", Map.of("roles", List.of(roles))));
    }

    @Test
    void mapsAppRolesAndIgnoresKeycloakRoles() {
        Jwt jwt = tokenWithRoles("offline_access", "LEAD_GUIDE", "default-roles-journeys", "uma_authorization", "USER");

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_LEAD_GUIDE", "ROLE_USER");
    }

    @Test
    void grantsNothingWithoutRealmRoles() {
        assertThat(converter.convert(token(Map.of("scope", "openid")))).isEmpty();
        assertThat(converter.convert(token(Map.of("realm_access", Map.of())))).isEmpty();
    }

    @Test
    void picksHighestRoleForProfile() {
        assertThat(Role.highestOf(KeycloakRealmRoleConverter.rolesOf(tokenWithRoles("USER", "ADMIN", "GUIDE"))))
                .isEqualTo(Role.ADMIN);
        assertThat(Role.highestOf(List.of())).isEqualTo(Role.USER);
    }
}
