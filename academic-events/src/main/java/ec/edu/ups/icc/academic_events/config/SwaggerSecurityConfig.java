package ec.edu.ups.icc.academic_events.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(
        name = "swagger.security.enabled",
        havingValue = "true"
)
public class SwaggerSecurityConfig {

    private final SwaggerSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;

    public SwaggerSecurityConfig(
            SwaggerSecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        validateCredentials();

        InMemoryUserDetailsManager swaggerUsers =
                new InMemoryUserDetailsManager(
                        User.withUsername(properties.getUsername())
                                .password(
                                        passwordEncoder.encode(
                                                properties.getPassword()
                                        )
                                )
                                .roles("SWAGGER")
                                .build()
                );

        DaoAuthenticationProvider swaggerProvider =
                new DaoAuthenticationProvider(swaggerUsers);

        swaggerProvider.setPasswordEncoder(passwordEncoder);

        http
                .securityMatcher(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml"
                )

                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .anyRequest()
                        .hasRole("SWAGGER")
                )

                .authenticationProvider(swaggerProvider)

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    private void validateCredentials() {
        if (!StringUtils.hasText(properties.getUsername())) {
            throw new IllegalStateException(
                    "SWAGGER_USERNAME es obligatorio cuando Swagger está protegido"
            );
        }

        if (!StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException(
                    "SWAGGER_PASSWORD es obligatorio cuando Swagger está protegido"
            );
        }

        if (properties.getPassword().length() < 12) {
            throw new IllegalStateException(
                    "SWAGGER_PASSWORD debe tener al menos 12 caracteres"
            );
        }
    }
}