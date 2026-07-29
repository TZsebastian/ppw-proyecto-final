package ec.edu.ups.icc.academic_events.events.controllers;

import ec.edu.ups.icc.academic_events.events.dtos.EventCreateRequestDTO;
import ec.edu.ups.icc.academic_events.events.dtos.EventResponseDTO;
import ec.edu.ups.icc.academic_events.events.dtos.EventUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.events.services.EventService;
import ec.edu.ups.icc.academic_events.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponseDTO>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                eventService.findAll(status, categoryId, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @GetMapping("/mine")
    public ResponseEntity<Page<EventResponseDTO>> findMine(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                eventService.findMine(currentUser, pageable)
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @PostMapping
    public ResponseEntity<EventResponseDTO> create(
            @Valid @RequestBody EventCreateRequestDTO request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.create(request, currentUser));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EventUpdateRequestDTO request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity.ok(
                eventService.update(id, request, currentUser)
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        eventService.delete(id, currentUser);

        return ResponseEntity.noContent().build();
    }
}