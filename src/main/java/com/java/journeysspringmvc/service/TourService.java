/*
 * The contract for tour business logic, used by TourController.
 *
 * Methods work with TourDTOs, not entities, so controllers never touch JPA objects.
 * includeSecret says whether the caller may see secret tours (TOUR_MANAGERS may); for
 * everyone else secret tours behave as if they didn't exist.
 * The Natours extras are here too: stats per difficulty, the monthly departure plan, and the
 * geo queries (tours within a radius, distances to every tour from a point).
 * "Not found" is expressed without exceptions: Optional.empty() for lookups/updates and
 * false for deletes; the controller decides how to turn that into HTTP.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.MonthlyPlanDTO;
import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.TourDistanceDTO;
import com.java.journeysspringmvc.dto.TourFilter;
import com.java.journeysspringmvc.dto.TourStatsDTO;
import com.java.journeysspringmvc.geo.DistanceUnit;
import com.java.journeysspringmvc.geo.GeoPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourService {
    Page<TourDTO> listTours(TourFilter filter, boolean includeSecret, Pageable pageable);

    Optional<TourDTO> getTourById(UUID id, boolean includeSecret);

    TourDTO saveNewTour(TourDTO tourDTO);

    Optional<TourDTO> updateTour(UUID id, TourDTO tourDTO);

    boolean deleteTour(UUID id);

    // A null cover or empty image list leaves that field as it is
    Optional<TourDTO> updateImages(UUID id, String imageCover, List<String> images);

    List<TourStatsDTO> getTourStats(double minRating);

    List<MonthlyPlanDTO> getMonthlyPlan(int year);

    List<TourDTO> findToursWithin(GeoPoint center, double distance, DistanceUnit unit, boolean includeSecret);

    List<TourDistanceDTO> getDistances(GeoPoint from, DistanceUnit unit, boolean includeSecret);
}
