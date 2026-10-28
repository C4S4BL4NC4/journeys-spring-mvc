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
