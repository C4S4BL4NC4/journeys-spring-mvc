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
 *   updateImages stores the file names produced by ImageService on the tour
 *   getTourStats / getMonthlyPlan   the Natours aggregations (secret tours excluded)
 *   findToursWithin / getDistances  the Natours geo queries on each tour's start location.
 *                Natours relies on MongoDB's geo indexes; here the database narrows the
 *                candidates to a latitude band and the exact great-circle distance is
 *                computed in Java (GeoPoint). With PostGIS this would move into SQL.
 * resolveGuides() checks every guideId belongs to an active GUIDE or LEAD_GUIDE user and
 * throws BadRequestException (-> 400) otherwise. Writes use saveAndFlush so constraint
 * errors like a duplicate slug surface here (-> 409) instead of at transaction commit.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.MonthlyPlanDTO;
import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.dto.TourDistanceDTO;
import com.java.journeysspringmvc.dto.TourFilter;
import com.java.journeysspringmvc.dto.TourStatsDTO;
import com.java.journeysspringmvc.entity.Location;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.exception.BadRequestException;
import com.java.journeysspringmvc.geo.DistanceUnit;
import com.java.journeysspringmvc.geo.GeoPoint;
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
import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public Optional<TourDTO> updateImages(UUID id, String imageCover, List<String> images) {
        return tourRepository.findById(id).map(tour -> {
            if (imageCover != null) {
                tour.setImageCover(imageCover);
            }
            if (images != null && !images.isEmpty()) {
                tour.getImages().clear();
                tour.getImages().addAll(images);
            }
            return tourMapper.tourToTourDTO(tourRepository.saveAndFlush(tour));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourStatsDTO> getTourStats(double minRating) {
        return tourRepository.findStatsByDifficulty(minRating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyPlanDTO> getMonthlyPlan(int year) {
        Map<Integer, List<String>> toursByMonth = new TreeMap<>();
        tourRepository.findStartsBetween(LocalDateTime.of(year, 1, 1, 0, 0), LocalDateTime.of(year + 1, 1, 1, 0, 0))
                .forEach(start -> toursByMonth
                        .computeIfAbsent(start.getStartDate().getMonthValue(), month -> new ArrayList<>())
                        .add(start.getName()));

        // Busiest months first, like Natours' { numTourStarts: -1 } sort
        return toursByMonth.entrySet().stream()
                .map(entry -> new MonthlyPlanDTO(entry.getKey(), entry.getValue().size(), entry.getValue()))
                .sorted(Comparator.comparingLong(MonthlyPlanDTO::numTourStarts).reversed()
                        .thenComparingInt(MonthlyPlanDTO::month))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourDTO> findToursWithin(GeoPoint center, double distance, DistanceUnit unit, boolean includeSecret) {
        double radiusKm = unit.toKm(distance);
        double latitudeDelta = Math.toDegrees(radiusKm / GeoPoint.EARTH_RADIUS_KM);
        Specification<Tour> candidates = Specification.allOf(
                visibility(includeSecret),
                atLeast("startLocation.latitude", center.latitude() - latitudeDelta),
                atMost("startLocation.latitude", center.latitude() + latitudeDelta));

        return tourRepository.findAll(candidates).stream()
                .filter(tour -> center.distanceKmTo(startPoint(tour)) <= radiusKm)
                .map(tourMapper::tourToTourDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourDistanceDTO> getDistances(GeoPoint from, DistanceUnit unit, boolean includeSecret) {
        Specification<Tour> withStart = Specification.allOf(visibility(includeSecret), isNotNull("startLocation.latitude"));

        return tourRepository.findAll(withStart).stream()
                .map(tour -> new TourDistanceDTO(tour.getId(), tour.getName(),
                        Math.round(unit.fromKm(from.distanceKmTo(startPoint(tour))) * 100) / 100.0, unit))
                .sorted(Comparator.comparingDouble(TourDistanceDTO::distance))
                .toList();
    }

    private static GeoPoint startPoint(Tour tour) {
        Location start = tour.getStartLocation();
        return new GeoPoint(start.getLatitude(), start.getLongitude());
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
        if (guides.size() != guideIds.size()
                || !guides.stream().allMatch(user -> user.isActive() && Role.GUIDES.contains(user.getRole()))) {
            throw new BadRequestException("guideIds must all reference active guides or lead guides");
        }
        return new HashSet<>(guides);
    }
}
