/*
 * Tests UserServiceImpl against a real (in-memory) database, using @DataJpaTest plus the service.
 *
 * Other tests may have seeded users, so every test user is named "Zelda ..." and listings
 * filter on that name. Covers the profile sync from Keycloak claims (first-time creation,
 * picking up changed claims, skipping the write when nothing changed, fitting long names),
 * listing filtered by name/role, and lookups by id.
 */
package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserFilter;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.mapper.UserMapper;
import com.java.journeysspringmvc.repository.UserRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static final String TEST_NAME = "Zelda";

    private User saveUser(String name, String email, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .role(role)
                .build());
    }

    @Test
    void syncCreatesProfileOnFirstRequest() {
        UUID keycloakId = UUID.randomUUID();

        UserDTO synced = userService.syncUser(keycloakId, "Zelda Newcomer", "Zelda@Example.com", Role.USER);

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

        userService.syncUser(existing.getId(), "Zelda New", "zelda.new@example.com", Role.GUIDE);

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

        userService.syncUser(existing.getId(), "Zelda Same", "ZELDA.same@example.com", Role.GUIDE);

        assertThat(userRepository.findById(existing.getId()).orElseThrow().getVersion()).isEqualTo(versionBefore);
    }

    @Test
    void syncFitsNamesLongerThanTheColumn() {
        String longName = TEST_NAME + " " + "x".repeat(80);

        UserDTO synced = userService.syncUser(UUID.randomUUID(), longName, "zelda.long@example.com", Role.USER);

        assertThat(synced.name()).hasSize(50).startsWith(TEST_NAME);
    }

    @Test
    void listsUsersFilteredByNameAndRole() {
        saveUser(TEST_NAME + " Guide", "z.guide@example.com", Role.GUIDE);
        saveUser(TEST_NAME + " User", "z.user@example.com", Role.USER);

        assertThat(userService.listUsers(new UserFilter(TEST_NAME.toLowerCase(), null), PageRequest.of(0, 25, Sort.by("name"))))
                .extracting(UserDTO::name)
                .containsExactly(TEST_NAME + " Guide", TEST_NAME + " User");
        assertThat(userService.listUsers(new UserFilter(TEST_NAME, Role.GUIDE), PageRequest.of(0, 25)))
                .extracting(UserDTO::email)
                .containsExactly("z.guide@example.com");
    }

    @Test
    void getsUserById() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);

        assertThat(userService.getUserById(user.getId())).get().extracting(UserDTO::email).isEqualTo("z@example.com");
        assertThat(userService.getUserById(UUID.randomUUID())).isEmpty();
    }
}
