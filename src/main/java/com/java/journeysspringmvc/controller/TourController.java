/*
 * REST endpoints for tours under /api/v1/tours.
 *
 *   GET    /api/v1/tours            list tours; optional query params:
 *                                   name, difficulty, minPrice, maxPrice, minDuration, maxDuration,
 *                                   minRating, maxGroupSize, sort (e.g. "-price,averageRating"),
 *                                   page (1-based) and size
 *   GET    /api/v1/tours/{tourId}   one tour, or 404
 *   POST   /api/v1/tours            create; 201 with a Location header pointing at the new tour
 *   PUT    /api/v1/tours/{tourId}   replace a tour's editable fields, or 404
 *   DELETE /api/v1/tours/{tourId}   delete; 204, or 404
 *
 * The controller only translates HTTP to service calls: request bodies are validated with
 * @Validated (400 on failure), the work happens in TourService, and missing tours become a
 * NotFoundException that GlobalExceptionHandler turns into a 404 response.
 * List results are wrapped in PagedModel, giving JSON shaped like {"content": [...], "page": {...}}.
 */
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
