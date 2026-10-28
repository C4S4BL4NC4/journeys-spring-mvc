package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Tour;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TourMapper {
    @Mapping(target = "version", ignore = true)
    Tour tourDTOToTour(TourDTO tourDTO);

    TourDTO tourToTourDTO(Tour tour);
}
