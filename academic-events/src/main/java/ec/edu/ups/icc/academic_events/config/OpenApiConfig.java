package ec.edu.ups.icc.academic_events.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {

        Info info = new Info()
                .title("Academic Events API")
                .version("1.0.0")
                .description("""
                        API REST para gestión de eventos académicos.

                        Incluye:
                        - autenticación JWT con refresh token
                        - autorización por roles (ADMIN, ORGANIZER, PARTICIPANT)
                        - rate limiting con Redis
                        - reportes descargables
                        """);

        SecurityScheme bearerScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Ingrese el access token generado en /api/auth/login");

        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme);

        return new OpenAPI()
                .info(info)
                .components(components);
    }
}