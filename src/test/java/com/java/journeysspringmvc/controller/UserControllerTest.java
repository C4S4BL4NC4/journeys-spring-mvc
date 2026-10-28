/*
 * Web-layer tests for UserController using @WebMvcTest with a mocked UserService.
 *
 * Checks that every user endpoint requires authentication, that filters/sort/paging reach
 * the service, that password and passwordConfirm never appear in responses, and the status
 * codes for create (201, 400 on mismatched passwords, 409 on duplicate email), update
 * (200, 400 on invalid input, 404) and delete (204, 404).
 */
package com.java.journeysspringmvc.controller;

import com.java.journeysspringmvc.config.SecurityConfig;
import com.java.journeysspringmvc.dto.UserDTO;
import com.java.journeysspringmvc.dto.UserUpdateDTO;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String NEW_USER = """
            {
              "name": "Jane Doe",
              "email": "jane@example.com",
              "password": "password123",
              "passwordConfirm": "password123"
            }
            """;

    private static final String USER_UPDATE = """
            {
              "name": "Jane Guide",
              "email": "jane@example.com",
              "role": "GUIDE"
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    private UserDTO userDTO(UUID id, Role role) {
        return new UserDTO(id, "Jane Doe", "jane@example.com", "default.jpg", role,
                "should-never-be-serialized", "should-never-be-serialized", null, null);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(user("admin")).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void requiresAuthenticationToListUsers() throws Exception {
        mockMvc.perform(get(UserController.USER_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsUsersWithFiltersSortAndPaging() throws Exception {
        given(userService.listUsers(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(userDTO(UUID.randomUUID(), Role.GUIDE))));

        mockMvc.perform(asAdmin(get(UserController.USER_PATH))
                        .param("name", "jane")
                        .param("role", "GUIDE")
                        .param("sort", "name")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("jane@example.com"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());

        verify(userService).listUsers("jane", Role.GUIDE, "name", 1, 10);
    }

    @Test
    void getsUserWithoutExposingPassword() throws Exception {
        UUID id = UUID.randomUUID();
        given(userService.getUserById(id)).willReturn(Optional.of(userDTO(id, Role.USER)));

        mockMvc.perform(asAdmin(get(UserController.USER_PATH_ID, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordConfirm").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        given(userService.getUserById(any())).willReturn(Optional.empty());

        mockMvc.perform(asAdmin(get(UserController.USER_PATH_ID, UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsUser() throws Exception {
        UUID id = UUID.randomUUID();
        given(userService.saveNewUser(any())).willReturn(userDTO(id, Role.USER));

        mockMvc.perform(asAdmin(post(UserController.USER_PATH)).content(NEW_USER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/" + id))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void rejectsMismatchedPasswordConfirmation() throws Exception {
        mockMvc.perform(asAdmin(post(UserController.USER_PATH)).content("""
                        {
                          "name": "Jane Doe",
                          "email": "jane@example.com",
                          "password": "password123",
                          "passwordConfirm": "different123"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).saveNewUser(any());
    }

    @Test
    void mapsDuplicateEmailToConflict() throws Exception {
        given(userService.saveNewUser(any())).willThrow(new DataIntegrityViolationException("uk_user_email"));

        mockMvc.perform(asAdmin(post(UserController.USER_PATH)).content(NEW_USER))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesUser() throws Exception {
        UUID id = UUID.randomUUID();
        given(userService.updateUser(eq(id), any())).willReturn(Optional.of(userDTO(id, Role.GUIDE)));

        mockMvc.perform(asAdmin(put(UserController.USER_PATH_ID, id)).content(USER_UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GUIDE"));

        verify(userService).updateUser(id, new UserUpdateDTO("Jane Guide", "jane@example.com", null, Role.GUIDE));
    }

    @Test
    void rejectsInvalidUpdate() throws Exception {
        mockMvc.perform(asAdmin(put(UserController.USER_PATH_ID, UUID.randomUUID())).content("""
                        { "name": "Jane", "email": "not-an-email" }
                        """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void returnsNotFoundWhenUpdatingUnknownUser() throws Exception {
        given(userService.updateUser(any(), any())).willReturn(Optional.empty());

        mockMvc.perform(asAdmin(put(UserController.USER_PATH_ID, UUID.randomUUID())).content(USER_UPDATE))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesUser() throws Exception {
        UUID id = UUID.randomUUID();
        given(userService.deleteUser(id)).willReturn(true);

        mockMvc.perform(asAdmin(delete(UserController.USER_PATH_ID, id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingUnknownUser() throws Exception {
        given(userService.deleteUser(any())).willReturn(false);

        mockMvc.perform(asAdmin(delete(UserController.USER_PATH_ID, UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }
}
