package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TourService {
    Page<TourDTO> listTours(String tourName, Difficulty tourDifficulty,
                            BigDecimal minPrice, BigDecimal maxPrice,
                            Integer minDuration, Integer maxDuration,
                            Double minRating, Integer maxGroupSize,
                            String sortBy,
                            Integer pageNumber, Integer pageSize);

    Optional<TourDTO> getTourById(UUID id);

    TourDTO saveNewTour(TourDTO tourDTO);

    Optional<TourDTO> updateTour(UUID id, TourDTO tourDTO);

    boolean deleteTour(UUID id);
}
