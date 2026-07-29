package ec.edu.ups.icc.academic_events.security.controllers;

import ec.edu.ups.icc.academic_events.security.dtos.AuthResponseDTO;
import ec.edu.ups.icc.academic_events.security.dtos.CurrentUserResponseDTO;
import ec.edu.ups.icc.academic_events.security.dtos.LoginRequestDTO;
import ec.edu.ups.icc.academic_events.security.dtos.RefreshTokenRequestDTO;
import ec.edu.ups.icc.academic_events.security.dtos.RegisterRequestDTO;
import ec.edu.ups.icc.academic_events.security.services.AuthService;
import ec.edu.ups.icc.academic_events.security.services.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        AuthResponseDTO response = authService.register(
                request,
                extractClientIp(httpRequest)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                authService.login(
                        request,
                        extractClientIp(httpRequest)
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                authService.refresh(
                        request,
                        extractClientIp(httpRequest)
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequestDTO request
    ) {
        authService.logout(request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDTO> me(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(
                authService.getCurrentUser(userDetails.getId())
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}