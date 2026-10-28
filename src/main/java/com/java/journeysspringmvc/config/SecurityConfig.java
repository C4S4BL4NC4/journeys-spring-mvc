/*
 * Interim Spring Security setup until real authentication (signup/login with JWTs) is built.
 *
 * Rules:
 *   - GET requests on /api/v1/tours/** are public, so anyone can browse tours like in Natours.
 *   - Every other request needs HTTP Basic credentials; currently the single admin user
 *     defined by spring.security.user.* in application.properties.
 *   - CSRF protection is off and sessions are stateless: this is a JSON API with no login
 *     cookie, so CSRF tokens don't apply and would otherwise block POST/PUT/DELETE calls.
 * There are no role checks yet; any authenticated user can do everything.
 */
package com.java.journeysspringmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Interim setup until real auth lands: HTTP Basic with the admin user from application.properties
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Browsing tours is public, like Natours
                        .requestMatchers(HttpMethod.GET, "/api/v1/tours", "/api/v1/tours/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // Stateless API with no session cookie, so CSRF tokens don't apply
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
