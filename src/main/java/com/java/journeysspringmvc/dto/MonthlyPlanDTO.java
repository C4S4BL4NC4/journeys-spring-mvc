/*
 * One row of GET /api/v1/tours/monthly-plan/{year}: how many tour departures fall in a
 * month of that year and which tours they are, like Natours' monthly-plan aggregation.
 */
package com.java.journeysspringmvc.dto;

import java.util.List;

public record MonthlyPlanDTO(
        int month,
        long numTourStarts,
        List<String> tours
) {
}
