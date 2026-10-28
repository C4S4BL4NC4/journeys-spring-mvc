/*
 * Web-layer tests for UserController using @WebMvcTest with a mocked UserService.
 *
 * Requests carry mock Keycloak tokens with a chosen role. Checks that listing and fetching
 * users requires a token (401) with the ADMIN role (403 otherwise), that filters/sort/paging
 * reach the service, that /me works for any logged-in user and looks up the token's subject,
 * and the 404 and 400 (malformed id) cases.
 */
package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.config.SecurityConfig;
import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserFilter;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    private final UUID userId = UUID.randomUUID();

    private UserDTO userDTO(UUID id, Role role) {
        return new UserDTO(id, "Jane Doe", "jane@example.com", "default.jpg", role, null, null);
    }

    // Stands in for a verified Keycloak token; real tokens are tested in KeycloakIntegrationTest
    private MockHttpServletRequestBuilder asRole(MockHttpServletRequestBuilder request, Role role) {
        return request.with(jwt().jwt(token -> token.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    void requiresTokenToListUsers() throws Exception {
        mockMvc.perform(get(UserController.USER_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forbidsNonAdminsFromListingUsers() throws Exception {
        mockMvc.perform(asRole(get(UserController.USER_PATH), Role.LEAD_GUIDE))
                .andExpect(status().isForbidden());
        mockMvc.perform(asRole(get(UserController.USER_PATH_ID, UUID.randomUUID()), Role.USER))
                .andExpect(status().isForbidden());

        verify(userService, never()).listUsers(any(), any());
        verify(userService, never()).getUserById(any());
    }

    @Test
    void listsUsersWithFiltersSortAndPaging() throws Exception {
        given(userService.listUsers(any(), any()))
                .willReturn(new PageImpl<>(List.of(userDTO(UUID.randomUUID(), Role.GUIDE))));

        mockMvc.perform(asRole(get(UserController.USER_PATH), Role.ADMIN)
                        .param("name", "jane")
                        .param("role", "GUIDE")
                        .param("sort", "name")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("jane@example.com"));

        verify(userService).listUsers(new UserFilter("jane", Role.GUIDE), PageRequest.of(1, 10, Sort.by("name")));
    }

    @Test
    void getsUserById() throws Exception {
        UUID id = UUID.randomUUID();
        given(userService.getUserById(id)).willReturn(Optional.of(userDTO(id, Role.USER)));

        mockMvc.perform(asRole(get(UserController.USER_PATH_ID, id), Role.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        given(userService.getUserById(any())).willReturn(Optional.empty());

        mockMvc.perform(asRole(get(UserController.USER_PATH_ID, UUID.randomUUID()), Role.ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestForMalformedId() throws Exception {
        mockMvc.perform(asRole(get(UserController.USER_PATH_ID, "not-a-uuid"), Role.ADMIN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsOwnProfileForAnyLoggedInUser() throws Exception {
        given(userService.getUserById(userId)).willReturn(Optional.of(userDTO(userId, Role.USER)));

        mockMvc.perform(asRole(get(UserController.ME_PATH), Role.USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void requiresTokenForMe() throws Exception {
        mockMvc.perform(get(UserController.ME_PATH))
                .andExpect(status().isUnauthorized());
    }
}
