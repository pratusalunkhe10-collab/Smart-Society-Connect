package com.smartsocietyconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger/OpenAPI configuration for Smart Society Connect.
 *
 * <p>This configuration customizes the generated OpenAPI documentation shown
 * in Swagger UI. It defines API title, version, description, contact details,
 * license information, external documentation, and JWT bearer authentication.
 *
 * <p>Swagger UI is usually available at:
 * <pre>
 * /swagger-ui/index.html
 * </pre>
 *
 * <p>OpenAPI JSON is usually available at:
 * <pre>
 * /v3/api-docs
 * </pre>
 *
 * <p><b>JWT Authentication in Swagger:</b>
 * The security scheme below adds an "Authorize" button in Swagger UI.
 * Paste only the JWT token value there. Swagger will automatically send it as:
 * <pre>
 * Authorization: Bearer your.jwt.token
 * </pre>
 */
@Configuration
public class SwaggerConfig {

    /**
     * Name of the JWT security scheme shown in Swagger UI.
     *
     * <p>The same name must be used in both {@link Components} and
     * {@link SecurityRequirement}.
     */
    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    // ==========================================================================
    // 1. OpenAPI Bean
    // ==========================================================================

    /**
     * Creates the main OpenAPI bean used by Springdoc.
     *
     * <p>Springdoc automatically detects this bean and uses it to customize
     * the generated API documentation.
     *
     * @return configured OpenAPI metadata and JWT security configuration
     */
    @Bean
    public OpenAPI smartSocietyOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocumentation())
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        jwtSecurityScheme()
                                )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(SECURITY_SCHEME_NAME)
                );
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Builds API information displayed at the top of Swagger UI.
     *
     * @return API title, version, description, contact, and license details
     */
    private Info apiInfo() {
        return new Info()
                .title("Smart Society Connect API")
                .version("1.0.0")
                .description(
                        "REST APIs for Smart Society Connect, including authentication, "
                                + "resident management, flat management, family members, "
                                + "document upload, and document verification."
                )
                .contact(apiContact())
                .license(apiLicense());
    }

    /**
     * Builds contact information for the API owner/team.
     *
     * @return API contact details
     */
    private Contact apiContact() {
        return new Contact()
                .name("Smart Society Connect Team")
                .email("support@smartsocietyconnect.com");
    }

    /**
     * Builds license information displayed in Swagger UI.
     *
     * @return API license details
     */
    private License apiLicense() {
        return new License()
                .name("Academic Project");
    }

    /**
     * Builds external documentation metadata.
     *
     * <p>You can add a URL later if documentation is hosted on GitHub,
     * Notion, or a project website.
     *
     * @return external documentation configuration
     */
    private ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation()
                .description("Smart Society Connect Documentation");
    }

    /**
     * Builds JWT bearer authentication scheme for Swagger UI.
     *
     * <p>This makes Swagger send the JWT token in the Authorization header:
     * <pre>
     * Authorization: Bearer your.jwt.token
     * </pre>
     *
     * @return JWT bearer security scheme
     */
    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}