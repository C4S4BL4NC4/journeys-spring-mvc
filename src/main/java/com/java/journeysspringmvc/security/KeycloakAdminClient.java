/*
 * Calls Keycloak's admin REST API (/admin/realms/journeys/...) on behalf of this API.
 *
 * It authenticates as the confidential "journeys-backend" client with the OAuth2 client
 * credentials grant (spring.security.oauth2.client.registration.keycloak-admin.*). Spring
 * fetches and caches that service-account token and adds it to every call. In the realm, the
 * client's service account holds realm-management's manage-users, view-users and view-realm
 * roles, which is exactly what these operations need.
 *
 *   disableUser  sets the Keycloak account to disabled: no more logins or new tokens
 *   setRole      makes the given app role the user's only directly-assigned app role
 *                (USER stays effective through the realm's default roles)
 *
 * Keycloak 404s become NotFoundException; any other failure becomes IdentityProviderException (502).
 */
package com.java.journeysspringmvc.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.java.journeysspringmvc.entity.Role;
import com.java.journeysspringmvc.exception.IdentityProviderException;
import com.java.journeysspringmvc.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver.principal;

@Component
public class KeycloakAdminClient {

    static final String REGISTRATION_ID = "keycloak-admin";

    private final RestClient restClient;

    public KeycloakAdminClient(ClientRegistrationRepository clientRegistrations,
                               @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrations, new InMemoryOAuth2AuthorizedClientService(clientRegistrations));
        var oauth2 = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        // The service-account token belongs to this API, not to whichever user made the request
        oauth2.setPrincipalResolver(new RequestAttributePrincipalResolver());

        this.restClient = RestClient.builder()
                // .../realms/journeys -> .../admin/realms/journeys
                .baseUrl(issuerUri.replace("/realms/", "/admin/realms/"))
                .requestFactory(new JdkClientHttpRequestFactory())
                .requestInterceptor(oauth2)
                .defaultRequest(request -> request
                        .attributes(clientRegistrationId(REGISTRATION_ID))
                        .attributes(principal(REGISTRATION_ID)))
                .build();
    }

    public void disableUser(UUID userId) {
        call(() -> restClient.put().uri("/users/{id}", userId)
                .body(Map.of("enabled", false))
                .retrieve().toBodilessEntity());
    }

    public void setRole(UUID userId, Role role) {
        List<KeycloakRole> current = call(() -> restClient.get().uri("/users/{id}/role-mappings/realm", userId)
                .retrieve().body(new ParameterizedTypeReference<List<KeycloakRole>>() {
                }));

        List<KeycloakRole> otherAppRoles = current.stream()
                .filter(mapped -> Role.fromName(mapped.name()).filter(app -> app != role && app != Role.USER).isPresent())
                .toList();
        if (!otherAppRoles.isEmpty()) {
            call(() -> restClient.method(HttpMethod.DELETE).uri("/users/{id}/role-mappings/realm", userId)
                    .body(otherAppRoles)
                    .retrieve().toBodilessEntity());
        }

        if (current.stream().noneMatch(mapped -> mapped.name().equals(role.name()))) {
            KeycloakRole target = call(() -> restClient.get().uri("/roles/{name}", role.name())
                    .retrieve().body(KeycloakRole.class));
            call(() -> restClient.post().uri("/users/{id}/role-mappings/realm", userId)
                    .body(List.of(target))
                    .retrieve().toBodilessEntity());
        }
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("No Keycloak account found for this user");
        } catch (RestClientException ex) {
            throw new IdentityProviderException("Keycloak admin API call failed: " + ex.getMessage(), ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeycloakRole(String id, String name) {
    }
}
