/*
 * Web-layer tests for TourController using @WebMvcTest.
 *
 * Only the MVC layer starts (controller, JSON conversion, validation, exception handler and
 * the imported SecurityConfig); TourService is replaced with a Mockito mock, so no database
 * is involved. Requests carry mock JWTs with a chosen role instead of real tokens.
 * Requests go through MockMvc and the tests check:
 *   - query parameters are passed to the service correctly, and the paged JSON shape
 *   - status codes: 200, 201 + Location header, 204, 400 (invalid body, malformed id,
 *     business rule), 404 (unknown tour) and 409 (duplicate slug)
 *   - reading tours is public; writing without a token gives 401, and with a token whose
 *     role isn't ADMIN or LEAD_GUIDE gives 403
 */
package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.config.SecurityConfig;
import com.java.journeysspringmvc.dto.TourDTO;
import com.java.journeysspringmvc.entity.Difficulty;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.service.TourService;
import com.java.journeysspringmvc.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TourController.class)
@Import(SecurityConfig.class)
class TourControllerTest {

    private static final String VALID_TOUR = """
            {
              "name": "The Forest Hiker",
              "duration": 5,
              "maxGroupSize": 25,
              "difficulty": "EASY",
              "price": 397.00,
              "summary": "Breathtaking hike",
              "imageCover": "cover.jpg"
            }
            """;

    @Autowired
    MockMvc mockMvc;

    // Required by SecurityConfig's profile sync filter; mock JWTs carry no email, so it's never called
    @MockitoBean
    UserService userService;

    @MockitoBean
    TourService tourService;

    private TourDTO tourDTO(UUID id) {
        return new TourDTO(id, "The Forest Hiker", "the-forest-hiker", 5, 25, Difficulty.EASY, 4.7, 37,
                new BigDecimal("397.00"), null, "Breathtaking hike", null, "cover.jpg",
                List.of(), List.of(), false, null, List.of(), null, null, null);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return asRole(request, Role.ADMIN);
    }

    // Stands in for a verified Keycloak token with the given role; real tokens are tested in KeycloakIntegrationTest
    private MockHttpServletRequestBuilder asRole(MockHttpServletRequestBuilder request, Role role) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role.name())))
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void listsToursPubliclyWithFiltersSortAndPaging() throws Exception {
        given(tourService.listTours(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(tourDTO(UUID.randomUUID()))));

        mockMvc.perform(get(TourController.TOUR_PATH)
                        .param("name", "forest")
                        .param("difficulty", "EASY")
                        .param("minPrice", "100")
                        .param("maxGroupSize", "30")
                        .param("sort", "-price")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("The Forest Hiker"))
                .andExpect(jsonPath("$.content[0].durationWeeks").exists())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(tourService).listTours("forest", Difficulty.EASY, new BigDecimal("100"), null,
                null, null, null, 30, "-price", 2, 5);
    }

    @Test
    void getsTourById() throws Exception {
        UUID id = UUID.randomUUID();
        given(tourService.getTourById(id)).willReturn(Optional.of(tourDTO(id)));

        mockMvc.perform(get(TourController.TOUR_PATH_ID, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.slug").value("the-forest-hiker"));
    }

    @Test
    void returnsNotFoundForUnknownTour() throws Exception {
        UUID id = UUID.randomUUID();
        given(tourService.getTourById(id)).willReturn(Optional.empty());

        mockMvc.perform(get(TourController.TOUR_PATH_ID, id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No tour found with id " + id));
    }

    @Test
    void returnsBadRequestForMalformedId() throws Exception {
        mockMvc.perform(get(TourController.TOUR_PATH_ID, "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsTour() throws Exception {
        UUID id = UUID.randomUUID();
        given(tourService.saveNewTour(any())).willReturn(tourDTO(id));

        mockMvc.perform(asAdmin(post(TourController.TOUR_PATH)).content(VALID_TOUR))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/tours/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void requiresAuthenticationToCreate() throws Exception {
        mockMvc.perform(post(TourController.TOUR_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_TOUR))
                .andExpect(status().isUnauthorized());

        verify(tourService, never()).saveNewTour(any());
    }

    @Test
    void rejectsInvalidTour() throws Exception {
        mockMvc.perform(asAdmin(post(TourController.TOUR_PATH)).content("""
                        { "name": "", "duration": -1 }
                        """))
                .andExpect(status().isBadRequest());

        verify(tourService, never()).saveNewTour(any());
    }

    @Test
    void mapsBusinessRuleViolationsToBadRequest() throws Exception {
        given(tourService.saveNewTour(any())).willThrow(new IllegalArgumentException("guideIds must all reference existing guides or lead guides"));

        mockMvc.perform(asAdmin(post(TourController.TOUR_PATH)).content(VALID_TOUR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("guideIds must all reference existing guides or lead guides"));
    }

    @Test
    void mapsDuplicateSlugToConflict() throws Exception {
        given(tourService.saveNewTour(any())).willThrow(new DataIntegrityViolationException("uk_tour_slug"));

        mockMvc.perform(asAdmin(post(TourController.TOUR_PATH)).content(VALID_TOUR))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesTour() throws Exception {
        UUID id = UUID.randomUUID();
        given(tourService.updateTour(eq(id), any())).willReturn(Optional.of(tourDTO(id)));

        mockMvc.perform(asAdmin(put(TourController.TOUR_PATH_ID, id)).content(VALID_TOUR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void returnsNotFoundWhenUpdatingUnknownTour() throws Exception {
        given(tourService.updateTour(any(), any())).willReturn(Optional.empty());

        mockMvc.perform(asAdmin(put(TourController.TOUR_PATH_ID, UUID.randomUUID())).content(VALID_TOUR))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesTour() throws Exception {
        UUID id = UUID.randomUUID();
        given(tourService.deleteTour(id)).willReturn(true);

        mockMvc.perform(asAdmin(delete(TourController.TOUR_PATH_ID, id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingUnknownTour() throws Exception {
        given(tourService.deleteTour(any())).willReturn(false);

        mockMvc.perform(asAdmin(delete(TourController.TOUR_PATH_ID, UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void letsLeadGuidesManageTours() throws Exception {
        given(tourService.deleteTour(any())).willReturn(true);

        mockMvc.perform(asRole(delete(TourController.TOUR_PATH_ID, UUID.randomUUID()), Role.LEAD_GUIDE))
                .andExpect(status().isNoContent());
    }

    @Test
    void forbidsRegularUsersAndGuidesFromManagingTours() throws Exception {
        mockMvc.perform(asRole(post(TourController.TOUR_PATH), Role.USER).content(VALID_TOUR))
                .andExpect(status().isForbidden());
        mockMvc.perform(asRole(put(TourController.TOUR_PATH_ID, UUID.randomUUID()), Role.GUIDE).content(VALID_TOUR))
                .andExpect(status().isForbidden());

        verify(tourService, never()).saveNewTour(any());
        verify(tourService, never()).updateTour(any(), any());
    }

    @Test
    void requiresAuthenticationToDelete() throws Exception {
        mockMvc.perform(delete(TourController.TOUR_PATH_ID, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verify(tourService, never()).deleteTour(any());
    }
}
