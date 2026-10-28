/*
 * Runs the persistence layer against a real PostgreSQL 17 (the docker-compose image) using
 * Testcontainers, instead of the in-memory H2 the other tests use (needs Docker).
 *
 * H2's PostgreSQL mode is close but not identical, so this guards the things that only a
 * real Postgres can prove: every Flyway migration applies cleanly and Hibernate's schema
 * validation accepts the result, and the queries with database-specific behaviour work
 * (element collections and guides, the JPQL stats aggregation, the monthly-plan join over
 * start dates, and LIKE escaping). @ServiceConnection points the datasource at the container.
 */
package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.dto.MonthlyPlanDTO;
import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.TourFilter;
import com.java.journeysspringmvc.dto.TourStatsDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Location;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.TourMapper;
import com.java.journeysspringmvc.service.TourService;
import com.java.journeysspringmvc.service.TourServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TourServiceImpl.class)
class PostgresMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.11");

    // Built via MapStruct's factory so the test doesn't reference the generated TourMapperImpl directly
    @TestConfiguration
    static class MapperConfig {
        @Bean
        TourMapper tourMapper() {
            return Mappers.getMapper(TourMapper.class);
        }
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TourRepository tourRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TourService tourService;

    @Autowired
    EntityManager entityManager;

    private User guide() {
        return userRepository.saveAndFlush(User.builder()
                .id(UUID.randomUUID())
                .name("Gus Guide")
                .email("gus@example.com")
                .role(Role.GUIDE)
                .build());
    }

    private Tour tour(String name, Difficulty difficulty, String price, double rating, User guide, LocalDateTime... starts) {
        return Tour.builder()
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-'))
                .duration(5)
                .maxGroupSize(10)
                .difficulty(difficulty)
                .averageRating(rating)
                .ratingQuantity(10)
                .price(new BigDecimal(price))
                .summary("Summary")
                .imageCover("cover.jpg")
                .images(new ArrayList<>(List.of("1.jpg", "2.jpg")))
                .startDates(new LinkedHashSet<>(List.of(starts)))
                .startLocation(Location.builder().latitude(51.17).longitude(-115.57).address("Banff").build())
                .locations(new ArrayList<>(List.of(Location.builder().latitude(51.4).longitude(-116.2).day(1).build())))
                .guides(Set.of(guide))
                .build();
    }

    @Test
    void appliesEveryMigration() {
        List<String> applied = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);

        assertThat(applied).containsExactly("1", "2", "3");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'users' and column_name = 'password'",
                Integer.class)).isZero();
    }

    @Test
    void persistsToursWithCollectionsAndGuides() {
        User guide = guide();
        UUID id = tourRepository.saveAndFlush(tour("Forest Hiker", Difficulty.EASY, "397.00", 4.7, guide,
                LocalDateTime.of(2031, 7, 1, 9, 0))).getId();
        entityManager.clear();

        Tour reloaded = tourRepository.findById(id).orElseThrow();
        assertThat(reloaded.getImages()).containsExactly("1.jpg", "2.jpg");
        assertThat(reloaded.getLocations()).extracting(Location::getDay).containsExactly(1);
        assertThat(reloaded.getGuides()).extracting(User::getId).containsExactly(guide.getId());
    }

    @Test
    void runsAggregationsAndEscapedSearches() {
        User guide = guide();
        tourRepository.saveAllAndFlush(List.of(
                tour("Forest Hiker", Difficulty.EASY, "400.00", 4.7, guide, LocalDateTime.of(2031, 7, 1, 9, 0)),
                tour("City Wanderer", Difficulty.EASY, "600.00", 4.9, guide, LocalDateTime.of(2031, 7, 9, 9, 0)),
                tour("Snow 100% Fun", Difficulty.DIFFICULT, "900.00", 4.8, guide, LocalDateTime.of(2031, 3, 2, 9, 0))));

        List<TourStatsDTO> stats = tourService.getTourStats(4.5);
        assertThat(stats).extracting(TourStatsDTO::difficulty).containsExactly(Difficulty.EASY, Difficulty.DIFFICULT);
        assertThat(stats.getFirst().avgPrice()).isEqualTo(500.0);

        assertThat(tourService.getMonthlyPlan(2031)).extracting(MonthlyPlanDTO::month, MonthlyPlanDTO::numTourStarts)
                .containsExactly(tuple(7, 2L), tuple(3, 1L));

        TourFilter percent = new TourFilter("100%", null, null, null, null, null, null, null);
        assertThat(tourService.listTours(percent, false, PageRequest.of(0, 10)))
                .extracting(TourDTO::name).containsExactly("Snow 100% Fun");
    }

    @Test
    void storesDeactivationTimestamps() {
        User user = guide();
        Instant deactivatedAt = Instant.parse("2031-01-02T03:04:05.123456Z");
        user.setDeactivatedAt(deactivatedAt);
        userRepository.saveAndFlush(user);
        entityManager.clear();

        assertThat(userRepository.findById(user.getId()).orElseThrow().getDeactivatedAt()).isEqualTo(deactivatedAt);
    }
}
