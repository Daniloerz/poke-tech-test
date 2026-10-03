package com.poketechtest.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Declares the Bearer scheme, so Swagger UI shows the "Authorize" button for protected routes. */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Poke Tech Test API", version = "v1"))
@SecurityScheme(name = OpenApiConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
