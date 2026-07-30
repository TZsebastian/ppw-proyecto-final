package ec.edu.ups.icc.academic_events.registrations.controllers;

import ec.edu.ups.icc.academic_events.registrations.dtos.RegistrationResponseDTO;
import ec.edu.ups.icc.academic_events.registrations.services.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/events/{eventId}/registrations")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<RegistrationResponseDTO> create(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        RegistrationResponseDTO response =
                registrationService.create(
                        eventId,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/registrations/mine")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<Page<RegistrationResponseDTO>>
    findMine(
            Authentication authentication,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                registrationService.findMine(
                        authentication,
                        pageable
                )
        );
    }

    @GetMapping("/events/{eventId}/registrations")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<Page<RegistrationResponseDTO>>
    findByEvent(
            @PathVariable Long eventId,
            Authentication authentication,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                registrationService.findByEvent(
                        eventId,
                        authentication,
                        pageable
                )
        );
    }

    @PatchMapping("/registrations/{id}/confirm")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<RegistrationResponseDTO> confirm(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                registrationService.confirm(
                        id,
                        authentication
                )
        );
    }

    @PatchMapping("/registrations/{id}/reject")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<RegistrationResponseDTO> reject(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                registrationService.reject(
                        id,
                        authentication
                )
        );
    }

    @PatchMapping("/registrations/{id}/cancel")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<RegistrationResponseDTO> cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                registrationService.cancel(
                        id,
                        authentication
                )
        );
    }
}