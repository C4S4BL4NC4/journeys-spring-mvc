/*
 * REST endpoints for tours under /api/v1/tours.
 *
 *   GET    /api/v1/tours            list tours. Optional query params:
 *                                  filters  name, difficulty, minPrice, maxPrice, minDuration,
 *                                           maxDuration, minRating, maxGroupSize (see TourFilter)
 *                                  paging   page (0-based), size, sort=field[,asc|desc] (repeatable),
 *                                           e.g. ?sort=price,desc&sort=averageRating; newest first by default
 *   GET    /api/v1/tours/{tourId}   one tour, or 404
 *   POST   /api/v1/tours            create; 201 with a Location header pointing at the new tour
 *   PUT    /api/v1/tours/{tourId}   replace a tour's editable fields, or 404
 *   DELETE /api/v1/tours/{tourId}   delete; 204, or 404
 *
 * Secret tours are only listed or returned for TOUR_MANAGERS (ADMIN, LEAD_GUIDE); for anyone
 * else, including anonymous callers, they don't exist. Writes are restricted to the same
 * roles by SecurityConfig.
 *
 * The controller only translates HTTP to service calls: request bodies are validated with
 * @Validated (400 on failure), the work happens in TourService, and missing tours become a
 * NotFoundException that GlobalExceptionHandler turns into a 404 response.
 * Pages serialize as {"content": [...], "page": {size, number, totalElements, totalPages}}.
 */
package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.TourFilter;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.exception.NotFoundException;
import com.java.journeysspringmvc.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TourController {

    public static final String TOUR_PATH = "/api/v1/tours";
    public static final String TOUR_PATH_ID = TOUR_PATH + "/{tourId}";

    private final TourService tourService;

    @GetMapping(TOUR_PATH)
    public Page<TourDTO> listTours(@ParameterObject TourFilter filter,
                                   @ParameterObject @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                   Authentication authentication) {
        return tourService.listTours(filter, canSeeSecretTours(authentication), pageable);
    }

    @GetMapping(TOUR_PATH_ID)
    public TourDTO getTourById(@PathVariable UUID tourId, Authentication authentication) {
        return tourService.getTourById(tourId, canSeeSecretTours(authentication))
                .orElseThrow(() -> tourNotFound(tourId));
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

    // Anonymous callers get a null Authentication
    private static boolean canSeeSecretTours(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> Role.TOUR_MANAGERS.stream().map(Role::authority).anyMatch(authority::equals));
    }

    private static NotFoundException tourNotFound(UUID tourId) {
        return new NotFoundException("No tour found with id " + tourId);
    }
}
