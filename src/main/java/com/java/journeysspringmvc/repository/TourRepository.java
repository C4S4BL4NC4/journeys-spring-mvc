/*
 * Database access for tours. Spring Data JPA implements this interface at runtime.
 *
 *   JpaRepository             gives the CRUD and paging methods (findById, save, deleteById, ...)
 *   JpaSpecificationExecutor  adds findAll(Specification, Pageable), which runs the dynamic
 *                             filter queries built from TourSpecifications
 */
package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID>, JpaSpecificationExecutor<Tour> {
}
