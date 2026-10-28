/*
 * Tests UserServiceImpl against a real (in-memory) database, using @DataJpaTest plus the service.
 * KeycloakAdminClient is a Mockito mock: these tests check the local side and that Keycloak
 * is asked to make each change (KeycloakIntegrationTest covers the real Keycloak calls).
 *
 * Other tests may have seeded users, so every test user is named "Zelda ..." and listings
 * filter on that name. Covers the profile sync from Keycloak claims (first-time creation,
 * picking up changed claims, skipping the write when nothing changed, fitting long names,
 * refusing deactivated accounts and reactivating them on a newer token), listing filtered by
 * name/role with deactivated users hidden, lookups, role changes, deactivation and photos.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserFilter;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.UserMapper;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.security.KeycloakAdminClient;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserServiceImpl.class)
class UserServiceImplTest {

    // Built via MapStruct's factory so the test doesn't reference the generated UserMapperImpl directly
    @TestConfiguration
    static class MapperConfig {
        @Bean
        UserMapper userMapper() {
            return Mappers.getMapper(UserMapper.class);
        }
    }

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    KeycloakAdminClient keycloakAdminClient;

    private static final String TEST_NAME = "Zelda";

    private User saveUser(String name, String email, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .role(role)
                .build());
    }

    private User deactivatedUser(Instant deactivatedAt) {
        User user = saveUser(TEST_NAME + " Gone", "zelda.gone@example.com", Role.USER);
        user.setDeactivatedAt(deactivatedAt);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void syncCreatesProfileOnFirstRequest() {
        UUID keycloakId = UUID.randomUUID();

        UserDTO synced = userService.syncUser(keycloakId, "Zelda Newcomer", "Zelda@Example.com", Role.USER, Instant.now())
                .orElseThrow();

        assertThat(synced.id()).isEqualTo(keycloakId);
        assertThat(synced.email()).isEqualTo("zelda@example.com");
        assertThat(synced.photo()).isEqualTo(User.DEFAULT_PHOTO);
        assertThat(userRepository.findById(keycloakId)).get()
                .extracting(User::getName, User::getRole)
                .containsExactly("Zelda Newcomer", Role.USER);
    }

    @Test
    void syncPicksUpChangesMadeInKeycloak() {
        User existing = saveUser("Zelda Old", "zelda.old@example.com", Role.USER);
        int versionBefore = existing.getVersion();

        userService.syncUser(existing.getId(), "Zelda New", "zelda.new@example.com", Role.GUIDE, Instant.now());

        User updated = userRepository.findById(existing.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Zelda New");
        assertThat(updated.getEmail()).isEqualTo("zelda.new@example.com");
        assertThat(updated.getRole()).isEqualTo(Role.GUIDE);
        assertThat(updated.getVersion()).isEqualTo(versionBefore + 1);
    }

    @Test
    void syncSkipsWriteWhenNothingChanged() {
        User existing = saveUser("Zelda Same", "zelda.same@example.com", Role.GUIDE);
        int versionBefore = existing.getVersion();

        userService.syncUser(existing.getId(), "Zelda Same", "ZELDA.same@example.com", Role.GUIDE, Instant.now());

        assertThat(userRepository.findById(existing.getId()).orElseThrow().getVersion()).isEqualTo(versionBefore);
    }

    @Test
    void syncFitsNamesLongerThanTheColumn() {
        String longName = TEST_NAME + " " + "x".repeat(80);

        UserDTO synced = userService.syncUser(UUID.randomUUID(), longName, "zelda.long@example.com", Role.USER, Instant.now())
                .orElseThrow();

        assertThat(synced.name()).hasSize(50).startsWith(TEST_NAME);
    }

    @Test
    void syncRefusesTokensIssuedBeforeDeactivation() {
        Instant deactivatedAt = Instant.now();
        User user = deactivatedUser(deactivatedAt);

        assertThat(userService.syncUser(user.getId(), user.getName(), user.getEmail(), Role.USER,
                deactivatedAt.minusSeconds(60))).isEmpty();
        assertThat(userRepository.findById(user.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void syncReactivatesWhenTokenIsNewerThanDeactivation() {
        Instant deactivatedAt = Instant.now().minusSeconds(3600);
        User user = deactivatedUser(deactivatedAt);

        assertThat(userService.syncUser(user.getId(), user.getName(), user.getEmail(), Role.USER, Instant.now())).isPresent();
        assertThat(userRepository.findById(user.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void listsActiveUsersFilteredByNameAndRole() {
        saveUser(TEST_NAME + " Guide", "z.guide@example.com", Role.GUIDE);
        saveUser(TEST_NAME + " User", "z.user@example.com", Role.USER);
        deactivatedUser(Instant.now());

        assertThat(userService.listUsers(new UserFilter(TEST_NAME.toLowerCase(), null), PageRequest.of(0, 25, Sort.by("name"))))
                .extracting(UserDTO::name)
                .containsExactly(TEST_NAME + " Guide", TEST_NAME + " User");
        assertThat(userService.listUsers(new UserFilter(TEST_NAME, Role.GUIDE), PageRequest.of(0, 25)))
                .extracting(UserDTO::email)
                .containsExactly("z.guide@example.com");
    }

    @Test
    void getsActiveUsersById() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);

        assertThat(userService.getUserById(user.getId())).get().extracting(UserDTO::email).isEqualTo("z@example.com");
        assertThat(userService.getUserById(UUID.randomUUID())).isEmpty();
        assertThat(userService.getUserById(deactivatedUser(Instant.now()).getId())).isEmpty();
    }

    @Test
    void changesRoleInKeycloakAndLocally() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);

        UserDTO updated = userService.changeRole(user.getId(), Role.LEAD_GUIDE).orElseThrow();

        verify(keycloakAdminClient).setRole(user.getId(), Role.LEAD_GUIDE);
        assertThat(updated.role()).isEqualTo(Role.LEAD_GUIDE);
        assertThat(userService.changeRole(UUID.randomUUID(), Role.GUIDE)).isEmpty();
    }

    @Test
    void deactivatesInKeycloakAndHidesProfile() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);

        assertThat(userService.deactivateUser(user.getId())).isTrue();

        verify(keycloakAdminClient).disableUser(user.getId());
        assertThat(userRepository.findById(user.getId()).orElseThrow().getDeactivatedAt()).isNotNull();
        assertThat(userService.getUserById(user.getId())).isEmpty();
        assertThat(userService.deactivateUser(user.getId())).isFalse();
    }

    @Test
    void leavesKeycloakAloneForUnknownUsers() {
        assertThat(userService.deactivateUser(UUID.randomUUID())).isFalse();

        verify(keycloakAdminClient, never()).disableUser(any());
    }

    @Test
    void updatesPhoto() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);

        assertThat(userService.updatePhoto(user.getId(), "user-x.jpeg")).get()
                .extracting(UserDTO::photo).isEqualTo("user-x.jpeg");
    }
}
