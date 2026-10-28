package com.java.journeysspringmvc.bootstrap;

import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.entity.Tour;
import com.java.journeysspringmvc.entity.User;
import com.java.journeysspringmvc.repository.TourRepository;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.util.FormatUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    // Development-only seed password shared by every bootstrapped user
    private static final String SEED_PASSWORD = "password123";

    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        loadTours();
        loadUsers();
        greeter();
    }

    private void loadTours() {
        if (tourRepository.count() > 0) {
            return;
        }

        tourRepository.saveAll(List.of(
                tour("The Forest Hiker", "the-forest-hiker", 4.7, 37, "397.00", null),
                tour("The Sea Explorer", "the-sea-explorer", 4.8, 23, "497.00", "50.00"),
                tour("The Snow Adventurer", "the-snow-adventurer", 4.5, 15, "997.00", null),
                tour("The City Wanderer", "the-city-wanderer", 4.6, 41, "1197.00", "100.00"),
                tour("The Park Camper", "the-park-camper", null, 0, "1497.00", null),
                tour("The Desert Nomad", "the-desert-nomad", 4.9, 8, "1597.00", "200.00")
        ));
    }

    private void loadUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        String hashedPassword = passwordEncoder.encode(SEED_PASSWORD);

        userRepository.saveAll(List.of(
                user("Admin", "admin@journeys.dev", Role.ADMIN, hashedPassword),
                user("Lena Lead", "lead.guide@journeys.dev", Role.LEAD_GUIDE, hashedPassword),
                user("Gus Guide", "guide@journeys.dev", Role.GUIDE, hashedPassword),
                user("Jane Doe", "jane@journeys.dev", Role.USER, hashedPassword),
                user("John Smith", "john@journeys.dev", Role.USER, hashedPassword)
        ));
    }

    private Tour tour(String name, String slug, Double averageRating, int ratingQuantity,
                      String price, String discount) {
        return Tour.builder()
                .name(name)
                .slug(slug)
                .averageRating(averageRating)
                .ratingQuantity(ratingQuantity)
                .price(new BigDecimal(price))
                .discount(discount == null ? null : new BigDecimal(discount))
                .build();
    }

    private User user(String name, String email, Role role, String hashedPassword) {
        return User.builder()
                .name(name)
                .email(email)
                .role(role)
                .password(hashedPassword)
                .build();
    }

    private void greeter() {
        System.out.println("=".repeat(50));
        System.out.println("Hello World!");
        System.out.println(FormatUrl.of("http://localhost:8080/swagger-ui.html"));
        System.out.println("=".repeat(50));
    }
}
