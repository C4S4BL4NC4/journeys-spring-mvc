package com.java.journeysspringmvc.service;

import com.java.journeysspringmvc.config.PasswordConfig;
import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserUpdateDTO;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserServiceImpl.class, PasswordConfig.class})
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

    @Autowired
    PasswordEncoder passwordEncoder;

    // The in-memory DB may already hold seeded users, so test users share a distinctive name to filter on
    private static final String TEST_NAME = "Zelda";

    private UserDTO newUser(String email) {
        return new UserDTO(null, TEST_NAME + " Test", email, null, Role.ADMIN,
                "password123", "password123", null, null);
    }

    private User saveUser(String name, String email, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .name(name)
                .email(email)
                .role(role)
                .password("{bcrypt}$2a$10$hashedvalue")
                .build());
    }

    @Test
    void savesNewUserWithHashedPasswordAndUserRole() {
        UserDTO saved = userService.saveNewUser(newUser("Zelda@Example.com"));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.role()).isEqualTo(Role.USER);
        assertThat(saved.email()).isEqualTo("zelda@example.com");
        assertThat(saved.photo()).isEqualTo(User.DEFAULT_PHOTO);
        assertThat(saved.password()).isNull();

        User stored = userRepository.findById(saved.id()).orElseThrow();
        assertThat(stored.getPassword()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("password123", stored.getPassword())).isTrue();
    }

    @Test
    void rejectsDuplicateEmail() {
        userService.saveNewUser(newUser("zelda@example.com"));

        assertThatThrownBy(() -> userService.saveNewUser(newUser("ZELDA@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listsActiveUsersFilteredByNameAndRole() {
        saveUser(TEST_NAME + " Guide", "z.guide@example.com", Role.GUIDE);
        saveUser(TEST_NAME + " User", "z.user@example.com", Role.USER);
        User inactive = saveUser(TEST_NAME + " Gone", "z.gone@example.com", Role.GUIDE);
        inactive.setActive(false);
        userRepository.saveAndFlush(inactive);

        assertThat(userService.listUsers(TEST_NAME.toLowerCase(), null, "name", null, null))
                .extracting(UserDTO::name)
                .containsExactly(TEST_NAME + " Guide", TEST_NAME + " User");
        assertThat(userService.listUsers(TEST_NAME, Role.GUIDE, null, null, null))
                .extracting(UserDTO::email)
                .containsExactly("z.guide@example.com");
    }

    @Test
    void getsActiveUserById() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);

        assertThat(userService.getUserById(user.getId())).get().extracting(UserDTO::email).isEqualTo("z@example.com");
        assertThat(userService.getUserById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void updatesProfileAndRoleButNotPassword() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);
        String passwordHash = user.getPassword();

        UserDTO updated = userService.updateUser(user.getId(),
                new UserUpdateDTO("Zelda Renamed", "Z.New@example.com", null, Role.GUIDE)).orElseThrow();

        assertThat(updated.name()).isEqualTo("Zelda Renamed");
        assertThat(updated.email()).isEqualTo("z.new@example.com");
        assertThat(updated.role()).isEqualTo(Role.GUIDE);
        assertThat(updated.photo()).isEqualTo(User.DEFAULT_PHOTO);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPassword()).isEqualTo(passwordHash);
    }

    @Test
    void returnsEmptyWhenUpdatingUnknownUser() {
        assertThat(userService.updateUser(UUID.randomUUID(),
                new UserUpdateDTO("Nobody", "nobody@example.com", null, Role.USER))).isEmpty();
    }

    @Test
    void deleteDeactivatesUser() {
        User user = saveUser(TEST_NAME, "z@example.com", Role.USER);

        assertThat(userService.deleteUser(user.getId())).isTrue();
        assertThat(userService.getUserById(user.getId())).isEmpty();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getActive()).isFalse();
        assertThat(userService.deleteUser(user.getId())).isFalse();
    }
}
