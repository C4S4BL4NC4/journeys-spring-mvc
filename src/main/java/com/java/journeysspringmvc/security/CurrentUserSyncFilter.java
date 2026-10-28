/*
 * Keeps the local users table in step with Keycloak ("just-in-time provisioning") and turns
 * away deactivated accounts, like Natours' "protect" middleware.
 *
 * Runs on every request after Spring Security has verified the bearer token. For a
 * token-authenticated request it copies the caller's id ("sub"), name ("name"), email
 * ("email") and highest app role from the token into the users table via
 * UserService.syncUser. A user's first API call creates their profile, and later calls pick
 * up changes made in Keycloak. Anonymous requests pass straight through.
 *
 * If the account was deactivated after the token was issued (admin delete or deleteMe), the
 * request is rejected with 401 invalid_token, even though the token itself is still valid.
 * Tokens without an email or with a non-UUID subject are skipped, and a clash with another
 * profile's email (e.g. a user deleted and re-created in Keycloak) is logged; neither fails
 * the request.
 */
package com.java.journeysspringmvc.security;

import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Deliberately not a @Component: Spring Boot would also register it as a plain servlet filter
@Slf4j
@RequiredArgsConstructor
public class CurrentUserSyncFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            sync(token.getToken());
        }
        filterChain.doFilter(request, response);
    }

    private void sync(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.warn("Skipping profile sync: token subject {} is not a Keycloak user id", jwt.getSubject());
            return;
        }

        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email)) {
            log.warn("Skipping profile sync for {}: token has no email claim", userId);
            return;
        }

        boolean active;
        try {
            active = userService.syncUser(userId, displayName(jwt, email), email,
                    Role.highestOf(KeycloakRealmRoleConverter.rolesOf(jwt)), jwt.getIssuedAt()).isPresent();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Could not sync profile for {}: email {} belongs to another profile", userId, email);
            return;
        }
        if (!active) {
            // SecurityConfig places this filter after ExceptionTranslationFilter, which answers with a 401
            throw new InvalidBearerTokenException("This account has been deactivated");
        }
    }

    private static String displayName(Jwt jwt, String email) {
        String name = jwt.getClaimAsString("name");
        if (StringUtils.hasText(name)) {
            return name;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return StringUtils.hasText(username) ? username : email;
    }
}
