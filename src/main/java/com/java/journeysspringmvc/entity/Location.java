/*
 * A geographic point stored inside the tours table, not as a table of its own (@Embeddable).
 *
 * Tour uses it twice: once as its start location (columns prefixed start_location_) and as a
 * list of stops in the tour_locations collection table. "day" is stored as day_number
 * because DAY is a reserved word in H2.
 */
package com.java.journeysspringmvc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @Size(max = 255)
    private String address;

    @Size(max = 255)
    private String description;

    // Day of the tour this stop is visited; unused for the start location. DAY is a reserved word in H2
    @Positive
    @Column(name = "day_number")
    private Integer day;
}
