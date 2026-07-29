package ec.edu.ups.icc.academic_events.sessions.services;

import ec.edu.ups.icc.academic_events.events.entities.EventEntity;
import ec.edu.ups.icc.academic_events.events.repositories.EventRepository;
import ec.edu.ups.icc.academic_events.sessions.dtos.SessionCreateRequestDTO;
import ec.edu.ups.icc.academic_events.sessions.dtos.SessionResponseDTO;
import ec.edu.ups.icc.academic_events.sessions.dtos.SessionUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.sessions.entities.SessionEntity;
import ec.edu.ups.icc.academic_events.sessions.mappers.SessionMapper;
import ec.edu.ups.icc.academic_events.sessions.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final SessionMapper sessionMapper;

    @Transactional(readOnly = true)
    public List<SessionResponseDTO> findAllByEvent(
            Long eventId) {
        findEventById(eventId);

        return sessionRepository
                .findAllByEventIdOrderByStartAtAsc(eventId)
                .stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponseDTO findById(
            Long eventId,
            Long sessionId) {
        findEventById(eventId);

        return sessionMapper.toResponse(
                findSessionByIdAndEventId(
                        sessionId,
                        eventId));
    }

    @Transactional
    public SessionResponseDTO create(
            Long eventId,
            SessionCreateRequestDTO request,
            Authentication authentication) {
        EventEntity event = findEventById(eventId);

        validateOwnership(event, authentication);
        validateDates(request.startAt(), request.endAt());
        validateInsideEvent(
                event,
                request.startAt(),
                request.endAt());
        validateNoOverlap(
                eventId,
                null,
                request.startAt(),
                request.endAt());

        SessionEntity session = new SessionEntity();
        session.setEvent(event);
        session.setTitle(normalizeRequiredText(
                request.title(),
                "El título es obligatorio"));
        session.setDescription(
                normalizeOptionalText(request.description()));
        session.setStartAt(request.startAt());
        session.setEndAt(request.endAt());
        session.setLocation(
                normalizeOptionalText(request.location()));
        session.setVirtualUrl(
                normalizeOptionalText(request.virtualUrl()));

        SessionEntity saved = sessionRepository.save(session);

        return sessionMapper.toResponse(saved);
    }

    @Transactional
    public SessionResponseDTO update(
            Long eventId,
            Long sessionId,
            SessionUpdateRequestDTO request,
            Authentication authentication) {
        EventEntity event = findEventById(eventId);

        validateOwnership(event, authentication);

        SessionEntity session = findSessionByIdAndEventId(
                sessionId,
                eventId);

        validateDates(request.startAt(), request.endAt());
        validateInsideEvent(
                event,
                request.startAt(),
                request.endAt());
        validateNoOverlap(
                eventId,
                sessionId,
                request.startAt(),
                request.endAt());

        session.setTitle(normalizeRequiredText(
                request.title(),
                "El título es obligatorio"));
        session.setDescription(
                normalizeOptionalText(request.description()));
        session.setStartAt(request.startAt());
        session.setEndAt(request.endAt());
        session.setLocation(
                normalizeOptionalText(request.location()));
        session.setVirtualUrl(
                normalizeOptionalText(request.virtualUrl()));

        SessionEntity saved = sessionRepository.save(session);

        return sessionMapper.toResponse(saved);
    }

    @Transactional
    public void delete(
            Long eventId,
            Long sessionId,
            Authentication authentication) {
        EventEntity event = findEventById(eventId);

        validateOwnership(event, authentication);

        SessionEntity session = findSessionByIdAndEventId(
                sessionId,
                eventId);

        sessionRepository.delete(session);
    }

    private EventEntity findEventById(Long eventId) {
        return eventRepository.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento no encontrado"));
    }

    private SessionEntity findSessionByIdAndEventId(
            Long sessionId,
            Long eventId) {
        return sessionRepository
                .findByIdAndEventId(sessionId, eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sesión no encontrada para el evento indicado"));
    }

    private void validateOwnership(
            EventEntity event,
            Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado");
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            return;
        }

        boolean isOrganizer = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ORGANIZER".equals(authority.getAuthority()));

        if (!isOrganizer) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para gestionar sesiones");
        }

        String authenticatedEmail = authentication.getName();

        String organizerEmail = event.getOrganizer().getEmail();

        if (!organizerEmail.equalsIgnoreCase(
                authenticatedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el propietario del evento puede gestionar sus sesiones");
        }
    }

    private void validateDates(
            LocalDateTime startAt,
            LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las fechas de inicio y fin son obligatorias");
        }

        if (!startAt.isBefore(endAt)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de inicio debe ser anterior a la fecha de finalización");
        }
    }

    private void validateInsideEvent(
            EventEntity event,
            LocalDateTime startAt,
            LocalDateTime endAt) {
        boolean startsBeforeEvent = startAt.isBefore(event.getStartAt());

        boolean endsAfterEvent = endAt.isAfter(event.getEndAt());

        if (startsBeforeEvent || endsAfterEvent) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La sesión debe estar dentro del horario del evento");
        }
    }

    private void validateNoOverlap(
            Long eventId,
            Long sessionId,
            LocalDateTime startAt,
            LocalDateTime endAt) {
        boolean overlaps;

        if (sessionId == null) {
            overlaps = !sessionRepository
                    .findAllByEventIdAndStartAtLessThanAndEndAtGreaterThan(
                            eventId,
                            endAt,
                            startAt)
                    .isEmpty();
        } else {
            overlaps = !sessionRepository
                    .findAllByEventIdAndIdNotAndStartAtLessThanAndEndAtGreaterThan(
                            eventId,
                            sessionId,
                            endAt,
                            startAt)
                    .isEmpty();
        }

        if (overlaps) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La sesión se solapa con otra sesión del evento");
        }
    }

    private String normalizeRequiredText(
            String value,
            String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorMessage);
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().replaceAll("\\s+", " ");
    }
}