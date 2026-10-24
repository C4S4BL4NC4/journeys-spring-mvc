package com.java.journeysspringmvc.mapper;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Tour;
import org.mapstruct.Mapper;

@Mapper
public interface TourMapper {
    Tour TourDTOToTour(TourDTO tourDTO);
    TourDTO TourToTourDTO(Tour tour);
}
