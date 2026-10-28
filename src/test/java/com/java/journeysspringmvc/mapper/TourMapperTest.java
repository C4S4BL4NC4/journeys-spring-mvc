/*
 * Unit tests for the MapStruct-generated TourMapper (no Spring context needed).
 *
 * Checks that mapping a DTO to an entity keeps server-side defaults instead of trusting the
 * client (e.g. ratings), and that a tour's guides are turned into a set of guide ids.
 */
package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.LocationDTO;
import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TourMapperTest {

    private final TourMapper mapper = Mappers.getMapper(TourMapper.class);

    @Test
    void mapsDtoToEntityKeepingServerDefaults() {
        LocalDateTime start = LocalDateTime.of(2027, 4, 25, 9, 0);
        TourDTO dto = new TourDTO(null, "Tour", "tour", 5, 25, Difficulty.EASY, 4.0, null,
                BigDecimal.TEN, null, "Summary", null, "cover.jpg", List.of("1.jpg"), List.of(start), true,
                new LocationDTO(-115.57, 51.17, "Banff", null, null),
                List.of(new LocationDTO(-116.21, 51.41, null, "Stop", 1)),
                Set.of(UUID.randomUUID()), null, null);

        Tour tour = mapper.tourDTOToTour(dto);

        assertThat(tour.getRatingQuantity()).isZero();
        assertThat(tour.getAverageRating()).isNull();
        assertThat(tour.getGuides()).isEmpty();
        assertThat(tour.getSecretTour()).isTrue();
        assertThat(tour.getStartDates()).containsExactly(start);
        assertThat(tour.getStartLocation().getLongitude()).isEqualTo(-115.57);
        assertThat(tour.getLocations()).singleElement().extracting("day").isEqualTo(1);
    }

    @Test
    void mapsGuidesToIds() {
        User guide = User.builder().id(UUID.randomUUID()).role(Role.GUIDE).build();
        Tour tour = Tour.builder().name("Tour").duration(14).guides(Set.of(guide)).build();

        TourDTO dto = mapper.tourToTourDTO(tour);

        assertThat(dto.guideIds()).containsExactly(guide.getId());
        assertThat(dto.durationWeeks()).isEqualTo(2.0);
    }
}
