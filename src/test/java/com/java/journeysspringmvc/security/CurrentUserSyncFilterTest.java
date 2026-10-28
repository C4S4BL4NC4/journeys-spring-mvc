/*
 * Unit tests for CurrentUserSyncFilter with a mocked UserService (no Spring context).
 *
 * Checks that a token-authenticated request syncs the caller's id, name, email, highest role
 * and token issue time; that the name falls back to preferred_username; that a deactivated
 * account is rejected with InvalidBearerTokenException (a 401 in the real filter chain); and
 * that anonymous requests, tokens without an email or with a non-UUID subject, and email
 * clashes never block the request.
 */
package com.java.journeysspringmvc.security;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.service.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class CurrentUserSyncFilterTest {

    private final UserService userService = mock(UserService.class);
    private final CurrentUserSyncFilter filter = new CurrentUserSyncFilter(userService);
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final UUID userId = UUID.randomUUID();
    private final Instant issuedAt = Instant.now().minusSeconds(30);

    @BeforeEach
    void activeByDefault() {
        given(userService.syncUser(any(), any(), any(), any(), any()))
                .willReturn(Optional.of(new UserDTO(userId, "Jane Doe", "jane@example.com", "default.jpg", Role.USER, null, null)));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String subject, Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).issuedAt(issuedAt);
        claims.forEach(builder::claim);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build()));
    }

    private Map<String, Object> claims(String name, String email, String... roles) {
        Map<String, Object> claims = new HashMap<>();
        if (name != null) {
            claims.put("name", name);
        }
        if (email != null) {
            claims.put("email", email);
        }
        claims.put("preferred_username", "jane.username");
        claims.put("realm_access", Map.of("roles", List.of(roles)));
        return claims;
    }

    @Test
    void syncsProfileFromTokenClaims() throws Exception {
        authenticate(userId.toString(), claims("Jane Doe", "jane@example.com", "USER", "GUIDE", "offline_access"));

        filter.doFilter(request, response, chain);

        verify(userService).syncUser(userId, "Jane Doe", "jane@example.com", Role.GUIDE, issuedAt);
        verify(chain).doFilter(request, response);
    }

    @Test
    void fallsBackToUsernameWhenNameIsMissing() throws Exception {
        authenticate(userId.toString(), claims(null, "jane@example.com", "USER"));

        filter.doFilter(request, response, chain);

        verify(userService).syncUser(userId, "jane.username", "jane@example.com", Role.USER, issuedAt);
    }

    @Test
    void passesAnonymousRequestsThrough() throws Exception {
        filter.doFilter(request, response, chain);

        verifyNoInteractions(userService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsTokensWithoutEmailOrUserId() throws Exception {
        authenticate(userId.toString(), claims("Jane Doe", null, "USER"));
        filter.doFilter(request, response, chain);

        authenticate("service-account", claims("Bot", "bot@example.com", "USER"));
        filter.doFilter(request, response, chain);

        verifyNoInteractions(userService);
        verify(chain, times(2)).doFilter(request, response);
    }

    @Test
    void doesNotFailRequestOnEmailClash() throws Exception {
        authenticate(userId.toString(), claims("Jane Doe", "taken@example.com", "USER"));
        given(userService.syncUser(any(), any(), any(), any(), any())).willThrow(new DataIntegrityViolationException("uk_users_email"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsDeactivatedAccounts() throws Exception {
        authenticate(userId.toString(), claims("Jane Doe", "jane@example.com", "USER"));
        given(userService.syncUser(any(), any(), any(), any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(InvalidBearerTokenException.class)
                .hasMessageContaining("deactivated");
        verify(chain, never()).doFilter(request, response);
    }
}
