package com.java.journeysspringmvc.bootstrap;

import com.java.journeysspringmvc.entity.*;
import com.java.journeysspringmvc.repository.TourRepository;
import com.java.journeysspringmvc.repository.UserRepository;
import com.java.journeysspringmvc.util.FormatUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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
        // Users first, so tours can be assigned guides
        loadUsers();
        loadTours();
        greeter();
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

    private void loadTours() {
        if (tourRepository.count() > 0) {
            return;
        }

        Set<User> guides = new HashSet<>(userRepository.findAllByRoleIn(List.of(Role.LEAD_GUIDE, Role.GUIDE)));
        int year = LocalDateTime.now().getYear() + 1;

        tourRepository.saveAll(List.of(
                tour("The Forest Hiker", "the-forest-hiker", 5, 25, Difficulty.EASY, 4.7, 37, "397.00", null,
                        "Breathtaking hike through the Canadian Banff National Park",
                        location(-115.570154, 51.178456, "224 Banff Ave, Banff, AB, Canada", "Banff, CAN"),
                        List.of(stop(-116.214531, 51.417611, "Banff National Park", 1),
                                stop(-118.076152, 52.875223, "Jasper National Park", 3)),
                        List.of(start(year, 4, 25), start(year, 7, 20), start(year, 10, 5)),
                        false, guides),
                tour("The Sea Explorer", "the-sea-explorer", 7, 15, Difficulty.MEDIUM, 4.8, 23, "497.00", "50.00",
                        "Exploring the jaw-dropping US east coast by foot and by boat",
                        location(-80.185942, 25.774772, "301 Biscayne Blvd, Miami, FL, USA", "Miami, USA"),
                        List.of(stop(-80.128473, 25.781842, "Lummus Park Beach", 1),
                                stop(-80.647885, 24.909047, "Islamorada", 2),
                                stop(-81.78639, 24.558502, "Key West", 5)),
                        List.of(start(year, 6, 19), start(year, 7, 20), start(year, 8, 18)),
                        false, guides),
                tour("The Snow Adventurer", "the-snow-adventurer", 4, 10, Difficulty.DIFFICULT, 4.5, 15, "997.00", null,
                        "Exciting adventure in the snow with snowboarding and skiing",
                        location(-106.822318, 39.190872, "419 S Mill St, Aspen, CO, USA", "Aspen, USA"),
                        List.of(stop(-106.11298, 39.479774, "Copper Mountain", 1),
                                stop(-106.837292, 39.192395, "Aspen Highlands", 3)),
                        List.of(start(year, 1, 5), start(year, 2, 12)),
                        false, guides),
                tour("The City Wanderer", "the-city-wanderer", 9, 20, Difficulty.EASY, 4.6, 41, "1197.00", "100.00",
                        "Living the life of Wanderlust in the US' most beautiful cities",
                        location(-73.985141, 40.75894, "Manhattan, NY, USA", "NYC, USA"),
                        List.of(stop(-73.967696, 40.781821, "New York", 1),
                                stop(-118.324396, 34.097984, "Los Angeles", 3),
                                stop(-122.408865, 37.787825, "San Francisco", 5)),
                        List.of(start(year, 3, 11), start(year, 5, 2)),
                        false, guides),
                tour("The Park Camper", "the-park-camper", 10, 15, Difficulty.MEDIUM, null, 0, "1497.00", null,
                        "Breathing in Nature in America's most spectacular National Parks",
                        location(-115.172652, 36.110904, "3663 S Las Vegas Blvd, Las Vegas, NV, USA", "Las Vegas, USA"),
                        List.of(stop(-112.987418, 37.198125, "Zion Canyon National Park", 1),
                                stop(-111.376161, 36.86438, "Antelope Canyon", 4),
                                stop(-112.115763, 36.058973, "Grand Canyon National Park", 5)),
                        List.of(start(year, 8, 5), start(year, 9, 12)),
                        false, guides),
                tour("The Desert Nomad", "the-desert-nomad", 6, 8, Difficulty.DIFFICULT, 4.9, 8, "1597.00", "200.00",
                        "An invitation-only crossing of the Wadi Rum desert by camel",
                        location(35.420415, 29.574893, "Wadi Rum Village, Aqaba, Jordan", "Wadi Rum, JOR"),
                        List.of(stop(35.443058, 29.531722, "Lawrence's Spring", 2),
                                stop(35.393318, 29.438712, "Burdah Rock Bridge", 4)),
                        List.of(start(year, 11, 3)),
                        true, guides)
        ));
    }

    private Tour tour(String name, String slug, int duration, int maxGroupSize, Difficulty difficulty,
                      Double averageRating, int ratingQuantity, String price, String discount, String summary,
                      Location startLocation, List<Location> locations, List<LocalDateTime> startDates,
                      boolean secretTour, Set<User> guides) {
        return Tour.builder()
                .name(name)
                .slug(slug)
                .duration(duration)
                .maxGroupSize(maxGroupSize)
                .difficulty(difficulty)
                .averageRating(averageRating)
                .ratingQuantity(ratingQuantity)
                .price(new BigDecimal(price))
                .discount(discount == null ? null : new BigDecimal(discount))
                .summary(summary)
                .description(summary + ". A " + duration + "-day journey for groups of up to " + maxGroupSize + ".")
                .imageCover(slug + "-cover.jpg")
                .images(new ArrayList<>(List.of(slug + "-1.jpg", slug + "-2.jpg", slug + "-3.jpg")))
                .startDates(new LinkedHashSet<>(startDates))
                .secretTour(secretTour)
                .startLocation(startLocation)
                .locations(new ArrayList<>(locations))
                .guides(new HashSet<>(guides))
                .build();
    }

    private Location location(double longitude, double latitude, String address, String description) {
        return Location.builder()
                .longitude(longitude)
                .latitude(latitude)
                .address(address)
                .description(description)
                .build();
    }

    private Location stop(double longitude, double latitude, String description, int day) {
        return Location.builder()
                .longitude(longitude)
                .latitude(latitude)
                .description(description)
                .day(day)
                .build();
    }

    private LocalDateTime start(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 9, 0);
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
