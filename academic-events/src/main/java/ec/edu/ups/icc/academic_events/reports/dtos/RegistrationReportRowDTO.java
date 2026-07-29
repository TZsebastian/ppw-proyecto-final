package ec.edu.ups.icc.academic_events.reports.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistrationReportRowDTO(
        Long registrationId,
        UUID registrationCode,
        String participantName,
        String participantEmail,
        String status,
        LocalDateTime registeredAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt
) {
}