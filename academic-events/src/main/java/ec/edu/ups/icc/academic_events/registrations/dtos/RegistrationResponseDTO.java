package ec.edu.ups.icc.academic_events.registrations.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistrationResponseDTO(
        Long id,
        UUID registrationCode,
        Long eventId,
        String eventTitle,
        Long participantId,
        String participantName,
        String participantEmail,
        String status,
        LocalDateTime registeredAt,
        LocalDateTime statusUpdatedAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        Long version
) {
}