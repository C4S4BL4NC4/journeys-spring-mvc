/*
 * JPA entity for a tour; the central domain object of the app, mapped to the "tours" table.
 *
 * Besides basic columns (name, unique slug, duration, price, difficulty, ratings...) it holds:
 *   - images and startDates: simple value collections in their own tables
 *   - startLocation: an embedded Location stored in the tours table itself
 *   - locations: the ordered list of stops, in the tour_locations table
 *   - guides: a many-to-many link to User through the tour_guides join table
 *   - createdAt/lastModifiedAt: filled in automatically by Hibernate
 *   - version: optimistic locking, so two concurrent edits can't silently overwrite each other
 * Input is validated on TourDTO at the API boundary; the @Column settings here mirror the
 * database constraints (Flyway migrations), which are the last line of defence.
 * secretTour marks tours that only tour managers can see.
 */
package com.java.journeysspringmvc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@Entity
@Table(name = "tours")
@NoArgsConstructor
@AllArgsConstructor
public class Tour {
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(length = 36, columnDefinition = "varchar", updatable = false, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;
    @Column(length = 50, nullable = false)
    private String name;
    @Column(length = 60, nullable = false, unique = true)
    private String slug;

    // Length of the tour in days
    @Column(nullable = false)
    private Integer duration;
    @Column(nullable = false)
    private Integer maxGroupSize;
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Difficulty difficulty;
    private Double averageRating;
    @Builder.Default
    @Column(nullable = false)
    private Integer ratingQuantity = 0;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(precision = 10, scale = 2)
    private BigDecimal discount;
    @Column(nullable = false)
    private String summary;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false)
    private String imageCover;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "tour_images", joinColumns = @JoinColumn(name = "tour_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image", nullable = false)
    private List<String> images = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "tour_start_dates", joinColumns = @JoinColumn(name = "tour_id"))
    @OrderBy
    @Column(name = "start_date", nullable = false)
    private Set<LocalDateTime> startDates = new LinkedHashSet<>();

    // Hidden from public listings, like the secret tours in Natours
    @Builder.Default
    @Column(nullable = false)
    private Boolean secretTour = false;
    @Embedded
    @AttributeOverride(name = "longitude", column = @Column(name = "start_location_longitude"))
    @AttributeOverride(name = "latitude", column = @Column(name = "start_location_latitude"))
    @AttributeOverride(name = "address", column = @Column(name = "start_location_address"))
    @AttributeOverride(name = "description", column = @Column(name = "start_location_description"))
    @AttributeOverride(name = "day", column = @Column(name = "start_location_day_number"))
    private Location startLocation;
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "tour_locations", joinColumns = @JoinColumn(name = "tour_id"))
    @OrderColumn(name = "location_order")
    private List<Location> locations = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "tour_guides",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> guides = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastModifiedAt;

    @Version
    private Integer version;
}
