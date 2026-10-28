/*
 * End-to-end security tests against a real Keycloak, using Testcontainers (needs Docker).
 *
 * Starts Keycloak 26.8.1 with the same keycloak/journeys-realm.json the docker-compose setup
 * imports, points the app's issuer-uri at it, and gets real access tokens from Keycloak's
 * token endpoint through the dev-only journeys-dev-cli client. This exercises the whole chain:
 * signature check against Keycloak's published keys, issuer and audience validation, realm
 * roles -> authorities, the profile sync into the users table, and the API's own calls to
 * Keycloak's admin API (role changes and deactivation through the journeys-backend client).
 *
 * @Transactional rolls back the app's database changes after each test (MockMvc runs requests
 * on the test's thread). Keycloak changes made through its admin API stay in the container,
 * so tests that create Keycloak users use unique emails.
 */
package com.java.journeysspringmvc.security;

import com.java.journeysspringmvc.controller.TourController;
import com.java.journeysspringmvc.controller.UserController;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KeycloakIntegrationTest {

    private static final String REALM = "journeys";
    private static final String PASSWORD = "password123";

    // Started once for the class; Testcontainers' Ryuk container removes it when the JVM exits
    static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.8.1")
            .withCopyFileToContainer(MountableFile.forHostPath("keycloak/journeys-realm.json"),
                    "/opt/keycloak/data/import/journeys-realm.json");

    static {
        keycloak.start();
    }

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakIntegrationTest::issuerUri);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    private static String issuerUri() {
        return keycloak.getAuthServerUrl() + "/realms/" + REALM;
    }

    private static String accessToken(String clientId, String email, String password) throws Exception {
        HttpResponse<String> response = requestToken(clientId, email, password);
        assertThat(response.statusCode()).as("token request for %s: %s", email, response.body()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.access_token");
    }

    private static HttpResponse<String> requestToken(String clientId, String email, String password) throws Exception {
        String form = Map.of("grant_type", "password", "client_id", clientId, "username", email, "password", password)
                .entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(issuerUri() + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private static String accessToken(String email) throws Exception {
        return accessToken("journeys-dev-cli", email, PASSWORD);
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static RealmResource realm() {
        return keycloak.getKeycloakAdminClient().realm(REALM);
    }

    // Same as signing up on Keycloak's registration page: a new account gets the default USER role
    private static UUID registerKeycloakUser(String email, String firstName, String lastName) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(PASSWORD);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        try (var response = realm().users().create(user)) {
            assertThat(response.getStatus()).isEqualTo(201);
            String location = response.getLocation().getPath();
            return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        }
    }

    @Test
    void rejectsRequestsWithoutValidToken() throws Exception {
        mockMvc.perform(get(UserController.ME_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(withToken(get(UserController.ME_PATH), "not.a.jwt")).andExpect(status().isUnauthorized());

        // A real token with a tampered signature
        String token = accessToken("jane@journeys.dev");
        String forged = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        mockMvc.perform(withToken(get(UserController.ME_PATH), forged)).andExpect(status().isUnauthorized());
    }

    @Test
    void seedUserTokenMapsToSeededProfile() throws Exception {
        mockMvc.perform(withToken(get(UserController.ME_PATH), accessToken("jane@journeys.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("6a1f3c52-3e1d-4c6b-9a57-1d2f0b7e8a04"))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void enforcesRealmRoles() throws Exception {
        String jane = accessToken("jane@journeys.dev");
        String leadGuide = accessToken("lead.guide@journeys.dev");
        String admin = accessToken("admin@journeys.dev");

        mockMvc.perform(withToken(post(TourController.TOUR_PATH), jane).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        // Past security; the empty body then fails validation, which proves the role check passed
        mockMvc.perform(withToken(post(TourController.TOUR_PATH), leadGuide).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(withToken(get(UserController.USER_PATH), leadGuide)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(get(UserController.USER_PATH), admin)).andExpect(status().isOk());
    }

    @Test
    void provisionsProfileForNewKeycloakUser() throws Exception {
        UUID keycloakId = registerKeycloakUser("newcomer@example.com", "Nora", "Newcomer");
        assertThat(userRepository.findById(keycloakId)).isEmpty();

        mockMvc.perform(withToken(get(UserController.ME_PATH), accessToken("newcomer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(keycloakId.toString()))
                .andExpect(jsonPath("$.name").value("Nora Newcomer"))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(userRepository.findById(keycloakId)).isPresent();
    }

    @Test
    void picksUpRoleGrantedInKeycloak() throws Exception {
        UUID keycloakId = registerKeycloakUser("promoted@example.com", "Pat", "Promoted");
        mockMvc.perform(withToken(get(UserController.ME_PATH), accessToken("promoted@example.com")))
                .andExpect(jsonPath("$.role").value("USER"));

        realm().users().get(keycloakId.toString()).roles().realmLevel()
                .add(List.of(realm().roles().get(Role.LEAD_GUIDE.name()).toRepresentation()));

        String newToken = accessToken("promoted@example.com");
        mockMvc.perform(withToken(get(UserController.ME_PATH), newToken))
                .andExpect(jsonPath("$.role").value("LEAD_GUIDE"));
        mockMvc.perform(withToken(post(TourController.TOUR_PATH), newToken).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTokensIssuedForAnotherAudience() throws Exception {
        ClientRepresentation otherApp = new ClientRepresentation();
        otherApp.setClientId("other-app-" + UUID.randomUUID());
        otherApp.setPublicClient(true);
        otherApp.setDirectAccessGrantsEnabled(true);
        otherApp.setStandardFlowEnabled(false);
        try (var response = realm().clients().create(otherApp)) {
            assertThat(response.getStatus()).isEqualTo(201);
        }

        // Same realm, same user, valid signature, but not minted for "journeys-api"
        String token = accessToken(otherApp.getClientId(), "jane@journeys.dev", PASSWORD);
        mockMvc.perform(withToken(get(UserController.ME_PATH), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminChangesRoleThroughKeycloak() throws Exception {
        UUID keycloakId = registerKeycloakUser("role.change@example.com", "Rory", "Role");
        mockMvc.perform(withToken(get(UserController.ME_PATH), accessToken("role.change@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(withToken(patch(UserController.USER_PATH_ID, keycloakId), accessToken("admin@journeys.dev"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"lead-guide\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LEAD_GUIDE"));

        assertThat(realm().users().get(keycloakId.toString()).roles().realmLevel().listAll())
                .extracting(RoleRepresentation::getName).contains("LEAD_GUIDE");
        // The next token Keycloak issues carries the new role
        mockMvc.perform(withToken(post(TourController.TOUR_PATH), accessToken("role.change@example.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminDeactivationDisablesAccountAndRejectsItsTokens() throws Exception {
        UUID keycloakId = registerKeycloakUser("deactivated@example.com", "Dee", "Activated");
        String token = accessToken("deactivated@example.com");
        mockMvc.perform(withToken(get(UserController.ME_PATH), token)).andExpect(status().isOk());

        mockMvc.perform(withToken(delete(UserController.USER_PATH_ID, keycloakId), accessToken("admin@journeys.dev")))
                .andExpect(status().isNoContent());

        assertThat(realm().users().get(keycloakId.toString()).toRepresentation().isEnabled()).isFalse();
        // The token is still cryptographically valid for a few minutes, but the API turns it away
        mockMvc.perform(withToken(get(UserController.ME_PATH), token)).andExpect(status().isUnauthorized());
        assertThat(requestToken("journeys-dev-cli", "deactivated@example.com", PASSWORD).statusCode()).isEqualTo(400);
    }

    @Test
    void deleteMeDeactivatesOwnAccount() throws Exception {
        UUID keycloakId = registerKeycloakUser("leaving@example.com", "Lee", "Leaving");
        String token = accessToken("leaving@example.com");
        mockMvc.perform(withToken(get(UserController.ME_PATH), token)).andExpect(status().isOk());

        mockMvc.perform(withToken(delete(UserController.ME_PATH), token)).andExpect(status().isNoContent());

        assertThat(realm().users().get(keycloakId.toString()).toRepresentation().isEnabled()).isFalse();
        mockMvc.perform(withToken(get(UserController.ME_PATH), token)).andExpect(status().isUnauthorized());
    }
}
