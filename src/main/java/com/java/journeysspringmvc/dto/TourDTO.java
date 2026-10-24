package com.java.journeysspringmvc.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TourDTO(
        UUID id,
        String name,
        String slug,
        Double averageRating,
        Integer ratingQuantity,
        BigDecimal price,
        BigDecimal discount,
        LocalDateTime createdAt,
        LocalDateTime lastModifiedAt
) {
}
