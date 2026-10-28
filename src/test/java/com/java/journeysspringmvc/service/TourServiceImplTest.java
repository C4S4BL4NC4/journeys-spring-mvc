/*
 * Tests TourServiceImpl against a real (in-memory) database, using @DataJpaTest plus the service.
 *
 * setUp() replaces whatever tours are in the database with four known tours; that cleanup is
 * rolled back after each test. TourMapper is provided through MapStruct's factory so the test
 * doesn't depend on the generated class name. Covers:
 *   - filtering: none, name, difficulty + price range, duration/rating/group size combined
 *   - sorting by several fields, ignoring unknown sort fields, and 1-based paging
 *   - get, create (slug generation, given slug, guide resolution and validation),
 *     update (fields replaced, slug and ratings kept) and delete
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.TourMapper;
import com.java.journeysspringmvc.repository.TourRepository;
import com.java.journeysspringmvc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TourServiceImpl.class)
class TourServiceImplTest {

    // Built via MapStruct's factory so the test doesn't reference the generated TourMapperImpl directly
    @TestConfiguration
    static class MapperConfig {
        @Bean
        TourMapper tourMapper() {
            return Mappers.getMapper(TourMapper.class);
        }
    }

    @Autowired
    TourService tourService;

    @Autowired
    TourRepository tourRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // The in-memory DB is shared with @SpringBootTest classes that seed sample tours; rolled back after each test
        tourRepository.deleteAll();
        tourRepository.flush();
        tourRepository.saveAllAndFlush(List.of(
                tour("The Forest Hiker", Difficulty.EASY, "397.00", 5, 25, 4.7),
                tour("The Sea Explorer", Difficulty.MEDIUM, "497.00", 7, 15, 4.8),
                tour("The Snow Adventurer", Difficulty.DIFFICULT, "997.00", 4, 10, 4.5),
                tour("The City Wanderer", Difficulty.EASY, "1197.00", 9, 20, 4.6)));
    }

    private Tour tour(String name, Difficulty difficulty, String price, int duration, int maxGroupSize, double rating) {
        return Tour.builder()
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-'))
                .duration(duration)
                .maxGroupSize(maxGroupSize)
                .difficulty(difficulty)
                .averageRating(rating)
                .price(new BigDecimal(price))
                .summary("Summary")
                .imageCover("cover.jpg")
                .build();
    }

    private Page<TourDTO> list(String name, Difficulty difficulty, String minPrice, String maxPrice, String sortBy) {
        return tourService.listTours(name, difficulty,
                minPrice == null ? null : new BigDecimal(minPrice),
                maxPrice == null ? null : new BigDecimal(maxPrice),
                null, null, null, null, sortBy, null, null);
    }

    @Test
    void returnsAllToursWithoutFilters() {
        assertThat(list(null, null, null, null, null).getTotalElements()).isEqualTo(4);
    }

    @Test
    void filtersByNameCaseInsensitively() {
        assertThat(list("sea", null, null, null, null))
                .extracting(TourDTO::name)
                .containsExactly("The Sea Explorer");
    }

    @Test
    void combinesDifficultyAndPriceRange() {
        assertThat(list(null, Difficulty.EASY, "300", "500", null))
                .extracting(TourDTO::name)
                .containsExactly("The Forest Hiker");
    }

    @Test
    void filtersByDurationRatingAndGroupSize() {
        Page<TourDTO> page = tourService.listTours(null, null, null, null,
                5, 8, 4.7, 20, null, null, null);

        assertThat(page).extracting(TourDTO::name).containsExactly("The Sea Explorer");
    }

    @Test
    void sortsByMultipleFieldsWithDirection() {
        assertThat(list(null, null, null, null, "difficulty,-price"))
                .extracting(TourDTO::name)
                .containsExactly("The Snow Adventurer", "The City Wanderer", "The Forest Hiker", "The Sea Explorer");
    }

    @Test
    void ignoresUnknownSortFields() {
        assertThat(list(null, null, null, null, "-bogus,price"))
                .extracting(TourDTO::name)
                .containsExactly("The Forest Hiker", "The Sea Explorer", "The Snow Adventurer", "The City Wanderer");
    }

    @Test
    void paginatesFromOneBasedPageNumber() {
        Page<TourDTO> page = tourService.listTours(null, null, null, null,
                null, null, null, null, "price", 2, 3);

        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page).extracting(TourDTO::name).containsExactly("The City Wanderer");
    }

    private TourDTO dto(String name, String slug, Set<UUID> guideIds) {
        return new TourDTO(null, name, slug, 6, 12, Difficulty.MEDIUM, null, null,
                new BigDecimal("599.00"), null, "New summary", null, "new-cover.jpg",
                List.of("a.jpg"), List.of(), false, null, List.of(), guideIds, null, null);
    }

    private User user(String email, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .name("Test User")
                .email(email)
                .role(role)
                .password("{bcrypt}$2a$10$hashedvalue")
                .build());
    }

    private Tour existingTour(String name) {
        return tourRepository.findAll().stream()
                .filter(tour -> tour.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void getsTourById() {
        UUID id = existingTour("The Sea Explorer").getId();

        assertThat(tourService.getTourById(id)).get().extracting(TourDTO::name).isEqualTo("The Sea Explorer");
    }

    @Test
    void returnsEmptyForUnknownId() {
        assertThat(tourService.getTourById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void savesNewTourWithGeneratedSlugAndGuides() {
        User guide = user("new.guide@example.com", Role.GUIDE);

        TourDTO saved = tourService.saveNewTour(dto("The Désert Crossing!", null, Set.of(guide.getId())));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.slug()).isEqualTo("the-desert-crossing");
        assertThat(saved.guideIds()).containsExactly(guide.getId());
        assertThat(tourRepository.count()).isEqualTo(5);
    }

    @Test
    void keepsSlugGivenOnCreate() {
        assertThat(tourService.saveNewTour(dto("Custom", "my-custom-slug", null)).slug()).isEqualTo("my-custom-slug");
    }

    @Test
    void rejectsGuidesThatAreNotGuides() {
        User regularUser = user("regular@example.com", Role.USER);

        assertThatThrownBy(() -> tourService.saveNewTour(dto("Bad Guides", null, Set.of(regularUser.getId()))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tourService.saveNewTour(dto("Missing Guides", null, Set.of(UUID.randomUUID()))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesTourAndKeepsSlugAndRatings() {
        Tour original = existingTour("The Forest Hiker");

        TourDTO updated = tourService.updateTour(original.getId(), dto("The Renamed Hiker", null, null)).orElseThrow();

        assertThat(updated.id()).isEqualTo(original.getId());
        assertThat(updated.name()).isEqualTo("The Renamed Hiker");
        assertThat(updated.slug()).isEqualTo("the-forest-hiker");
        assertThat(updated.price()).isEqualByComparingTo("599.00");
        assertThat(updated.averageRating()).isEqualTo(4.7);
        assertThat(updated.images()).containsExactly("a.jpg");
        assertThat(updated.createdAt()).isNotNull();
    }

    @Test
    void returnsEmptyWhenUpdatingUnknownTour() {
        assertThat(tourService.updateTour(UUID.randomUUID(), dto("Nope", null, null))).isEmpty();
    }

    @Test
    void deletesTour() {
        UUID id = existingTour("The City Wanderer").getId();

        assertThat(tourService.deleteTour(id)).isTrue();
        assertThat(tourRepository.existsById(id)).isFalse();
        assertThat(tourService.deleteTour(id)).isFalse();
    }
}
