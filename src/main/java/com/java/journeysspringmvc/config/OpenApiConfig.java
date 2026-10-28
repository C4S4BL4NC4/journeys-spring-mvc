/*
 * Swagger / OpenAPI documentation settings (served at /swagger-ui.html by springdoc).
 *
 * Declares Keycloak as an OAuth2 authorization-code provider and applies it to every
 * endpoint. In Swagger UI, "Authorize" sends you to Keycloak's login page (using the public
 * journeys-web client with PKCE, configured by springdoc.swagger-ui.oauth.* properties) and
 * then attaches the access token to every "Try it out" request. Public endpoints ignore it.
 * The Keycloak URLs are derived from the same issuer-uri the API validates tokens against.
 */
package com.java.journeysspringmvc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "keycloak";

    @Bean
    public OpenAPI journeysOpenApi(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        String endpoints = issuerUri + "/protocol/openid-connect";
        OAuthFlow authorizationCode = new OAuthFlow()
                .authorizationUrl(endpoints + "/auth")
                .tokenUrl(endpoints + "/token")
                .scopes(new Scopes().addString("openid", "Sign in with OpenID Connect"));

        return new OpenAPI()
                .info(new Info().title("Journeys API").version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(authorizationCode))))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
