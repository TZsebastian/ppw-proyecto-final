package ec.edu.ups.icc.academic_events.security.filters;

import ec.edu.ups.icc.academic_events.security.config.JwtProperties;
import ec.edu.ups.icc.academic_events.security.services.UserDetailsServiceImpl;
import ec.edu.ups.icc.academic_events.security.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            JwtProperties jwtProperties,
            UserDetailsServiceImpl userDetailsService
    ) {
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null
                    && jwtUtil.validateAccessToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String email = jwtUtil.getEmailFromToken(token);

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                if (userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }
        } catch (Exception exception) {
            logger.warn(
                    "No se pudo autenticar la solicitud mediante JWT: {}",
                    exception.getMessage()
            );

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String headerName = jwtProperties.getHeader();
        String prefix = jwtProperties.getPrefix();

        String authorizationHeader = request.getHeader(headerName);

        if (authorizationHeader == null
                || prefix == null
                || !authorizationHeader.startsWith(prefix)) {
            return null;
        }

        String token = authorizationHeader
                .substring(prefix.length())
                .trim();

        return token.isBlank() ? null : token;
    }
}