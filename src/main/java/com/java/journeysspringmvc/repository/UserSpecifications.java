/*
 * Reusable query filters for users, combined by UserServiceImpl.listUsers.
 *
 * Same pattern as TourSpecifications: each filter returns null when unused so it drops out.
 *   nameContains  case-insensitive substring match on the name
 *   hasRole       exact role
 */
package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

// Each spec returns null when its filter is absent, which Spring Data treats as "no condition"
public final class UserSpecifications {

    private UserSpecifications() {
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
