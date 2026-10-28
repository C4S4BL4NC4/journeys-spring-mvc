/*
 * REST endpoints for users under /api/v1/users. All of them require authentication.
 *
 *   GET    /api/v1/users            list active users; optional query params:
 *                                   name, role, sort (e.g. "-createdAt"), page (1-based) and size
 *   GET    /api/v1/users/{userId}   one active user, or 404
 *   POST   /api/v1/users            create a user (UserDTO, with password + passwordConfirm); 201
 *   PUT    /api/v1/users/{userId}   admin update of name, email, photo and role (UserUpdateDTO), or 404
 *   DELETE /api/v1/users/{userId}   deactivate the user; 204, or 404
 *
 * Like TourController it only maps HTTP to UserService calls. Passwords are write-only in
 * the DTO, so they are accepted in requests but never appear in responses.
 */
package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserUpdateDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.exception.NotFoundException;
import com.java.journeysspringmvc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    public static final String USER_PATH = "/api/v1/users";
    public static final String USER_PATH_ID = USER_PATH + "/{userId}";

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
        return userService.getUserById(userId).orElseThrow(() -> userNotFound(userId));
    }

    @PostMapping(USER_PATH)
    public ResponseEntity<UserDTO> createUser(@Validated @RequestBody UserDTO userDTO, UriComponentsBuilder uriBuilder) {
        UserDTO saved = userService.saveNewUser(userDTO);

        return ResponseEntity.created(uriBuilder.path(USER_PATH_ID).buildAndExpand(saved.id()).toUri()).body(saved);
    }

    @PutMapping(USER_PATH_ID)
    public UserDTO updateUser(@PathVariable UUID userId, @Validated @RequestBody UserUpdateDTO userUpdateDTO) {
        return userService.updateUser(userId, userUpdateDTO).orElseThrow(() -> userNotFound(userId));
    }

    @DeleteMapping(USER_PATH_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        if (!userService.deleteUser(userId)) {
            throw userNotFound(userId);
        }
    }

    private static NotFoundException userNotFound(UUID userId) {
        return new NotFoundException("No user found with id " + userId);
    }
}
