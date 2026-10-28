/*
 * Tests BootstrapData's startup seeding against the real (in-memory) database.
 *
 *   - The expected number of tours and users exists after startup, and running the seeder
 *     a second time adds nothing (it only seeds empty tables).
 *   - Seeded passwords are stored BCrypt-hashed and still match the development password.
 */
package com.java.journeysspringmvc.bootstrap;

import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.repository.TourRepository;
import com.java.journeysspringmvc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BootstrapDataTest {

    @Autowired
    BootstrapData bootstrapData;

    @Autowired
    TourRepository tourRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void populatesOnFirstRunOnly() throws Exception {
        assertThat(tourRepository.count()).isEqualTo(6);
        assertThat(userRepository.count()).isEqualTo(5);

        bootstrapData.run();

        assertThat(tourRepository.count()).isEqualTo(6);
        assertThat(userRepository.count()).isEqualTo(5);
    }

    @Test
    void storesHashedPasswords() {
        User user = userRepository.findAll().getFirst();

        assertThat(user.getPassword()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
    }
}
