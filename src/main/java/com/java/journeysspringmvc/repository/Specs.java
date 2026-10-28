/*
 * Reusable, null-tolerant query filters ("Specifications") for any entity.
 *
 * Each method builds one WHERE condition on the named attribute, or no condition at all when
 * the filter value is absent. So a service combines every
 * optional filter with Specification.allOf(...) and only the ones actually given apply:
 *   containsIgnoreCase  case-insensitive substring match; % and _ in the input are matched
 *                       literally, so searching for "%" doesn't match everything
 *   equalTo             exact match
 *   atLeast / atMost    >= / <= on any comparable attribute (numbers, dates...)
 */
package com.java.journeysspringmvc.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class Specs {

    private static final char LIKE_ESCAPE = '\\';

    private Specs() {
    }

    // Absent filters still return a Specification, just one whose predicate is null:
    // Specification.and/allOf reject null specifications but skip null predicates

    public static <T> Specification<T> containsIgnoreCase(String attribute, String value) {
        String pattern = StringUtils.hasText(value) ? "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%" : null;
        return (root, query, cb) -> pattern == null ? null : cb.like(cb.lower(root.get(attribute)), pattern, LIKE_ESCAPE);
    }

    public static <T> Specification<T> equalTo(String attribute, Object value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(attribute), value);
    }

    public static <T, V extends Comparable<? super V>> Specification<T> atLeast(String attribute, V value) {
        return (root, query, cb) -> value == null ? null : cb.greaterThanOrEqualTo(root.get(attribute), value);
    }

    public static <T, V extends Comparable<? super V>> Specification<T> atMost(String attribute, V value) {
        return (root, query, cb) -> value == null ? null : cb.lessThanOrEqualTo(root.get(attribute), value);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
