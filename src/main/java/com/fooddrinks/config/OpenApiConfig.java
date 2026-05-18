package com.fooddrinks.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * SpringDoc OpenAPI configuration.
 *
 * Only active when {@code springdoc.swagger-ui.enabled=true} (default:
 * {@code false}).
 * Set the property to {@code true} in development; leave it unset (or
 * explicitly
 * {@code false}) in production to prevent API schema exposure.
 *
 * Swagger UI: {@code /swagger-ui.html}
 * JSON spec: {@code /v3/api-docs}
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true")
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    /**
     * Base URL of this server — used as the Swagger UI "server" entry.
     * Override via {@code APP_SERVER_URL} env var in non-local environments.
     */
    @Value("${app.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Foods & Drinks API")
                        .description("""
                                REST API for the Foods & Drinks ordering platform.

                                **Authentication**: Most endpoints require a JWT token obtained from
                                `POST /api/auth/login`. Click **Authorize** and enter the token value
                                (without the `Bearer ` prefix).

                                **Social Login**: OAuth2 flows (Google / Facebook / Apple) are initiated
                                via browser at `/oauth2/authorization/{provider}`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Foods & Drinks Team")))
                .servers(List.of(
                        new Server().url(serverUrl).description("Current server")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token (without 'Bearer ' prefix)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
