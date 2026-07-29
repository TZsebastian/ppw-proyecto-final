package ec.edu.ups.icc.academic_events.sessions.controllers;

import ec.edu.ups.icc.academic_events.sessions.dtos.SessionCreateRequestDTO;
import ec.edu.ups.icc.academic_events.sessions.dtos.SessionResponseDTO;
import ec.edu.ups.icc.academic_events.sessions.dtos.SessionUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.sessions.services.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionResponseDTO>> findAll(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(
                sessionService.findAllByEvent(eventId)
        );
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponseDTO> findById(
            @PathVariable Long eventId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(
                sessionService.findById(
                        eventId,
                        sessionId
                )
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<SessionResponseDTO> create(
            @PathVariable Long eventId,
            @Valid @RequestBody
            SessionCreateRequestDTO request,
            Authentication authentication
    ) {
        SessionResponseDTO response =
                sessionService.create(
                        eventId,
                        request,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<SessionResponseDTO> update(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @Valid @RequestBody
            SessionUpdateRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                sessionService.update(
                        eventId,
                        sessionId,
                        request,
                        authentication
                )
        );
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<Void> delete(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        sessionService.delete(
                eventId,
                sessionId,
                authentication
        );

        return ResponseEntity.noContent().build();
    }
}