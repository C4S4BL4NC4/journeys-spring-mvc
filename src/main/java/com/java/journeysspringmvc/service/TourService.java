/*
 * The contract for tour business logic, used by TourController.
 *
 * Methods work with TourDTOs, not entities, so controllers never touch JPA objects.
 * includeSecret says whether the caller may see secret tours (TOUR_MANAGERS may); for
 * everyone else secret tours behave as if they didn't exist.
 * "Not found" is expressed without exceptions: Optional.empty() for lookups/updates and
 * false for deletes; the controller decides how to turn that into HTTP.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.TourFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface TourService {
    Page<TourDTO> listTours(TourFilter filter, boolean includeSecret, Pageable pageable);

    Optional<TourDTO> getTourById(UUID id, boolean includeSecret);

    TourDTO saveNewTour(TourDTO tourDTO);

    Optional<TourDTO> updateTour(UUID id, TourDTO tourDTO);

    boolean deleteTour(UUID id);
}
