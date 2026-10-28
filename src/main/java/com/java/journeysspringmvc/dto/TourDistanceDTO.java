/*
 * One row of GET /api/v1/tours/distances/{latlng}/unit/{unit}: how far a tour's start
 * location is from the given point, in the requested unit (rounded to 2 decimals).
 */
package com.java.journeysspringmvc.dto;

import com.java.journeysspringmvc.geo.DistanceUnit;

import java.util.UUID;

public record TourDistanceDTO(
        UUID id,
        String name,
        double distance,
        DistanceUnit unit
) {
}
