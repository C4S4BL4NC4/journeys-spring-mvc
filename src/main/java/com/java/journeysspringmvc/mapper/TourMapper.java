/*
 * Converts between the Tour entity and TourDTO. MapStruct generates the implementation
 * (TourMapperImpl) at compile time, so there's no hand-written copying code.
 *
 *   tourDTOToTour      builds a new entity from a request (used when creating a tour)
 *   updateTourFromDTO  copies a request onto an existing entity in place (used when updating);
 *                      collections are cleared and refilled rather than replaced, which keeps
 *                      Hibernate's tracking of them intact
 *   tourToTourDTO      builds the response, turning the guides into a set of ids via guideToId()
 * Fields the client must not control (ratings, guides, version, and on update the id and
 * timestamps) are explicitly ignored. unmappedTargetPolicy = ERROR makes the build fail if
 * a new field is added to Tour without deciding how it's mapped.
 */
package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TourMapper {
    // Ratings are computed server-side and guides are resolved from guideIds by the service
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "ratingQuantity", ignore = true)
    @Mapping(target = "guides", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "secretTour", defaultValue = "false")
    Tour tourDTOToTour(TourDTO tourDTO);

    // Full replace of client-editable fields; identity, ratings and audit fields stay as stored
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "ratingQuantity", ignore = true)
    @Mapping(target = "guides", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "secretTour", defaultValue = "false")
    void updateTourFromDTO(TourDTO tourDTO, @MappingTarget Tour tour);

    @Mapping(target = "guideIds", source = "guides")
    TourDTO tourToTourDTO(Tour tour);

    default UUID guideToId(User guide) {
        return guide.getId();
    }
}
