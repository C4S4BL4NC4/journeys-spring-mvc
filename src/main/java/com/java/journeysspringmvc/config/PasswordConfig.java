/*
 * Provides the application's PasswordEncoder bean.
 *
 * It uses Spring Security's delegating encoder, which hashes with BCrypt and prefixes the
 * hash with the algorithm id, e.g. "{bcrypt}$2a$10$...". The prefix lets the app switch
 * algorithms later while still verifying passwords hashed the old way. Used by
 * UserServiceImpl when creating users and by BootstrapData when seeding them.
 */
package com.java.journeysspringmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    // Delegating encoder hashes with BCrypt and prefixes the algorithm id, e.g. {bcrypt}$2a$...
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
