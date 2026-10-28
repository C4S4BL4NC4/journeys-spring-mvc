/*
 * User profile logic: the implementation behind UserService.
 *
 *   listUsers      active users filtered by name/role (via Specs), paged and sorted by the
 *                  client's Pageable after checking its sort fields against SORTABLE_FIELDS
 *   getUserById    one active profile by its Keycloak user id
 *   syncUser       "just-in-time provisioning": creates the local profile the first time a
 *                  Keycloak user calls the API, and afterwards updates name, email and role
 *                  whenever they changed in Keycloak. Unchanged profiles aren't written, so
 *                  the per-request sync usually costs a single primary-key read. For a
 *                  deactivated profile it reports "not allowed" unless the token was issued
 *                  after the deactivation (Keycloak only issues tokens to enabled accounts,
 *                  so that means an admin re-enabled it), in which case it reactivates.
 *   changeRole     assigns the role in Keycloak (KeycloakAdminClient), then mirrors it
 *   deactivateUser disables the Keycloak account, then hides the profile; the row is kept
 *                  so tours that list the user as a guide stay intact (Natours' deleteMe)
 *   updatePhoto    stores the file name produced by ImageService
 * Keycloak is called before the local change, so a Keycloak failure leaves both untouched.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserFilter;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.UserMapper;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.security.KeycloakAdminClient;
import com.java.journeysspringmvc.util.Pageables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.java.journeysspringmvc.repository.Specs.containsIgnoreCase;
import static com.java.journeysspringmvc.repository.Specs.equalTo;
import static com.java.journeysspringmvc.repository.Specs.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final static Set<String> SORTABLE_FIELDS = Set.of("name", "email", "role", "createdAt");
    private final static int MAX_NAME_LENGTH = 50;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakAdminClient keycloakAdminClient;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(UserFilter filter, Pageable pageable) {
        Specification<User> spec = Specification.allOf(
                isNull("deactivatedAt"),
                containsIgnoreCase("name", filter.name()),
                equalTo("role", filter.role()));

        return userRepository.findAll(spec, Pageables.requireSortable(pageable, SORTABLE_FIELDS))
                .map(userMapper::userToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(UUID id) {
        return findActive(id).map(userMapper::userToUserDTO);
    }

    @Override
    @Transactional
    public Optional<UserDTO> syncUser(UUID id, String name, String email, Role role, Instant tokenIssuedAt) {
        // Keycloak allows longer names than the users table does
        String fittedName = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findById(id).orElseGet(() -> User.builder().id(id).build());
        boolean isNew = user.getVersion() == null;
        boolean reactivated = false;
        if (!user.isActive()) {
            if (tokenIssuedAt == null || !tokenIssuedAt.isAfter(user.getDeactivatedAt())) {
                return Optional.empty();
            }
            user.setDeactivatedAt(null);
            reactivated = true;
        }
        if (!isNew && !reactivated && Objects.equals(user.getName(), fittedName)
                && Objects.equals(user.getEmail(), normalizedEmail) && user.getRole() == role) {
            return Optional.of(userMapper.userToUserDTO(user));
        }

        user.setName(fittedName);
        user.setEmail(normalizedEmail);
        user.setRole(role);
        if (isNew) {
            log.info("Provisioning local profile for Keycloak user {}", id);
        }
        return Optional.of(userMapper.userToUserDTO(userRepository.saveAndFlush(user)));
    }

    @Override
    @Transactional
    public Optional<UserDTO> changeRole(UUID id, Role role) {
        return findActive(id).map(user -> {
            keycloakAdminClient.setRole(id, role);
            user.setRole(role);
            return userMapper.userToUserDTO(userRepository.saveAndFlush(user));
        });
    }

    @Override
    @Transactional
    public boolean deactivateUser(UUID id) {
        return findActive(id).map(user -> {
            keycloakAdminClient.disableUser(id);
            user.setDeactivatedAt(Instant.now());
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public Optional<UserDTO> updatePhoto(UUID id, String photo) {
        return findActive(id).map(user -> {
            user.setPhoto(photo);
            return userMapper.userToUserDTO(userRepository.saveAndFlush(user));
        });
    }

    private Optional<User> findActive(UUID id) {
        return userRepository.findById(id).filter(User::isActive);
    }
}
