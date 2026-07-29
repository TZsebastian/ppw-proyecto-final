package ec.edu.ups.icc.academic_events.events.dtos;

import java.time.LocalDateTime;

public record EventResponseDTO(
        Long id,
        String title,
        String description,
        String modality,
        String location,
        String virtualUrl,
        Integer capacity,
        Integer availableCapacity,
        LocalDateTime registrationStartAt,
        LocalDateTime registrationEndAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        Long organizerId,
        String organizerName,
        Long categoryId,
        String categoryName,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}