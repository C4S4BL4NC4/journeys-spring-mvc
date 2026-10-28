/*
 * The contract for user business logic, used by UserController.
 *
 * Works with UserDTO / UserUpdateDTO, never entities. As with TourService, "not found" is
 * Optional.empty() or false rather than an exception.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserUpdateDTO;
import com.java.journeysspringmvc.entity.Role;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Page<UserDTO> listUsers(String name, Role role, String sortBy, Integer pageNumber, Integer pageSize);

    Optional<UserDTO> getUserById(UUID id);

    UserDTO saveNewUser(UserDTO userDTO);

    Optional<UserDTO> updateUser(UUID id, UserUpdateDTO userUpdateDTO);

    boolean deleteUser(UUID id);
}
