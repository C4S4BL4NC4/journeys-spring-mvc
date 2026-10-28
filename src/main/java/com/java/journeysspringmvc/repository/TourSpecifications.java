package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Tour;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

// Each spec returns null when its filter is absent, which Spring Data treats as "no condition"
public final class TourSpecifications {

    private TourSpecifications() {
    }

    public static Specification<Tour> nameContains(String name) {
        return (root, query, cb) -> StringUtils.hasText(name)
                ? cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%")
                : null;
    }

    public static Specification<Tour> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> difficulty == null ? null : cb.equal(root.get("difficulty"), difficulty);
    }

    public static <T extends Comparable<? super T>> Specification<Tour> atLeast(String field, T value) {
        return (root, query, cb) -> value == null ? null : cb.greaterThanOrEqualTo(root.get(field), value);
    }

    public static <T extends Comparable<? super T>> Specification<Tour> atMost(String field, T value) {
        return (root, query, cb) -> value == null ? null : cb.lessThanOrEqualTo(root.get(field), value);
    }
}
