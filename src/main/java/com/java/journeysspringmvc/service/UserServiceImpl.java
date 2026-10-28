/*
 * User profile logic: the implementation behind UserService.
 *
 *   listUsers    filtered by name/role, sorted and paged via PageRequests
 *   getUserById  one profile by its Keycloak user id
 *   syncUser     "just-in-time provisioning": creates the local profile the first time a
 *                Keycloak user calls the API, and afterwards updates name, email and role
 *                whenever they changed in Keycloak. Unchanged profiles aren't written, so
 *                the per-request sync usually costs a single primary-key read.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.UserMapper;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.util.PageRequests;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.java.journeysspringmvc.repository.UserSpecifications.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final static Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));
    private final static Set<String> SORTABLE_FIELDS = Set.of("name", "email", "role", "createdAt");
    private final static int MAX_NAME_LENGTH = 50;

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(String name, Role role, String sortBy, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = PageRequests.of(sortBy, pageNumber, pageSize, SORTABLE_FIELDS, DEFAULT_SORT);

        Specification<User> spec = Specification.allOf(
                nameContains(name),
                hasRole(role));

        return userRepository.findAll(spec, pageRequest).map(userMapper::userToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(UUID id) {
        return userRepository.findById(id).map(userMapper::userToUserDTO);
    }

    @Override
    @Transactional
    public UserDTO syncUser(UUID id, String name, String email, Role role) {
        // Keycloak allows longer names than the users table does
        String fittedName = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findById(id).orElseGet(() -> User.builder().id(id).build());
        boolean isNew = user.getVersion() == null;
        if (!isNew && Objects.equals(user.getName(), fittedName)
                && Objects.equals(user.getEmail(), normalizedEmail) && user.getRole() == role) {
            return userMapper.userToUserDTO(user);
        }

        user.setName(fittedName);
        user.setEmail(normalizedEmail);
        user.setRole(role);
        if (isNew) {
            log.info("Provisioning local profile for Keycloak user {}", id);
        }
        return userMapper.userToUserDTO(userRepository.saveAndFlush(user));
    }
}
