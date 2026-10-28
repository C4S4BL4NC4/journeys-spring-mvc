package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.exception.NotFoundException;
import com.java.journeysspringmvc.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TourController {

    public static final String TOUR_PATH = "/api/v1/tours";
    public static final String TOUR_PATH_ID = TOUR_PATH + "/{tourId}";

    private final TourService tourService;

    @GetMapping(TOUR_PATH)
    public PagedModel<TourDTO> listTours(@RequestParam(required = false) String name,
                                         @RequestParam(required = false) Difficulty difficulty,
                                         @RequestParam(required = false) BigDecimal minPrice,
                                         @RequestParam(required = false) BigDecimal maxPrice,
                                         @RequestParam(required = false) Integer minDuration,
                                         @RequestParam(required = false) Integer maxDuration,
                                         @RequestParam(required = false) Double minRating,
                                         @RequestParam(required = false) Integer maxGroupSize,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return new PagedModel<>(tourService.listTours(name, difficulty, minPrice, maxPrice,
                minDuration, maxDuration, minRating, maxGroupSize, sort, page, size));
    }

    @GetMapping(TOUR_PATH_ID)
    public TourDTO getTourById(@PathVariable UUID tourId) {
        return tourService.getTourById(tourId).orElseThrow(() -> tourNotFound(tourId));
    }

    @PostMapping(TOUR_PATH)
    public ResponseEntity<TourDTO> createTour(@Validated @RequestBody TourDTO tourDTO, UriComponentsBuilder uriBuilder) {
        TourDTO saved = tourService.saveNewTour(tourDTO);

        return ResponseEntity.created(uriBuilder.path(TOUR_PATH_ID).buildAndExpand(saved.id()).toUri()).body(saved);
    }

    @PutMapping(TOUR_PATH_ID)
    public TourDTO updateTour(@PathVariable UUID tourId, @Validated @RequestBody TourDTO tourDTO) {
        return tourService.updateTour(tourId, tourDTO).orElseThrow(() -> tourNotFound(tourId));
    }

    @DeleteMapping(TOUR_PATH_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTour(@PathVariable UUID tourId) {
        if (!tourService.deleteTour(tourId)) {
            throw tourNotFound(tourId);
        }
    }

    private static NotFoundException tourNotFound(UUID tourId) {
        return new NotFoundException("No tour found with id " + tourId);
    }
}
