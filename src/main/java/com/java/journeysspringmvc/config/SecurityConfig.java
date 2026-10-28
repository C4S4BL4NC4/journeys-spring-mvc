/*
 * Spring Security setup: this API is an OAuth2 resource server for Keycloak.
 *
 * Authentication: users log in at Keycloak (see docker-compose.yml and
 * keycloak/journeys-realm.json) and call the API with "Authorization: Bearer <access token>".
 * Spring Boot builds the token decoder from spring.security.oauth2.resourceserver.jwt.* in
 * application.properties: it fetches Keycloak's public signing keys from the issuer and
 * checks each token's signature, expiry, issuer and audience ("journeys-api"). The API never
 * sees passwords and keeps no sessions, so CSRF protection is off.
 *
 * After a token is verified:
 *   - KeycloakRealmRoleConverter turns its realm roles into ROLE_* authorities
 *   - CurrentUserSyncFilter mirrors the caller's profile into the users table
 *
 * Access rules (first match wins):
 *   Swagger UI/docs, GET tours            public
 *   GET /api/v1/users/me                  any logged-in user
 *   create/update/delete tours            ADMIN or LEAD_GUIDE
 *   other /api/v1/users endpoints         ADMIN
 *   anything else                         any logged-in user
 * Missing or invalid tokens get 401; a valid token without the required role gets 403.
 */
package com.java.journeysspringmvc.config;

import com.java.journeysspringmvc.controller.UserController;
import com.java.journeysspringmvc.security.CurrentUserSyncFilter;
import com.java.journeysspringmvc.security.KeycloakRealmRoleConverter;
import com.java.journeysspringmvc.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserService userService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tours", "/api/v1/tours/**").permitAll()
                        .requestMatchers(HttpMethod.GET, UserController.ME_PATH).authenticated()
                        .requestMatchers("/api/v1/tours", "/api/v1/tours/**").hasAnyRole("ADMIN", "LEAD_GUIDE")
                        .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(new CurrentUserSyncFilter(userService), BearerTokenAuthenticationFilter.class)
                // Stateless API with no session cookie, so CSRF tokens don't apply
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    // The authentication's name stays the token's "sub" claim, i.e. the Keycloak user id
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
