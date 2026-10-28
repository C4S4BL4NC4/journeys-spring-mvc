/*
 * Tests BootstrapData's startup seeding against the real (in-memory) database.
 *
 *   - The expected number of tours and users exists after startup, and running the seeder
 *     a second time adds nothing (it only seeds empty tables).
 *   - The seeded profiles match the seed users in keycloak/journeys-realm.json (same ids,
 *     emails and roles), so logging in as a seed user lands on the right profile and the two
 *     files can't silently drift apart.
 */
package com.java.journeysspringmvc.bootstrap;

import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.repository.TourRepository;
import com.java.journeysspringmvc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.jayway.jsonpath.JsonPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BootstrapDataTest {

    @Autowired
    BootstrapData bootstrapData;

    @Autowired
    TourRepository tourRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void populatesOnFirstRunOnly() throws Exception {
        assertThat(tourRepository.count()).isEqualTo(6);
        assertThat(userRepository.count()).isEqualTo(5);

        bootstrapData.run();

        assertThat(tourRepository.count()).isEqualTo(6);
        assertThat(userRepository.count()).isEqualTo(5);
    }

    @Test
    void seedsProfilesMatchingKeycloakRealm() throws Exception {
        List<Map<String, Object>> realmUsers = JsonPath.read(
                Files.readString(Path.of("keycloak/journeys-realm.json")), "$.users");

        assertThat(realmUsers).hasSize(5);
        for (Map<String, Object> realmUser : realmUsers) {
            User profile = userRepository.findById(UUID.fromString((String) realmUser.get("id"))).orElseThrow();
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmUser.get("realmRoles");

            assertThat(profile.getEmail()).isEqualTo(realmUser.get("email"));
            assertThat(profile.getName()).isEqualTo(realmUser.get("firstName") + " " + realmUser.get("lastName"));
            assertThat(realmRoles).contains(profile.getRole().name());
        }
    }
}
