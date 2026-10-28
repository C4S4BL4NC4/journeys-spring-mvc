/*
 * The optional filters for listing tours, bound by Spring MVC straight from query parameters:
 *   GET /api/v1/tours?name=forest&difficulty=EASY&minPrice=100&maxPrice=500
 *                    &minDuration=3&maxDuration=7&minRating=4.5&maxGroupSize=20
 * Any filter left out simply doesn't apply. Grouping them in a record keeps the controller
 * and service signatures short; paging and sorting arrive separately as a Pageable.
 */
package com.java.journeysspringmvc.dto;

import com.java.journeysspringmvc.entity.Difficulty;

import java.math.BigDecimal;

public record TourFilter(
        String name,
        Difficulty difficulty,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minDuration,
        Integer maxDuration,
        Double minRating,
        Integer maxGroupSize
) {
}
