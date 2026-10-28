/*
 * Database access for tours. Spring Data JPA implements this interface at runtime.
 *
 *   JpaRepository             gives the CRUD and paging methods (findById, save, deleteById, ...)
 *   JpaSpecificationExecutor  adds findAll(Specification, Pageable), which runs the dynamic
 *                             filter queries built from Specs
 *   findStatsByDifficulty     the Natours tour-stats aggregation, done as one JPQL GROUP BY
 *   findStartsBetween         every (tour name, departure) pair in a date range, which
 *                             TourServiceImpl groups into the monthly plan
 */
package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.dto.TourStatsDTO;
import com.java.journeysspringmvc.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TourRepository extends JpaRepository<Tour, UUID>, JpaSpecificationExecutor<Tour> {

    @Query("""
            select new com.java.journeysspringmvc.dto.TourStatsDTO(
                t.difficulty, count(t), sum(t.ratingQuantity), avg(t.averageRating),
                avg(t.price), min(t.price), max(t.price))
            from Tour t
            where t.secretTour = false and t.averageRating >= :minRating
            group by t.difficulty
            order by avg(t.price)""")
    List<TourStatsDTO> findStatsByDifficulty(double minRating);

    @Query("""
            select t.name as name, d as startDate
            from Tour t join t.startDates d
            where t.secretTour = false and d >= :from and d < :to""")
    List<TourStart> findStartsBetween(LocalDateTime from, LocalDateTime to);

    interface TourStart {
        String getName();

        LocalDateTime getStartDate();
    }
}
