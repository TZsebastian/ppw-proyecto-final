package ec.edu.ups.icc.academic_events.registrations.services;

import ec.edu.ups.icc.academic_events.events.entities.EventEntity;
import ec.edu.ups.icc.academic_events.events.repositories.EventRepository;
import ec.edu.ups.icc.academic_events.registrations.dtos.RegistrationResponseDTO;
import ec.edu.ups.icc.academic_events.registrations.entities.RegistrationEntity;
import ec.edu.ups.icc.academic_events.registrations.enums.RegistrationStatus;
import ec.edu.ups.icc.academic_events.registrations.mappers.RegistrationMapper;
import ec.edu.ups.icc.academic_events.registrations.repositories.RegistrationRepository;
import ec.edu.ups.icc.academic_events.users.entities.UserEntity;
import ec.edu.ups.icc.academic_events.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;

    @Transactional
    public RegistrationResponseDTO create(
            Long eventId,
            Authentication authentication
    ) {
        UserEntity participant =
                findAuthenticatedUser(authentication);

        EventEntity event = findEventById(eventId);

        validateEventAllowsRegistrations(event);

        if (registrationRepository
                .existsByEventIdAndParticipantId(
                        eventId,
                        participant.getId()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El participante ya tiene una inscripción para este evento"
            );
        }

        /*
         * Aunque una inscripción PENDING todavía no consume cupo,
         * evitamos nuevas solicitudes cuando el evento ya está lleno.
         */
        if (event.getAvailableCapacity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento no tiene cupos disponibles"
            );
        }

        RegistrationEntity registration =
                new RegistrationEntity();

        registration.setEvent(event);
        registration.setParticipant(participant);
        registration.setStatus(
                RegistrationStatus.PENDING.name()
        );

        RegistrationEntity saved =
                registrationRepository.save(registration);

        return registrationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<RegistrationResponseDTO> findMine(
            Authentication authentication,
            Pageable pageable
    ) {
        UserEntity participant =
                findAuthenticatedUser(authentication);

        return registrationRepository
                .findAllByParticipantId(
                        participant.getId(),
                        pageable
                )
                .map(registrationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RegistrationResponseDTO> findByEvent(
            Long eventId,
            Authentication authentication,
            Pageable pageable
    ) {
        EventEntity event = findEventById(eventId);

        validateEventManagementPermission(
                event,
                authentication
        );

        return registrationRepository
                .findAllByEventId(eventId, pageable)
                .map(registrationMapper::toResponse);
    }

    @Transactional
    public RegistrationResponseDTO confirm(
            Long registrationId,
            Authentication authentication
    ) {
        RegistrationEntity registration =
                findRegistrationById(registrationId);

        EventEntity event = registration.getEvent();

        validateEventManagementPermission(
                event,
                authentication
        );

        if (!RegistrationStatus.PENDING.name()
                .equals(registration.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden confirmar inscripciones pendientes"
            );
        }

        if (event.getAvailableCapacity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento no tiene cupos disponibles"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        registration.setStatus(
                RegistrationStatus.CONFIRMED.name()
        );
        registration.setConfirmedAt(now);
        registration.setCancelledAt(null);
        registration.setStatusUpdatedAt(now);

        event.setAvailableCapacity(
                event.getAvailableCapacity() - 1
        );

    
        eventRepository.save(event);

        RegistrationEntity saved =
                registrationRepository.save(registration);

        return registrationMapper.toResponse(saved);
    }

    @Transactional
    public RegistrationResponseDTO reject(
            Long registrationId,
            Authentication authentication
    ) {
        RegistrationEntity registration =
                findRegistrationById(registrationId);

        validateEventManagementPermission(
                registration.getEvent(),
                authentication
        );

        if (!RegistrationStatus.PENDING.name()
                .equals(registration.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden rechazar inscripciones pendientes"
            );
        }

        registration.setStatus(
                RegistrationStatus.REJECTED.name()
        );
        registration.setConfirmedAt(null);
        registration.setCancelledAt(null);
        registration.setStatusUpdatedAt(
                LocalDateTime.now()
        );

        RegistrationEntity saved =
                registrationRepository.save(registration);

        return registrationMapper.toResponse(saved);
    }

    @Transactional
    public RegistrationResponseDTO cancel(
            Long registrationId,
            Authentication authentication
    ) {
        UserEntity participant =
                findAuthenticatedUser(authentication);

        RegistrationEntity registration =
                registrationRepository
                        .findByIdAndParticipantId(
                                registrationId,
                                participant.getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Inscripción no encontrada para el participante autenticado"
                                )
                        );

        if (RegistrationStatus.CANCELLED.name()
                .equals(registration.getStatus())) {
            return registrationMapper.toResponse(
                    registration
            );
        }

        if (RegistrationStatus.REJECTED.name()
                .equals(registration.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Una inscripción rechazada no puede cancelarse"
            );
        }

        EventEntity event = registration.getEvent();

        
         //Solo una inscripción confirmada consume cupo.
         
        if (RegistrationStatus.CONFIRMED.name()
                .equals(registration.getStatus())) {
            if (event.getAvailableCapacity()
                    < event.getCapacity()) {
                event.setAvailableCapacity(
                        event.getAvailableCapacity() + 1
                );
                eventRepository.save(event);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        registration.setStatus(
                RegistrationStatus.CANCELLED.name()
        );
        registration.setCancelledAt(now);
        registration.setStatusUpdatedAt(now);

        RegistrationEntity saved =
                registrationRepository.save(registration);

        return registrationMapper.toResponse(saved);
    }

    private EventEntity findEventById(Long eventId) {
        return eventRepository
                .findByIdAndDeletedFalse(eventId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Evento no encontrado"
                        )
                );
    }

    private RegistrationEntity findRegistrationById(
            Long registrationId
    ) {
        return registrationRepository
                .findById(registrationId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Inscripción no encontrada"
                        )
                );
    }

    private UserEntity findAuthenticatedUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Usuario autenticado no encontrado"
                        )
                );
    }

    private void validateEventAllowsRegistrations(
            EventEntity event
    ) {
        if (!"PUBLISHED".equals(event.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento no está publicado"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(event.getRegistrationStartAt())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El periodo de inscripciones todavía no ha comenzado"
            );
        }

        if (now.isAfter(event.getRegistrationEndAt())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El periodo de inscripciones ha finalizado"
            );
        }

        if (!now.isBefore(event.getStartAt())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se permiten inscripciones para un evento iniciado o finalizado"
            );
        }
    }

    private void validateEventManagementPermission(
            EventEntity event,
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                );

        if (isAdmin) {
            return;
        }

        boolean isOrganizer =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                "ROLE_ORGANIZER".equals(
                                        authority.getAuthority()
                                )
                        );

        if (!isOrganizer) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para gestionar inscripciones"
            );
        }

        String authenticatedEmail =
                authentication.getName();

        String organizerEmail =
                event.getOrganizer().getEmail();

        if (!organizerEmail.equalsIgnoreCase(
                authenticatedEmail
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el propietario del evento puede gestionar sus inscripciones"
            );
        }
    }
}