package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

// Each spec returns null when its filter is absent, which Spring Data treats as "no condition"
public final class UserSpecifications {

    private UserSpecifications() {
    }

    // Deactivated accounts are hidden from every lookup, like Natours' /^find/ query middleware
    public static Specification<User> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<User> nameContains(String name) {
        return (root, query, cb) -> StringUtils.hasText(name)
                ? cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%")
                : null;
    }

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }
}
