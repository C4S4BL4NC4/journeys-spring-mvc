package com.java.journeysspringmvc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
}
