package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.mapper.TourMapper;
import com.java.journeysspringmvc.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.java.journeysspringmvc.repository.TourSpecifications.*;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final static int DEFAULT_PAGE_NUMBER = 0;
    private final static int DEFAULT_PAGE_SIZE = 25;
    private final static Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));
    private final static Set<String> SORTABLE_FIELDS = Set.of(
            "name", "price", "duration", "maxGroupSize", "difficulty",
            "averageRating", "ratingQuantity", "createdAt");

    private final TourRepository tourRepository;
    private final TourMapper tourMapper;


    @Override
    public Page<TourDTO> listTours(String tourName, Difficulty tourDifficulty, BigDecimal minPrice, BigDecimal maxPrice, Integer minDuration, Integer maxDuration, Double minRating, Integer maxGroupSize, String sortBy, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(sortBy, pageNumber, pageSize);

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
    public Optional<TourDTO> getTourById(UUID id) {
        return Optional.empty();
    }

    @Override
    public TourDTO saveNewTour(TourDTO tourDTO) {
        return null;
    }

    @Override
    public Optional<TourDTO> updateTour(UUID id, TourDTO tourDTO) {
        return Optional.empty();
    }

    @Override
    public boolean deleteTour(UUID id) {
        return false;
    }


    private PageRequest buildPageRequest(String sortBy, Integer pageNumber, Integer pageSize) {
        int queryPageNumber = pageNumber == null || pageNumber <= 0 ? DEFAULT_PAGE_NUMBER : pageNumber - 1;
        // Set maximum page size constants later 1000
        int queryPageSize = pageSize == null || pageSize <= 0 || pageSize > 1000 ? DEFAULT_PAGE_SIZE : pageSize;

        return PageRequest.of(queryPageNumber, queryPageSize, buildSort(sortBy));
    }

    // Natours-style sort string: "-price,averageRating" = price desc, then averageRating asc
    private Sort buildSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return DEFAULT_SORT;
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String token : sortBy.split(",")) {
            String field = token.trim();
            boolean descending = field.startsWith("-");
            if (descending) {
                field = field.substring(1);
            }
            // Ignore unknown fields so a bad query param can't blow up the query
            if (SORTABLE_FIELDS.contains(field)) {
                orders.add(descending ? Sort.Order.desc(field) : Sort.Order.asc(field));
            }
        }

        return orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders);
    }
}
