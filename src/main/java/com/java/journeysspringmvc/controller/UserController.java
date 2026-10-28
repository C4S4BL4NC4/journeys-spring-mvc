/*
 * REST endpoints for users under /api/v1/users.
 *
 * Admin only (SecurityConfig requires ADMIN):
 *   GET    /api/v1/users            list active users. Optional query params:
 *                                  filters  name, role (see UserFilter; ?role=lead-guide works)
 *                                  paging   page (0-based), size, sort=field[,asc|desc]; newest first by default
 *   GET    /api/v1/users/{userId}   one active user by their Keycloak id, or 404
 *   PATCH  /api/v1/users/{userId}   change a user's role, e.g. {"role": "guide"} (applied in Keycloak)
 *   DELETE /api/v1/users/{userId}   deactivate the account (disabled in Keycloak); 204, or 404
 *
 * Any logged-in user, acting on themselves (the token's subject is their Keycloak id):
 *   GET    /api/v1/users/me         their profile
 *   PUT    /api/v1/users/me/photo   multipart upload of "photo"; resized to 500x500 by ImageService
 *   DELETE /api/v1/users/me         deactivate their own account (Natours' deleteMe); 204
 *
 * Signing up, logging in and changing name/email/password happen in Keycloak (registration
 * page, account console at /realms/journeys/account), so there are no endpoints for them here.
 * The literal "/me" paths take precedence over the {userId} pattern.
 */
package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserFilter;
import com.java.journeysspringmvc.dto.UserUpdateDTO;
import com.java.journeysspringmvc.exception.NotFoundException;
import com.java.journeysspringmvc.service.UserService;
import com.java.journeysspringmvc.storage.ImageService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    public static final String USER_PATH = "/api/v1/users";
    public static final String USER_PATH_ID = USER_PATH + "/{userId}";
    public static final String ME_PATH = USER_PATH + "/me";
    public static final String ME_PHOTO_PATH = ME_PATH + "/photo";

    private final UserService userService;
    private final ImageService imageService;

    @GetMapping(USER_PATH)
    public Page<UserDTO> listUsers(@ParameterObject UserFilter filter,
                                   @ParameterObject @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return userService.listUsers(filter, pageable);
    }

    @GetMapping(USER_PATH_ID)
    public UserDTO getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId).orElseThrow(() -> userNotFound(userId));
    }

    @PatchMapping(USER_PATH_ID)
    public UserDTO updateUser(@PathVariable UUID userId, @Validated @RequestBody UserUpdateDTO userUpdateDTO) {
        return userService.changeRole(userId, userUpdateDTO.role()).orElseThrow(() -> userNotFound(userId));
    }

    @DeleteMapping(USER_PATH_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        if (!userService.deactivateUser(userId)) {
            throw userNotFound(userId);
        }
    }

    // CurrentUserSyncFilter has already mirrored the caller, so their profile exists here
    @GetMapping(ME_PATH)
    public UserDTO getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUserById(currentUserId(jwt)).orElseThrow(UserController::profileNotFound);
    }

    @PutMapping(path = ME_PHOTO_PATH, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDTO uploadMyPhoto(@AuthenticationPrincipal Jwt jwt, @RequestPart MultipartFile photo) {
        UUID userId = currentUserId(jwt);
        String fileName = imageService.saveUserPhoto(userId, photo);
        return userService.updatePhoto(userId, fileName).orElseThrow(UserController::profileNotFound);
    }

    @DeleteMapping(ME_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal Jwt jwt) {
        if (!userService.deactivateUser(currentUserId(jwt))) {
            throw profileNotFound();
        }
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static NotFoundException userNotFound(UUID userId) {
        return new NotFoundException("No user found with id " + userId);
    }

    private static NotFoundException profileNotFound() {
        return new NotFoundException("No profile found for the current user");
    }
}
