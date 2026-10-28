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
 *   PATCH  /api/v1/tours/{tourId}   change only the fields sent (JSON Merge Patch, RFC 7396), or 404
 *   PUT    /api/v1/tours/{tourId}/images  multipart upload: "imageCover" (one file) and/or
 *                                  "images" (up to 3 files); resized and stored by ImageService
 *   DELETE /api/v1/tours/{tourId}   delete; 204, or 404
 *
 * Natours extras:
 *   GET /api/v1/tours/top-5-cheap                               best rated, then cheapest, 5 rated tours
 *   GET /api/v1/tours/stats?minRating=4.5                       aggregates per difficulty
 *   GET /api/v1/tours/monthly-plan/{year}                       departures per month (staff only)
 *   GET /api/v1/tours/tours-within/{distance}/center/{lat,lng}/unit/{mi|km}
 *   GET /api/v1/tours/distances/{lat,lng}/unit/{mi|km}
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

import com.java.journeysspringmvc.dto.MonthlyPlanDTO;
import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.TourDistanceDTO;
import com.java.journeysspringmvc.dto.TourFilter;
import com.java.journeysspringmvc.dto.TourStatsDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.exception.BadRequestException;
import com.java.journeysspringmvc.exception.NotFoundException;
import com.java.journeysspringmvc.geo.DistanceUnit;
import com.java.journeysspringmvc.geo.GeoPoint;
import com.java.journeysspringmvc.service.TourService;
import com.java.journeysspringmvc.storage.ImageService;
import com.java.journeysspringmvc.util.JsonMergePatcher;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TourController {

    public static final String TOUR_PATH = "/api/v1/tours";
    public static final String TOUR_PATH_ID = TOUR_PATH + "/{tourId}";
    public static final String TOUR_IMAGES_PATH = TOUR_PATH_ID + "/images";
    public static final String TOP_CHEAP_PATH = TOUR_PATH + "/top-5-cheap";
    public static final String STATS_PATH = TOUR_PATH + "/stats";
    public static final String MONTHLY_PLAN_PATH = TOUR_PATH + "/monthly-plan/{year}";
    public static final String TOURS_WITHIN_PATH = TOUR_PATH + "/tours-within/{distance}/center/{latLng}/unit/{unit}";
    public static final String DISTANCES_PATH = TOUR_PATH + "/distances/{latLng}/unit/{unit}";

    private static final String MERGE_PATCH_JSON = "application/merge-patch+json";
    private static final int MAX_TOUR_IMAGES = 3;
    // Natours' alias: best rated first, then cheapest. Only rated tours compete (Natours gives every
    // tour a default rating; here unrated tours have none, and the database would sort them first)
    private static final TourFilter RATED_TOURS = new TourFilter(null, null, null, null, null, null, 1.0, null);
    private static final Pageable TOP_FIVE_CHEAP =
            PageRequest.of(0, 5, Sort.by(Sort.Order.desc("averageRating"), Sort.Order.asc("price")));

    private final TourService tourService;
    private final ImageService imageService;
    private final JsonMergePatcher jsonMergePatcher;

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

    // Only tour managers get here (SecurityConfig), so secret tours can be patched too
    @PatchMapping(path = TOUR_PATH_ID, consumes = {MERGE_PATCH_JSON, MediaType.APPLICATION_JSON_VALUE})
    public TourDTO patchTour(@PathVariable UUID tourId, @RequestBody JsonNode patch) {
        TourDTO current = tourService.getTourById(tourId, true).orElseThrow(() -> tourNotFound(tourId));
        TourDTO patched = jsonMergePatcher.apply(current, patch, TourDTO.class);

        return tourService.updateTour(tourId, patched).orElseThrow(() -> tourNotFound(tourId));
    }

    @PutMapping(path = TOUR_IMAGES_PATH, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TourDTO uploadTourImages(@PathVariable UUID tourId,
                                    @RequestPart(required = false) MultipartFile imageCover,
                                    @RequestPart(required = false) List<MultipartFile> images) {
        List<MultipartFile> imageFiles = images == null ? List.of() : images;
        if (imageCover == null && imageFiles.isEmpty()) {
            throw new BadRequestException("Upload an imageCover and/or up to " + MAX_TOUR_IMAGES + " images");
        }
        if (imageFiles.size() > MAX_TOUR_IMAGES) {
            throw new BadRequestException("A tour can have at most " + MAX_TOUR_IMAGES + " images");
        }
        // Check the tour exists before writing any files
        tourService.getTourById(tourId, true).orElseThrow(() -> tourNotFound(tourId));

        String cover = imageCover == null ? null : imageService.saveTourCover(tourId, imageCover);
        List<String> names = imageFiles.isEmpty() ? List.of() : imageService.saveTourImages(tourId, imageFiles);
        return tourService.updateImages(tourId, cover, names).orElseThrow(() -> tourNotFound(tourId));
    }

    @DeleteMapping(TOUR_PATH_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTour(@PathVariable UUID tourId) {
        if (!tourService.deleteTour(tourId)) {
            throw tourNotFound(tourId);
        }
    }

    @GetMapping(TOP_CHEAP_PATH)
    public List<TourDTO> getTopFiveCheapTours(Authentication authentication) {
        return tourService.listTours(RATED_TOURS, canSeeSecretTours(authentication), TOP_FIVE_CHEAP).getContent();
    }

    @GetMapping(STATS_PATH)
    public List<TourStatsDTO> getTourStats(@RequestParam(defaultValue = "4.5") @Min(1) @Max(5) double minRating) {
        return tourService.getTourStats(minRating);
    }

    @GetMapping(MONTHLY_PLAN_PATH)
    public List<MonthlyPlanDTO> getMonthlyPlan(@PathVariable @Min(1) @Max(9999) int year) {
        return tourService.getMonthlyPlan(year);
    }

    @GetMapping(TOURS_WITHIN_PATH)
    public List<TourDTO> getToursWithin(@PathVariable @Positive double distance, @PathVariable String latLng,
                                        @PathVariable DistanceUnit unit, Authentication authentication) {
        return tourService.findToursWithin(GeoPoint.parse(latLng), distance, unit, canSeeSecretTours(authentication));
    }

    @GetMapping(DISTANCES_PATH)
    public List<TourDistanceDTO> getDistances(@PathVariable String latLng, @PathVariable DistanceUnit unit,
                                              Authentication authentication) {
        return tourService.getDistances(GeoPoint.parse(latLng), unit, canSeeSecretTours(authentication));
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
