package com.java.journeysspringmvc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.java.journeysspringmvc.entity.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TourDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        UUID id,

        @NotBlank
        @Size(max = 50)
        String name,

        @Size(max = 60)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be lowercase words separated by hyphens")
        String slug,

        @NotNull
        @Positive
        Integer duration,

        @NotNull
        @Positive
        Integer maxGroupSize,

        @NotNull
        Difficulty difficulty,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        Double averageRating,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        Integer ratingQuantity,

        @NotNull
        @Positive
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        @Digits(integer = 8, fraction = 2)
        BigDecimal discount,

        @NotBlank
        @Size(max = 255)
        String summary,

        @Size(max = 2000)
        String description,

        @NotBlank
        @Size(max = 255)
        String imageCover,

        List<@NotBlank @Size(max = 255) String> images,

        List<@NotNull LocalDateTime> startDates,

        // Optional on input; the mapper defaults a missing value to false
        Boolean secretTour,

        @Valid
        LocationDTO startLocation,

        List<@Valid @NotNull LocationDTO> locations,

        // Guides are referenced by user id; the service resolves them to users
        Set<@NotNull UUID> guideIds,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdAt,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        LocalDateTime lastModifiedAt
) {
    @JsonIgnore
    @AssertTrue(message = "discount must be lower than price")
    public boolean isDiscountBelowPrice() {
        return discount == null || price == null || discount.compareTo(price) < 0;
    }

    // Natours' durationWeeks virtual property
    @JsonProperty(value = "durationWeeks", access = JsonProperty.Access.READ_ONLY)
    public Double durationWeeks() {
        return duration == null ? null : duration / 7.0;
    }
}
