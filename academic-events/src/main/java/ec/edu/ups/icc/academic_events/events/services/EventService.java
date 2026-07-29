package ec.edu.ups.icc.academic_events.events.services;

import ec.edu.ups.icc.academic_events.categories.entities.CategoryEntity;
import ec.edu.ups.icc.academic_events.categories.repositories.CategoryRepository;
import ec.edu.ups.icc.academic_events.events.dtos.EventCreateRequestDTO;
import ec.edu.ups.icc.academic_events.events.dtos.EventResponseDTO;
import ec.edu.ups.icc.academic_events.events.dtos.EventUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.events.entities.EventEntity;
import ec.edu.ups.icc.academic_events.events.mappers.EventMapper;
import ec.edu.ups.icc.academic_events.events.repositories.EventRepository;
import ec.edu.ups.icc.academic_events.security.services.UserDetailsImpl;
import ec.edu.ups.icc.academic_events.users.entities.UserEntity;
import ec.edu.ups.icc.academic_events.users.repositories.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class EventService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    public EventService(
            EventRepository eventRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> findAll(
            String status,
            Long categoryId,
            Pageable pageable) {
        Page<EventEntity> events;

        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);

            validateStatus(normalizedStatus);

            events = eventRepository.findAllByStatusAndDeletedFalse(
                    normalizedStatus,
                    pageable);
        } else if (categoryId != null) {
            events = eventRepository.findAllByCategoryIdAndDeletedFalse(
                    categoryId,
                    pageable);
        } else {
            events = eventRepository.findAllByDeletedFalse(pageable);
        }

        return events.map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long id) {
        return eventMapper.toResponse(findActiveEvent(id));
    }

    @Transactional
    public EventResponseDTO create(
            EventCreateRequestDTO request,
            UserDetailsImpl currentUser) {
        UserEntity organizer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado"));

        CategoryEntity category = findActiveCategory(
                request.categoryId());

        String modality = normalize(request.modality());
        String status = request.status() == null
                || request.status().isBlank()
                        ? "DRAFT"
                        : normalize(request.status());

        validateEventData(
                modality,
                request.location(),
                request.virtualUrl(),
                request.registrationStartAt(),
                request.registrationEndAt(),
                request.startAt(),
                request.endAt());

        EventEntity event = new EventEntity();

        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setModality(modality);
        applyModalityData(
                event,
                modality,
                request.location(),
                request.virtualUrl());
        event.setCapacity(request.capacity());
        event.setAvailableCapacity(request.capacity());
        event.setRegistrationStartAt(request.registrationStartAt());
        event.setRegistrationEndAt(request.registrationEndAt());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setStatus(status);
        event.setOrganizer(organizer);
        event.setCategory(category);
        event.setDeleted(false);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDTO update(
            Long id,
            EventUpdateRequestDTO request,
            UserDetailsImpl currentUser) {
        EventEntity event = findActiveEvent(id);

        validateOwnership(event, currentUser);

        if (!event.getVersion().equals(request.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento fue modificado por otro usuario. Recarga los datos.");
        }

        CategoryEntity category = findActiveCategory(
                request.categoryId());

        String modality = normalize(request.modality());
        String status = normalize(request.status());

        validateStatus(status);

        validateEventData(
                modality,
                request.location(),
                request.virtualUrl(),
                request.registrationStartAt(),
                request.registrationEndAt(),
                request.startAt(),
                request.endAt());

        int registeredParticipants = event.getCapacity() - event.getAvailableCapacity();

        if (request.capacity() < registeredParticipants) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La capacidad no puede ser menor al número de inscritos");
        }

        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setModality(modality);
        applyModalityData(
                event,
                modality,
                request.location(),
                request.virtualUrl());
        event.setAvailableCapacity(
                request.capacity() - registeredParticipants);
        event.setCapacity(request.capacity());
        event.setRegistrationStartAt(request.registrationStartAt());
        event.setRegistrationEndAt(request.registrationEndAt());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setStatus(status);
        event.setCategory(category);

        try {
            return eventMapper.toResponse(eventRepository.save(event));
        } catch (OptimisticLockingFailureException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento fue modificado simultáneamente");
        }
    }

    @Transactional
    public void delete(
            Long id,
            UserDetailsImpl currentUser) {
        EventEntity event = findActiveEvent(id);

        validateOwnership(event, currentUser);

        event.setDeleted(true);
        event.setStatus("CANCELLED");

        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> findMine(
            UserDetailsImpl currentUser,
            Pageable pageable) {
        return eventRepository
                .findAllByOrganizerIdAndDeletedFalse(
                        currentUser.getId(),
                        pageable)
                .map(eventMapper::toResponse);
    }

    private EventEntity findActiveEvent(Long id) {
        return eventRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento no encontrado"));
    }

    private CategoryEntity findActiveCategory(Long categoryId) {
        CategoryEntity category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Categoría no encontrada"));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La categoría seleccionada está inactiva");
        }

        return category;
    }

    private void validateOwnership(
            EventEntity event,
            UserDetailsImpl currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }

        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para modificar este evento");
        }
    }

    private boolean isAdmin(UserDetailsImpl currentUser) {
        return currentUser.getAuthorities()
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(authority -> authority.getAuthority())
                .anyMatch(ROLE_ADMIN::equals);
    }

    private void validateEventData(
            String modality,
            String location,
            String virtualUrl,
            java.time.LocalDateTime registrationStartAt,
            java.time.LocalDateTime registrationEndAt,
            java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt) {
        if (!registrationStartAt.isBefore(registrationEndAt)
                || registrationEndAt.isAfter(startAt)
                || !startAt.isBefore(endAt)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las fechas del evento no respetan el orden requerido");
        }

        boolean hasLocation = location != null && !location.isBlank();

        boolean hasVirtualUrl = virtualUrl != null && !virtualUrl.isBlank();

        switch (modality) {
            case "PRESENTIAL" -> {
                if (!hasLocation || hasVirtualUrl) {
                    throw invalidModalityData();
                }
            }
            case "VIRTUAL" -> {
                if (hasLocation || !hasVirtualUrl) {
                    throw invalidModalityData();
                }
            }
            case "HYBRID" -> {
                if (!hasLocation || !hasVirtualUrl) {
                    throw invalidModalityData();
                }
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Modalidad no permitida");
        }
    }

    private void applyModalityData(
            EventEntity event,
            String modality,
            String location,
            String virtualUrl) {
        switch (modality) {
            case "PRESENTIAL" -> {
                event.setLocation(location.trim());
                event.setVirtualUrl(null);
            }
            case "VIRTUAL" -> {
                event.setLocation(null);
                event.setVirtualUrl(virtualUrl.trim());
            }
            case "HYBRID" -> {
                event.setLocation(location.trim());
                event.setVirtualUrl(virtualUrl.trim());
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Modalidad no permitida");
        }
    }

    private void validateStatus(String status) {
        if (!status.equals("DRAFT")
                && !status.equals("PUBLISHED")
                && !status.equals("FINISHED")
                && !status.equals("CANCELLED")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de evento no permitido");
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private ResponseStatusException invalidModalityData() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Los datos no corresponden con la modalidad seleccionada");
    }
}