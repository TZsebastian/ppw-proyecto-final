package ec.edu.ups.icc.academic_events.sessions.dtos;

import java.time.LocalDateTime;

public record SessionResponseDTO(
        Long id,
        Long eventId,
        String eventTitle,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        String virtualUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}