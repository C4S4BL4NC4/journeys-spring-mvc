/*
 * Business logic for tours: the implementation behind TourService.
 *
 *   listTours    combines the optional TourFilter fields into one Specification query (via
 *                Specs), hides secret tours unless includeSecret, and pages/sorts with the
 *                client's Pageable after checking its sort fields against SORTABLE_FIELDS
 *   getTourById  looks one tour up, again hiding secret tours unless includeSecret
 *   saveNewTour  generates the slug from the name when none is given (like Natours' pre-save
 *                hook, e.g. "The Désert Crossing!" -> "the-desert-crossing") and resolves guideIds
 *   updateTour   full replace of the editable fields; keeps the existing slug if none is sent,
 *                so renaming a tour doesn't break its URL
 *   deleteTour   deletes if it exists
 * resolveGuides() checks every guideId belongs to an existing GUIDE or LEAD_GUIDE user and
 * throws BadRequestException (-> 400) otherwise. Writes use saveAndFlush so constraint
 * errors like a duplicate slug surface here (-> 409) instead of at transaction commit.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.TourFilter;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.exception.BadRequestException;
import com.java.journeysspringmvc.mapper.TourMapper;
import com.java.journeysspringmvc.repository.TourRepository;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.util.Pageables;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.*;

import static com.java.journeysspringmvc.repository.Specs.*;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final static Set<String> SORTABLE_FIELDS = Set.of(
            "name", "price", "duration", "maxGroupSize", "difficulty",
            "averageRating", "ratingQuantity", "createdAt");

    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourMapper tourMapper;

    private static String slugify(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourDTO> listTours(TourFilter filter, boolean includeSecret, Pageable pageable) {
        Specification<Tour> spec = Specification.allOf(
                visibility(includeSecret),
                containsIgnoreCase("name", filter.name()),
                equalTo("difficulty", filter.difficulty()),
                atLeast("price", filter.minPrice()),
                atMost("price", filter.maxPrice()),
                atLeast("duration", filter.minDuration()),
                atMost("duration", filter.maxDuration()),
                atLeast("averageRating", filter.minRating()),
                atMost("maxGroupSize", filter.maxGroupSize()));

        return tourRepository.findAll(spec, Pageables.requireSortable(pageable, SORTABLE_FIELDS))
                .map(tourMapper::tourToTourDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TourDTO> getTourById(UUID id, boolean includeSecret) {
        return tourRepository.findById(id)
                .filter(tour -> includeSecret || !tour.getSecretTour())
                .map(tourMapper::tourToTourDTO);
    }

    @Override
    @Transactional
    public TourDTO saveNewTour(TourDTO tourDTO) {
        Tour tour = tourMapper.tourDTOToTour(tourDTO);
        tour.setId(null);
        if (!StringUtils.hasText(tour.getSlug())) {
            tour.setSlug(slugify(tour.getName()));
        }
        tour.setGuides(resolveGuides(tourDTO.guideIds()));

        return tourMapper.tourToTourDTO(tourRepository.saveAndFlush(tour));
    }

    @Override
    @Transactional
    public Optional<TourDTO> updateTour(UUID id, TourDTO tourDTO) {
        return tourRepository.findById(id).map(tour -> {
            String existingSlug = tour.getSlug();
            tourMapper.updateTourFromDTO(tourDTO, tour);
            // Keep the existing slug (and so existing URLs) unless a new one was given
            if (!StringUtils.hasText(tour.getSlug())) {
                tour.setSlug(existingSlug);
            }
            tour.setGuides(resolveGuides(tourDTO.guideIds()));

            return tourMapper.tourToTourDTO(tourRepository.saveAndFlush(tour));
        });
    }

    @Override
    @Transactional
    public boolean deleteTour(UUID id) {
        if (!tourRepository.existsById(id)) {
            return false;
        }
        tourRepository.deleteById(id);
        return true;
    }

    // Like Natours' query middleware, secret tours are left out for regular callers
    private static Specification<Tour> visibility(boolean includeSecret) {
        return equalTo("secretTour", includeSecret ? null : false);
    }

    private Set<User> resolveGuides(Set<UUID> guideIds) {
        if (guideIds == null || guideIds.isEmpty()) {
            return new HashSet<>();
        }

        List<User> guides = userRepository.findAllById(guideIds);
        if (guides.size() != guideIds.size() || !guides.stream().allMatch(user -> Role.GUIDES.contains(user.getRole()))) {
            throw new BadRequestException("guideIds must all reference existing guides or lead guides");
        }
        return new HashSet<>(guides);
    }
}
