/*
 * REST endpoints for user profiles under /api/v1/users.
 *
 *   GET /api/v1/users           ADMIN; list users. Optional query params:
 *                               name, role, sort (e.g. "-createdAt"), page (1-based) and size
 *   GET /api/v1/users/{userId}  ADMIN; one user by their Keycloak id, or 404
 *   GET /api/v1/users/me        any logged-in user; their own profile
 *
 * Signing up, logging in, changing name/email/password and disabling accounts all happen in
 * Keycloak (registration page, account console at /realms/journeys/account, admin console),
 * so there are no endpoints for them here. "/me" takes precedence over the {userId} pattern.
 */
package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.exception.NotFoundException;
import com.java.journeysspringmvc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    public static final String USER_PATH = "/api/v1/users";
    public static final String USER_PATH_ID = USER_PATH + "/{userId}";
    public static final String ME_PATH = USER_PATH + "/me";

    private final UserService userService;

    @GetMapping(USER_PATH)
    public PagedModel<UserDTO> listUsers(@RequestParam(required = false) String name,
                                         @RequestParam(required = false) Role role,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return new PagedModel<>(userService.listUsers(name, role, sort, page, size));
    }

    @GetMapping(USER_PATH_ID)
    public UserDTO getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("No user found with id " + userId));
    }

    // The token's subject is the caller's Keycloak id; CurrentUserSyncFilter has already mirrored them
    @GetMapping(ME_PATH)
    public UserDTO getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUserById(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new NotFoundException("No profile found for the current user"));
    }
}
