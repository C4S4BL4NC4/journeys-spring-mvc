package com.java.journeysspringmvc.repository;

import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    @Autowired
    TourRepository tourRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void savesTourWithGeneratedFields() {
        Tour saved = tourRepository.saveAndFlush(Tour.builder()
                .name("The Forest Hiker")
                .slug("the-forest-hiker")
                .price(new BigDecimal("397.00"))
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRatingQuantity()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void rejectsDuplicateSlug() {
        tourRepository.saveAndFlush(Tour.builder().name("A").slug("same").price(BigDecimal.TEN).build());

        assertThatThrownBy(() -> tourRepository.saveAndFlush(
                Tour.builder().name("B").slug("same").price(BigDecimal.TEN).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesUserWithDefaultRole() {
        User saved = userRepository.saveAndFlush(User.builder()
                .name("Jane")
                .email("jane@example.com")
                .password("$2a$10$hashedvalue")
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }
}
