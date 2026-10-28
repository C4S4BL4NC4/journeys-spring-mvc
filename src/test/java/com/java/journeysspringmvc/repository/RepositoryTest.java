/*
 * Persistence tests for TourRepository and UserRepository using @DataJpaTest.
 *
 * Each test runs in a transaction that's rolled back afterwards, against the in-memory H2
 * database built by the real Flyway migrations. Covers generated fields and defaults (id,
 * timestamps, version), user ids assigned from Keycloak, saving and reloading a tour's collections and guides, unique
 * constraints (slug, case-insensitive email) and the findAllByRoleIn derived query.
 */
package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    @Autowired
    TourRepository tourRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager entityManager;

    private Tour.TourBuilder validTour(String slug) {
        return Tour.builder()
                .name("The Forest Hiker")
                .slug(slug)
                .duration(5)
                .maxGroupSize(25)
                .difficulty(Difficulty.EASY)
                .price(new BigDecimal("397.00"))
                .summary("Breathtaking hike")
                .imageCover("cover.jpg");
    }

    // User ids come from Keycloak, so tests assign them instead of letting the database generate them
    private User validUser(String email, Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Jane")
                .email(email)
                .role(role)
                .build();
    }

    @Test
    void savesTourWithGeneratedFields() {
        Tour saved = tourRepository.saveAndFlush(validTour("the-forest-hiker").build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRatingQuantity()).isZero();
        assertThat(saved.getSecretTour()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void persistsTourDetailsAndCollections() {
        User guide = userRepository.saveAndFlush(validUser("guide@example.com", Role.GUIDE));
        LocalDateTime later = LocalDateTime.of(2027, 7, 20, 9, 0);
        LocalDateTime sooner = LocalDateTime.of(2027, 4, 25, 9, 0);

        Tour saved = tourRepository.saveAndFlush(validTour("detailed-tour")
                .images(new ArrayList<>(List.of("b.jpg", "a.jpg")))
                .startDates(new LinkedHashSet<>(List.of(later, sooner)))
                .startLocation(Location.builder().longitude(-115.57).latitude(51.17).address("Banff").build())
                .locations(new ArrayList<>(List.of(
                        Location.builder().longitude(-116.21).latitude(51.41).description("Stop 1").day(1).build(),
                        Location.builder().longitude(-118.07).latitude(52.87).description("Stop 2").day(3).build())))
                .guides(Set.of(guide))
                .build());
        entityManager.clear();

        Tour reloaded = tourRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getImages()).containsExactly("b.jpg", "a.jpg");
        assertThat(reloaded.getStartDates()).containsExactly(sooner, later);
        assertThat(reloaded.getStartLocation().getAddress()).isEqualTo("Banff");
        assertThat(reloaded.getLocations()).extracting(Location::getDay).containsExactly(1, 3);
        assertThat(reloaded.getGuides()).extracting(User::getId).containsExactly(guide.getId());
    }

    @Test
    void rejectsDuplicateSlug() {
        tourRepository.saveAndFlush(validTour("same").build());

        assertThatThrownBy(() -> tourRepository.saveAndFlush(validTour("same").name("Other").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesUserWithAssignedIdAndDefaults() {
        UUID keycloakId = UUID.randomUUID();
        User saved = userRepository.saveAndFlush(User.builder()
                .id(keycloakId)
                .name("Jane")
                .email("  Jane.Doe@Example.COM ")
                .build());

        assertThat(saved.getId()).isEqualTo(keycloakId);
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(saved.getPhoto()).isEqualTo(User.DEFAULT_PHOTO);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void rejectsDuplicateEmailRegardlessOfCase() {
        userRepository.saveAndFlush(validUser("jane@example.com", Role.USER));

        assertThatThrownBy(() -> userRepository.saveAndFlush(validUser("JANE@example.com", Role.USER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsUsersByRole() {
        userRepository.saveAll(List.of(
                validUser("lead@example.com", Role.LEAD_GUIDE),
                validUser("guide@example.com", Role.GUIDE),
                validUser("user@example.com", Role.USER)));

        assertThat(userRepository.findAllByRoleIn(List.of(Role.LEAD_GUIDE, Role.GUIDE)))
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("lead@example.com", "guide@example.com");
    }
}
