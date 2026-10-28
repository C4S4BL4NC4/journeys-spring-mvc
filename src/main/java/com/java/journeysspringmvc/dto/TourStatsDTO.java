/*
 * One row of GET /api/v1/tours/stats: aggregates over the well-rated, non-secret tours of
 * one difficulty, like Natours' tour-stats aggregation pipeline.
 */
package com.java.journeysspringmvc.dto;

import com.java.journeysspringmvc.entity.Difficulty;

import java.math.BigDecimal;

public record TourStatsDTO(
        Difficulty difficulty,
        Long numTours,
        Long numRatings,
        Double avgRating,
        Double avgPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
