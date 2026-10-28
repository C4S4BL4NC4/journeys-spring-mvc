/*
 * The optional filters for listing users, bound by Spring MVC straight from query
 * parameters: GET /api/v1/users?name=jane&role=GUIDE. Filters left out don't apply.
 */
package com.java.journeysspringmvc.dto;

import com.java.journeysspringmvc.entity.Role;

public record UserFilter(
        String name,
        Role role
) {
}
