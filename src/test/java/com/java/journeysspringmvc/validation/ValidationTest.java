package com.java.journeysspringmvc.validation;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Tour;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
        TourDTO tour = new TourDTO(null, "The Forest Hiker", "the-forest-hiker", null, null,
                new BigDecimal("397.00"), new BigDecimal("50.00"), null, null);
        assertThat(violatedProperties(tour)).isEmpty();
    }

    @Test
    void tourDtoRejectsMissingPriceAndBadSlug() {
        TourDTO tour = new TourDTO(null, "", "Bad Slug!", null, null, null, null, null, null);
        assertThat(violatedProperties(tour)).containsExactlyInAnyOrder("name", "slug", "price");
    }

    @Test
    void tourDtoRejectsDiscountAbovePrice() {
        TourDTO tour = new TourDTO(null, "Tour", "tour", null, null,
                new BigDecimal("100.00"), new BigDecimal("150.00"), null, null);
        assertThat(violatedProperties(tour)).containsExactly("discountBelowPrice");
    }

    @Test
    void tourEntityRejectsInvalidState() {
        Tour tour = Tour.builder()
                .name("Tour")
                .slug("tour")
                .averageRating(6.0)
                .price(new BigDecimal("100.00"))
                .discount(new BigDecimal("100.00"))
                .build();
        assertThat(violatedProperties(tour)).containsExactlyInAnyOrder("averageRating", "discountBelowPrice");
    }
}
