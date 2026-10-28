package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.mapper.TourMapperImpl;
import com.java.journeysspringmvc.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TourServiceImpl.class, TourMapperImpl.class})
class TourServiceImplTest {

    @Autowired
    TourService tourService;

    @Autowired
    TourRepository tourRepository;

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
}
