package ec.edu.ups.icc.academic_events.registrations.mappers;

import ec.edu.ups.icc.academic_events.registrations.dtos.RegistrationResponseDTO;
import ec.edu.ups.icc.academic_events.registrations.entities.RegistrationEntity;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {

    public RegistrationResponseDTO toResponse(
            RegistrationEntity registration
    ) {
        String participantName =
                registration.getParticipant().getFirstName()
                        + " "
                        + registration.getParticipant().getLastName();

        return new RegistrationResponseDTO(
                registration.getId(),
                registration.getRegistrationCode(),
                registration.getEvent().getId(),
                registration.getEvent().getTitle(),
                registration.getParticipant().getId(),
                participantName,
                registration.getParticipant().getEmail(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                registration.getStatusUpdatedAt(),
                registration.getConfirmedAt(),
                registration.getCancelledAt(),
                registration.getVersion()
        );
    }
}