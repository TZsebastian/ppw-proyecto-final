package ec.edu.ups.icc.academic_events.events.mappers;

import ec.edu.ups.icc.academic_events.events.dtos.EventResponseDTO;
import ec.edu.ups.icc.academic_events.events.entities.EventEntity;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponseDTO toResponse(EventEntity event) {
        return new EventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getModality(),
                event.getLocation(),
                event.getVirtualUrl(),
                event.getCapacity(),
                event.getAvailableCapacity(),
                event.getRegistrationStartAt(),
                event.getRegistrationEndAt(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getOrganizer().getId(),
                event.getOrganizer().getFirstName()
                        + " "
                        + event.getOrganizer().getLastName(),
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getVersion(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}