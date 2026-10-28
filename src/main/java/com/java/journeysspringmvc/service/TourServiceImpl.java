package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.TourMapper;
import com.java.journeysspringmvc.repository.TourRepository;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.util.PageRequests;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

import static com.java.journeysspringmvc.repository.TourSpecifications.*;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final static Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));
    private final static Set<String> SORTABLE_FIELDS = Set.of(
            "name", "price", "duration", "maxGroupSize", "difficulty",
            "averageRating", "ratingQuantity", "createdAt");
    private final static Set<Role> GUIDE_ROLES = Set.of(Role.GUIDE, Role.LEAD_GUIDE);

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
    public Page<TourDTO> listTours(String tourName, Difficulty tourDifficulty, BigDecimal minPrice, BigDecimal maxPrice, Integer minDuration, Integer maxDuration, Double minRating, Integer maxGroupSize, String sortBy, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = PageRequests.of(sortBy, pageNumber, pageSize, SORTABLE_FIELDS, DEFAULT_SORT);

        Specification<Tour> spec = Specification.allOf(
                nameContains(tourName),
                hasDifficulty(tourDifficulty),
                atLeast("price", minPrice),
                atMost("price", maxPrice),
                atLeast("duration", minDuration),
                atMost("duration", maxDuration),
                atLeast("averageRating", minRating),
                atMost("maxGroupSize", maxGroupSize));

        return tourRepository.findAll(spec, pageRequest).map(tourMapper::tourToTourDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TourDTO> getTourById(UUID id) {
        return tourRepository.findById(id).map(tourMapper::tourToTourDTO);
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

    private Set<User> resolveGuides(Set<UUID> guideIds) {
        if (guideIds == null || guideIds.isEmpty()) {
            return new HashSet<>();
        }

        List<User> guides = userRepository.findAllById(guideIds);
        if (guides.size() != guideIds.size() || !guides.stream().allMatch(user -> GUIDE_ROLES.contains(user.getRole()))) {
            throw new IllegalArgumentException("guideIds must all reference existing guides or lead guides");
        }
        return new HashSet<>(guides);
    }
}
