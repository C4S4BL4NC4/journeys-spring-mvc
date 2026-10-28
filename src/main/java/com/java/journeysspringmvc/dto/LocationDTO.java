package com.java.journeysspringmvc.dto;

import jakarta.validation.constraints.*;

public record LocationDTO(
        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @Size(max = 255)
        String address,

        @Size(max = 255)
        String description,

        // Day of the tour this stop is visited; unused for the start location
        @Positive
        Integer day
) {
}
