package ec.edu.ups.icc.academic_events.security.config;

import ec.edu.ups.icc.academic_events.security.filters.JwtAuthenticationFilter;
import ec.edu.ups.icc.academic_events.security.services.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ec.edu.ups.icc.academic_events.security.handlers.CustomAccessDeniedHandler;
import ec.edu.ups.icc.academic_events.security.handlers.CustomAuthenticationEntryPoint;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final UserDetailsServiceImpl userDetailsService;
        private final CustomAuthenticationEntryPoint authenticationEntryPoint;
        private final CustomAccessDeniedHandler accessDeniedHandler;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        UserDetailsServiceImpl userDetailsService,
                        CustomAuthenticationEntryPoint authenticationEntryPoint,
                        CustomAccessDeniedHandler accessDeniedHandler) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.userDetailsService = userDetailsService;
                this.authenticationEntryPoint = authenticationEntryPoint;
                this.accessDeniedHandler = accessDeniedHandler;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(
                                                                authenticationEntryPoint)
                                                .accessDeniedHandler(
                                                                accessDeniedHandler))

                                .authorizeHttpRequests(auth -> auth

                                                // Autenticación pública
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/api/auth/refresh",
                                                                "/api/auth/logout")
                                                .permitAll()

                                                // Usuario autenticado
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/auth/me")
                                                .authenticated()

                                                // Actuator
                                                .requestMatchers(
                                                                "/actuator/health")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/api/users/**")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/categories/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/categories/**")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/categories/**")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/categories/**")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/categories/**")
                                                .hasRole("ADMIN")

                                                // Eventos propios
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/events/mine")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                // Participante crea inscripción
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/events/*/registrations")
                                                .hasRole("PARTICIPANT")

                                                // ADMIN u ORGANIZER consulta inscripciones de un evento
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/events/*/registrations")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                // Participante consulta sus propias inscripciones
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/registrations/mine")
                                                .hasRole("PARTICIPANT")

                                                // ADMIN u ORGANIZER confirma o rechaza
                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/registrations/*/confirm",
                                                                "/api/registrations/*/reject")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                // Participante cancela su propia inscripción
                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/registrations/*/cancel")
                                                .hasRole("PARTICIPANT")

                                                // Consultas públicas
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/events/*/sessions",
                                                                "/api/events/*/sessions/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/events/*/sessions",
                                                                "/api/events/*/sessions/**")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/events/*/sessions/**")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/events/*/sessions/**")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/events/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/events/**")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/events/**")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/events/**")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/events/**")
                                                .hasAnyRole("ADMIN", "ORGANIZER")

                                                .anyRequest()
                                                .authenticated())

                                .authenticationProvider(
                                                authenticationProvider())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

                provider.setPasswordEncoder(passwordEncoder());

                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}