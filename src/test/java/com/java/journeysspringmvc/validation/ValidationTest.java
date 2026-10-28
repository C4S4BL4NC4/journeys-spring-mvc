package com.java.journeysspringmvc.validation;

import com.java.journeysspringmvc.dto.LocationDTO;
import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Tour;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> violatedProperties(Object target) {
        return validator.validate(target).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private TourDTO tourDto(String name, String slug, Integer duration, Difficulty difficulty, BigDecimal price,
                            BigDecimal discount, String summary, LocationDTO startLocation,
                            List<LocationDTO> locations) {
        return new TourDTO(null, name, slug, duration, 25, difficulty, null, null, price, discount,
                summary, null, "cover.jpg", List.of("1.jpg"), List.of(), false,
                startLocation, locations, Set.of(), null, null);
    }

    private TourDTO validTourDto() {
        return tourDto("The Forest Hiker", "the-forest-hiker", 5, Difficulty.EASY, new BigDecimal("397.00"),
                new BigDecimal("50.00"), "Breathtaking hike", new LocationDTO(-115.57, 51.17, "Banff", null, null),
                List.of(new LocationDTO(-116.21, 51.41, null, "Stop", 1)));
    }

    @Test
    void validUserDtoPasses() {
        UserDTO user = new UserDTO(null, "Jane", "jane@example.com", null, null,
                "password123", "password123", null, null);
        assertThat(violatedProperties(user)).isEmpty();
    }

    @Test
    void userDtoRejectsShortAndMismatchedPasswords() {
        UserDTO user = new UserDTO(null, " ", "not-an-email", null, null,
                "short", "different", null, null);
        assertThat(violatedProperties(user))
                .containsExactlyInAnyOrder("name", "email", "password", "passwordConfirmed");
    }

    @Test
    void validTourDtoPasses() {
        assertThat(violatedProperties(validTourDto())).isEmpty();
    }

    @Test
    void tourDtoComputesDurationWeeks() {
        assertThat(validTourDto().durationWeeks()).isEqualTo(5 / 7.0);
    }

    @Test
    void tourDtoRejectsMissingRequiredFieldsAndBadSlug() {
        TourDTO tour = tourDto("", "Bad Slug!", null, null, null, null, " ", null, List.of());
        assertThat(violatedProperties(tour))
                .containsExactlyInAnyOrder("name", "slug", "duration", "difficulty", "price", "summary");
    }

    @Test
    void tourDtoRejectsDiscountAbovePrice() {
        TourDTO tour = tourDto("Tour", "tour", 5, Difficulty.EASY, new BigDecimal("100.00"),
                new BigDecimal("150.00"), "Summary", null, List.of());
        assertThat(violatedProperties(tour)).containsExactly("discountBelowPrice");
    }

    @Test
    void tourDtoRejectsInvalidCoordinates() {
        TourDTO tour = tourDto("Tour", "tour", 5, Difficulty.EASY, BigDecimal.TEN, null, "Summary",
                new LocationDTO(200.0, 0.0, null, null, null),
                List.of(new LocationDTO(0.0, -95.0, null, null, 0)));
        assertThat(violatedProperties(tour)).containsExactlyInAnyOrder(
                "startLocation.longitude", "locations[0].latitude", "locations[0].day");
    }

    @Test
    void tourEntityRejectsInvalidState() {
        Tour tour = Tour.builder()
                .name("Tour")
                .slug("tour")
                .duration(5)
                .maxGroupSize(0)
                .difficulty(Difficulty.EASY)
                .averageRating(6.0)
                .price(new BigDecimal("100.00"))
                .discount(new BigDecimal("100.00"))
                .summary("Summary")
                .imageCover("cover.jpg")
                .build();
        assertThat(violatedProperties(tour))
                .containsExactlyInAnyOrder("maxGroupSize", "averageRating", "discountBelowPrice");
    }
}
