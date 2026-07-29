package ec.edu.ups.icc.academic_events.sessions.mappers;

import ec.edu.ups.icc.academic_events.sessions.dtos.SessionResponseDTO;
import ec.edu.ups.icc.academic_events.sessions.entities.SessionEntity;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponseDTO toResponse(
            SessionEntity session
    ) {
        return new SessionResponseDTO(
                session.getId(),
                session.getEvent().getId(),
                session.getEvent().getTitle(),
                session.getTitle(),
                session.getDescription(),
                session.getStartAt(),
                session.getEndAt(),
                session.getLocation(),
                session.getVirtualUrl(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}